/*
 * Copyright (C) 2024 Pushlytic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pushlytic.sdk

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.pushlytic.sdk.logging.DefaultLogger
import com.pushlytic.sdk.logging.PushlyticLogger
import com.pushlytic.sdk.model.MessageStreamState
import com.pushlytic.sdk.model.MetadataOperationType
import com.pushlytic.sdk.server.HeartbeatManagerImpl
import com.pushlytic.sdk.server.interfaces.ApiClient
import com.pushlytic.sdk.server.interfaces.HeartbeatManaging
import com.pushlytic.sdk.server.interfaces.MessageStreamListener
import io.grpc.*
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import org.json.JSONObject
import pb.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
/**
 * ApiClient facilitates the connection and communication with Pushlytic's gRPC server.
 *
 * This client manages:
 * - Bi-directional streaming for real-time messages
 * - Connection and reconnection lifecycle with automatic reconnection
 * - Heartbeat monitoring to maintain connection health
 * - Custom events and metadata management
 * - Detailed state tracking for each stage of the connection
 *
 * Thread Safety:
 * - All public methods are thread-safe
 * - State updates and callbacks are always delivered on the main thread
 * - Internal state is protected by synchronization primitives
 *
 * Usage:
 * 1. Initialize the `ApiClient` with a valid API key.
 * 2. Call `openMessageStream` to establish a streaming connection.
 * 3. Use `sendCustomEvent`, `registerUserID`, `registerTags`, and `updateMetadata` for additional session configurations.
 * 4. Listen to `messageStreamState` for real-time state updates and handle any state changes or errors.
 *
 * @constructor Creates an instance of the ApiClient.
 * @param apiKey A valid API key for authenticating the client
 * @param serverHost Optional server host; defaults to Pushlytic's primary server
 * @param serverPort Optional server port; defaults to Pushlytic's primary server port
 */
class ApiClientImpl(
    private val application: Application,
    private val logger: PushlyticLogger = DefaultLogger()
) : ApiClient {

    private lateinit var channel: ManagedChannel
    private lateinit var stub: StreamlinkGrpc.StreamlinkStub
    private val sessionID: String = UUID.randomUUID().toString()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectionJob: Job? = null
    private var isManuallyClosed = false
    private var messageStreamListener: MessageStreamListener? = null
    private var requestObserver: StreamObserver<MessageRequest>? = null

    override var apiKey: String? = null
    override var isConnected = false

    var userID: String? = null
    var tags: List<String>? = null
    var connectionInProgress = false

    private var heartbeatManager: HeartbeatManaging = HeartbeatManagerImpl {
        messageStreamListener?.onStateChanged(MessageStreamState.Timeout)
    }

    private val deviceId: String by lazy {
        @SuppressLint("HardwareIds")
        val id = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
        if (id.isNullOrEmpty()) {
            UUID.randomUUID().toString()
        } else {
            id
        }
    }

    init {
        initializeChannel()
    }

    /**S
     * Initializes the gRPC channel for communication with the server.
     */
    private fun initializeChannel() {
        val host = ApiConstants.RELEASE_SERVER_HOST

        if (::channel.isInitialized && !channel.isShutdown) {
            channel.shutdownNow()
        }

        val customTrustManager =
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(customTrustManager), null)
        }

        channel = OkHttpChannelBuilder.forTarget("$host:${ApiConstants.SERVER_PORT}")
            .sslSocketFactory(sslContext.socketFactory)
            .useTransportSecurity()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .intercept(LoggingInterceptor())
            .build()
        stub = StreamlinkGrpc.newStub(channel)
    }

    /**
     * Opens a message stream to receive messages from the server.
     * This method sets up the connection and manages the stream observer for incoming messages.
     */
    override fun openMessageStream() {
        isManuallyClosed = false
        if (::channel.isInitialized && channel.isShutdown) {
            initializeChannel()
        }

        if (isConnected || connectionInProgress) return
        connectionInProgress = true

        val metadata = Metadata().apply {
            apiKey?.let { put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $it") }
            put(Metadata.Key.of("Client-Type", Metadata.ASCII_STRING_MARSHALLER), "Android")
            put(Metadata.Key.of("Device-ID", Metadata.ASCII_STRING_MARSHALLER), deviceId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            requestObserver = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .messageStream(object : StreamObserver<MessageResponse> {
                    override fun onNext(message: MessageResponse) {
                        handleIncomingMessage(message)
                    }

                    override fun onError(t: Throwable) {
                        if (t is StatusRuntimeException) {
                            when (t.status.code) {
                                Status.Code.UNAVAILABLE -> {
                                    logInfo("Stream unavailable, attempting to reconnect: ${t.message}")
                                    scheduleReconnection()
                                    return
                                }
                                Status.Code.INTERNAL -> {
                                    logInfo("Internal error, resetting the connection: ${t.message}")
                                    resetConnection()
                                    return
                                }
                                else -> {
                                    logInfo("Unhandled gRPC error: ${t.status.code}, ${t.message}")
                                }
                            }
                        } else {
                            logError("Unexpected error in message stream", t)
                        }

                        // Handle unrecoverable errors
                        handleStreamCompletion(t)
                    }

                    override fun onCompleted() {
                        handleStreamCompletion(null)
                    }
                })

            requestObserver?.let { observer ->
                sendOpenConnectionMessage(observer)
            }
            startHeartbeatMonitoring()
        }
    }

    /**
     * Sends the initial connection message to open the stream.
     *
     * @param requestObserver The stream observer handling outgoing messages.
     */
    private fun sendOpenConnectionMessage(requestObserver: StreamObserver<MessageRequest>) {
        val openConnectionMessage = MessageRequest.newBuilder().apply {
            sessionId = this@ApiClientImpl.sessionID
            userId = this@ApiClientImpl.userID ?: ""
            addAllTags(this@ApiClientImpl.tags ?: emptyList())
            controlMessage = ControlMessage.newBuilder().apply {
                command = ControlCommand.OPEN
            }.build()
        }.build()

        sendMessage(openConnectionMessage, requestObserver)
    }

    /**
     * Sets the listener for message stream state changes.
     * This listener receives state updates, message events, and errors.
     * Only one listener can be active at a time, with newer listeners replacing previous ones.
     *
     * @param listener The message stream listener, or null to remove current listener
     */
    override fun setMessageStreamListener(listener: MessageStreamListener) {
        messageStreamListener = listener
    }

    /**
     * Registers the user ID for tracking in the SDK.
     *
     * @param newUserID The user ID to register.
     */
    override fun registerUserID(newUserID: String) {
        userID = newUserID
        requestObserver?.let { observer ->
            val registerClientIDMessage = MessageRequest.newBuilder().apply {
                sessionId = this@ApiClientImpl.sessionID
                userId = newUserID
            }.build()

            sendMessage(registerClientIDMessage, observer)
        }
    }

    /**
     * Registers tags for tracking in the SDK.
     *
     * @param newTags A list of tags to register.
     */
    override fun registerTags(newTags: List<String>) {
        tags = newTags
        requestObserver?.let { observer ->
            val registerTagsMessage = MessageRequest.newBuilder().apply {
                sessionId = this@ApiClientImpl.sessionID
                addAllTags(newTags)
            }.build()

            sendMessage(registerTagsMessage, observer)
        }
    }

    /**
     * Sends a custom event to the server.
     *
     * @param name The name of the event.
     * @param metadata Additional metadata for the event.
     */
    override fun sendCustomEvent(name: String, metadata: Map<String, Any>?) {
        if (!isConnected) return

        val metadataJson = JSONObject(metadata).toString()
        val customEventMessage = MessageRequest.newBuilder().apply {
            sessionId = this@ApiClientImpl.sessionID
            customEvent = CustomEvent.newBuilder().apply {
                this.name = name
                this.metadata = metadataJson
            }.build()
        }.build()

        requestObserver?.let { sendMessage(customEventMessage, it) }
    }

    /**
     * Handles metadata operations. Supports both updating and clearing metadata.
     * For updates, merges with existing metadata. For clearing, removes all metadata values.
     *
     * @param operation The type of operation (UPDATE or CLEAR)
     * @param metadata Optional map of metadata to set/update (only used for UPDATE operations)
     */
    override fun updateMetadata(operation: MetadataOperationType, metadata: Map<String, Any>?) {
        if (!isConnected) {
            return
        }

        val metadataJson = metadata?.let { JSONObject(it).toString() } ?: ""

        val metadataMessage = MessageRequest.newBuilder().apply {
            sessionId = this@ApiClientImpl.sessionID
            metadataOperationValue = operation.toProtoValue()
            if (operation == MetadataOperationType.UPDATE) {
                this.metadata = metadataJson
            }
        }.build()

        requestObserver?.let { sendMessage(metadataMessage, it) }
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The incoming message from the server.
     */
    private fun handleIncomingMessage(message: MessageResponse) {
        mainHandler.post {
            when (message.dataCase) {
                MessageResponse.DataCase.MESSAGES -> {
                    message.messages.messageList.forEach { msg ->
                        messageStreamListener?.onStateChanged(MessageStreamState.MessageReceived(msg.content))
                        requestObserver?.let { acknowledgeMessage(msg.traceId, it) }
                    }
                }
                MessageResponse.DataCase.HEARTBEAT -> {
                    heartbeatManager.heartbeatReceived()
                }
                MessageResponse.DataCase.CONNECTION_ACKNOWLEDGEMENT -> {
                    isConnected = true
                    connectionInProgress = false
                    reconnectionJob?.cancel()
                    messageStreamListener?.onStateChanged(MessageStreamState.Connected)
                }
                MessageResponse.DataCase.CONTROL_MESSAGE -> {
                    handleControlMessage(message.controlMessage)
                }
                else -> {
                    logWarning("Received unknown or unhandled MessageResponse.DataCase: ${message.dataCase}.")
                }
            }
        }
    }

    /**
     * Acknowledges a received message.
     *
     * @param traceID The trace ID of the message to acknowledge.
     * @param requestObserver The stream observer handling outgoing messages.
     */
    private fun acknowledgeMessage(traceID: String, requestObserver: StreamObserver<MessageRequest>) {
        val ackMessage = MessageRequest.newBuilder().apply {
            sessionId = this@ApiClientImpl.sessionID
            addMessageAcknowledgement(
                MessageAcknowledgement.newBuilder().setTraceId(traceID).build()
            )
        }.build()

        sendMessage(ackMessage, requestObserver)
    }

    /**
     * Handles control messages from the server.
     *
     * @param controlMessage The control message to handle.
     */
    private fun handleControlMessage(controlMessage: ControlMessage) {
        when (controlMessage.command) {
            ControlCommand.CLOSE -> {
                endConnection()
            }
            else -> {
                logWarning("Unknown ControlCommand received: ${controlMessage.command}.")
            }
        }
    }

    /**
     * Logs an error message with an optional throwable, but only in debug mode.
     *
     * Use this for capturing errors during development without impacting production.
     *
     * @param message A clear description of the error.
     * @param throwable (Optional) The exception or error for additional context.
     */
    private fun logError(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            logger.logError(TAG, message, throwable)
        }
    }

    /**
     * Logs a warning message, but only in debug mode.
     *
     * Use this for non-critical issues that may require attention during development.
     *
     * @param message A clear description of the warning.
     * @param throwable (Optional) The exception or warning for additional context.
     */
    private fun logWarning(message: String) {
        if (BuildConfig.DEBUG) {
            logger.logWarning(TAG, message)
        }
    }

    /**
     * Logs an informational message, but only in debug mode.
     *
     * Use this for capturing useful information during development without impacting production.
     *
     * @param message A clear description of the information.
     */
    private fun logInfo(message: String) {
        if (BuildConfig.DEBUG) {
            logger.logInfo(TAG, message)
        }
    }

    /**
     * Handles stream completion and error handling.
     *
     * @param t The throwable representing an error, or null if completed successfully.
     */
    private fun handleStreamCompletion(t: Throwable?) {
        mainHandler.post {
            isConnected = false
            connectionInProgress = false
            if (t == null) {
                messageStreamListener?.onStateChanged(MessageStreamState.Disconnected)
            } else {
                messageStreamListener?.onStateChanged(MessageStreamState.ConnectionError(t))
            }
            scheduleReconnection()
        }
    }

    /**
     * Schedules reconnection attempts in case of a disconnection.
     */
    private fun scheduleReconnection() {
        if (isManuallyClosed) return
        reconnectionJob?.cancel()
        reconnectionJob = CoroutineScope(Dispatchers.Default).launch {
            var delayTime = ApiConstants.RECONNECTION_DELAY_SECONDS
            while (!isConnected && !isManuallyClosed) {
                logInfo("Attempting to reconnect stream...")
                delay(delayTime * 1000L)
                openMessageStream()
                delayTime = (delayTime * 2).coerceAtMost(ApiConstants.MAX_RECONNECTION_DELAY_SECONDS)
            }
        }
    }

    /**
     * Sends a message through the gRPC stream.
     *
     * @param message The message to send.
     * @param requestObserver The stream observer handling outgoing messages.
     */
    private fun sendMessage(message: MessageRequest, requestObserver: StreamObserver<MessageRequest>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isConnected && !connectionInProgress) {
                    return@launch
                }
                requestObserver.onNext(message)
            } catch (e: IllegalStateException) {
                logError("Failed to send a message on a completed or inactive stream", e)
            }
        }
    }

    /**
     * Ends the message stream connection and closes the gRPC channel.
     */
    override fun endConnection(wasManuallyDisconnected: Boolean) {
        isManuallyClosed = true
        reconnectionJob?.cancel()

        requestObserver?.let { observer ->
            try {
                if (isConnected) {
                    val closeConnectionMessage = MessageRequest.newBuilder().apply {
                        sessionId = this@ApiClientImpl.sessionID
                        controlMessage = ControlMessage.newBuilder().apply {
                            command = ControlCommand.CLOSE
                        }.build()
                    }.build()

                    sendMessage(closeConnectionMessage, observer)
                    isConnected = false
                }
                stopHeartbeatMonitoring()
            } catch (e: IllegalStateException) {
                logError("Attempted to close an already completed stream", e)
            } finally {
                requestObserver = null
                observer.onCompleted()
                shutdown()
            }
        }
    }

    /**
     * Resets the gRPC connection by shutting down the current channel,
     * reinitializing it, and reopening the message stream.
     *
     * This method is useful for handling situations where the connection
     * encounters a non-recoverable error, such as an `INTERNAL` gRPC error,
     * and a clean reset is required to establish a fresh connection.
     *
     * Steps:
     * 1. Ends the current connection and cleans up resources.
     * 2. Waits briefly to avoid immediate reconnection issues.
     * 3. Reinitializes the gRPC channel.
     * 4. Reopens the message stream.
     */
    private fun resetConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logInfo("Resetting connection: ending the current connection")
                endConnection() // Clean up the existing connection and resources.
            } catch (e: Exception) {
                logError("Error while ending connection during reset", e)
            }
            delay(2000)
            initializeChannel()
            openMessageStream()
        }
    }

    /**
     * Starts heartbeat monitoring to keep the connection alive.
     */
    private fun startHeartbeatMonitoring() {
        stopHeartbeatMonitoring()
        heartbeatManager.startMonitoring(ApiConstants.HEARTBEAT_INTERVAL_SECONDS)
    }

    /**
     * Stops the ongoing heartbeat monitoring.
     */
    private fun stopHeartbeatMonitoring() {
        heartbeatManager.stopMonitoring()
        messageStreamListener?.onStateChanged(MessageStreamState.Disconnected)
    }

    /**
     * Shuts down the gRPC channel and terminates all connections.
     */
    override fun shutdown() {
        requestObserver = null
        connectionInProgress = false
        heartbeatManager.cleanup()

        if (::channel.isInitialized && !channel.isShutdown) {
            channel.shutdownNow()
        }
    }

    companion object {
        private const val TAG = "Pushlytic"
    }
}

class LoggingInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return next.newCall(method, callOptions)
    }
}