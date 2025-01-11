/*
* Copyright (C) 2024 Pushlytic
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.pushlytic.sdk

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.pushlytic.sdk.model.ConnectionStatus
import com.pushlytic.sdk.model.MessageStreamState
import com.pushlytic.sdk.model.MetadataOperationType
import com.pushlytic.sdk.model.PushlyticError
import com.pushlytic.sdk.model.PushlyticListener
import com.pushlytic.sdk.server.LifecycleManagerImpl
import com.pushlytic.sdk.server.interfaces.ApiClient
import com.pushlytic.sdk.server.interfaces.LifecycleManaging
import com.pushlytic.sdk.server.interfaces.MessageStreamListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pushlytic provides a thread-safe, configurable SDK for real-time mobile messaging and analytics.
 *
 * This object serves as the primary interface for integrating real-time messaging capabilities
 * into Android applications. It provides features such as:
 * - Real-time message streaming
 * - User identification and tracking
 * - Custom event analytics
 * - Metadata management
 * - Connection state management
 *
 * Thread Safety:
 * All public methods are thread-safe and can be called from any thread.
 * Callbacks are always delivered on the main thread for UI safety.
 *
 * Example Usage:
 * ```kotlin
 * class MessageHandler : PushlyticListener {
 * override fun onConnectionStatusChanged(status: ConnectionStatus) {
 * when (status) {
 * ConnectionStatus.Connected -> handleConnected()
 * ConnectionStatus.Disconnected -> handleDisconnected()
 * is ConnectionStatus.Error -> handleError(status.error)
 * ConnectionStatus.Timeout -> handleTimeout()
 * }
 * }
 *
 * override fun onMessageReceived(message: String) {
 * processMessage(message)
 * }
 * }
 *
 * // Configure the SDK
 * val config = Pushlytic.Configuration("your-api-key")
 * Pushlytic.configure(application, config)
 * Pushlytic.setListener(MessageHandler())
 * Pushlytic.openMessageStream()
 * ```
 *
 * @since 1.0.0
 */
object Pushlytic: MessageStreamListener {
    private lateinit var application: Application
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var apiClient: ApiClient? = null
    private var lifecycleManager: LifecycleManaging? = null
    private var listener: PushlyticListener? = null

    private var isInitialized = false
    private var isManuallyDisconnected = false
    private var _isStreamPreviouslyOpened = false

    private var storedApiKey: String? = null

    private var storedUserID: String? = null
    private var storedTags: List<String>? = null
    private var storedMetadata: Map<String, Any>? = null

    /**
     * Configuration settings for the Pushlytic SDK.
     *
     * @param apiKey Your API key obtained from the Pushlytic dashboard
     */
    data class Configuration(val apiKey: String)

    /**
     * Indicates whether the SDK is currently connected to the streaming service.
     * This property is thread-safe and can be accessed from any thread.
     */
    @JvmStatic
    val isConnected: Boolean
        get() = apiClient?.isConnected ?: false

    /**
     * Internal property to determine if the SDK should automatically reconnect.
     * Returns false if the connection was manually disconnected by the user.
     */
    internal val shouldAutoReconnect: Boolean
        get() = !isManuallyDisconnected

    /**
     * Indicates whether the message stream was previously opened.
     */
    internal val isStreamPreviouslyOpened: Boolean
        get() = _isStreamPreviouslyOpened

    /**
     * Sets the listener to receive SDK events and status updates.
     * All callbacks are guaranteed to be executed on the main thread.
     *
     * @param listener The listener implementation to receive updates
     */
    @JvmStatic
    fun setListener(listener: PushlyticListener?) {
        scope.launch {
            mutex.withLock {
                this@Pushlytic.listener = listener
            }
        }
    }

    /**
     * Configures and initializes the Pushlytic SDK.
     * This should be called from your Application class before using any other SDK features.
     *
     * @param application The Application instance
     * @param configuration SDK configuration containing your API key
     */
    @JvmStatic
    fun configure(application: Application, configuration: Configuration) {
        scope.launch {
            mutex.withLock {
                cleanup()
                this@Pushlytic.application = application
                storedApiKey = configuration.apiKey
                if (initializeClient(storedApiKey)) {
                    initializeLifecycleManager()
                    apiClient?.setMessageStreamListener(this@Pushlytic)
                    isInitialized = true
                } else {
                    notifyListenerOnMain {
                        it.onConnectionStatusChanged(ConnectionStatus.Error(PushlyticError.NotConfigured))
                    }
                }
            }
        }
    }

    /**
     * Opens a message stream to receive messages from the server.
     * The stream will remain open until explicitly closed or the application goes to background.
     */
    @JvmStatic
    fun openMessageStream() {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                if (lifecycleManager == null) {
                    initializeLifecycleManager()
                }
                isManuallyDisconnected = false
                _isStreamPreviouslyOpened = true
                startMessageStream()
                reapplyStoredState()
            }
        }
    }

    /**
     * Ends the current streaming connection.
     *
     * @param clearState If true, clears all stored state including user ID and stream metadata. It also prevents
     * reconnection on foregrounding unless Pushlytic.openMessageStream() is called first.
     */
    @JvmStatic
    fun endStream(clearState: Boolean = false) {
        scope.launch {
            mutex.withLock {
                checkInitialization()

                if (clearState) {
                    isManuallyDisconnected = true
                    _isStreamPreviouslyOpened = false
                    lifecycleManager?.cleanup()
                    lifecycleManager = null
                    cleanup()
                    clearStoredState()
                }

                apiClient?.endConnection()
                notifyListenerOnMain {
                    it.onConnectionStatusChanged(ConnectionStatus.Disconnected)
                }
            }
        }
    }

    /**
     * Decodes and processes a received JSON message string into a specified type.
     *
     * This method provides a type-safe way to handle incoming messages by attempting to decode
     * them into a specified `T` type. The completion handler is always called on the main thread
     * for UI safety.
     *
     * @param jsonString The JSON string to decode
     * @param type The class of the type to decode the JSON into
     * @param completion A lambda called with the decoded message on success
     * @param errorHandler Optional lambda called if decoding fails
     *
     * # Example Usage
     * ```kotlin
     * data class CustomMessage(val id: String, val content: String)
     *
     * Pushlytic.parseMessage(jsonString, CustomMessage::class.java,
     * completion = { message ->
     * println("Received message: $message")
     * },
     * errorHandler = { error ->
     * println("Failed to decode message: $error")
     * }
     * )
     * ```
     */
    @JvmStatic
    fun <T> parseMessage(
        message: String,
        type: Class<T>,
        completion: (T) -> Unit,
        errorHandler: ((Exception) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val message: T = Gson().fromJson(message, type)
                mainHandler.post {
                    completion(message)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    errorHandler?.invoke(e)
                }
            }
        }
    }

    /**
     * Registers a user identifier with the service.
     *
     * @param userID Unique identifier for the user
     */
    @JvmStatic
    fun registerUserID(userID: String) {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                storedUserID = userID
                apiClient?.registerUserID(userID)
            }
        }
    }

    /**
     * Registers tags associated with the current user.
     *
     * @param tags List of string tags to associate with the user
     */
    @JvmStatic
    fun registerTags(tags: List<String>) {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                storedTags = tags
                apiClient?.registerTags(tags)
            }
        }
    }

    /**
     * Sends a custom event to the analytics service.
     *
     * @param name Name of the custom event
     * @param metadata Additional data associated with the event
     */
    @JvmStatic
    fun sendCustomEvent(name: String, metadata: Map<String, Any>? = null) {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                apiClient?.sendCustomEvent(name, metadata)
            }
        }
    }

    /**
     * Sets or updates metadata for the connection.
     * If existing metadata is present, new values will be merged with existing ones.
     *
     * @param metadata Map of metadata key-value pairs
     */
    @JvmStatic
    fun setMetadata(metadata: Map<String, Any>) {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                storedMetadata = metadata
                apiClient?.updateMetadata(MetadataOperationType.UPDATE, metadata)
            }
        }
    }

    /**
     * Clears all metadata associated with the current connection.
     */
    @JvmStatic
    fun clearMetadata() {
        scope.launch {
            mutex.withLock {
                checkInitialization()
                storedMetadata = null
                apiClient?.updateMetadata(MetadataOperationType.CLEAR)
            }
        }
    }

    // MessageStreamListener Implementation
    override fun onStateChanged(state: MessageStreamState) {
        handleMessageStreamState(state)
    }

    private fun handleMessageStreamState(state: MessageStreamState) {
        when (state) {
            is MessageStreamState.Connected -> {
                notifyListenerOnMain {
                    it.onConnectionStatusChanged(ConnectionStatus.Connected)
                }
            }
            is MessageStreamState.MessageReceived -> {
                notifyListenerOnMain {
                    it.onMessageReceived(state.message)
                }
            }
            is MessageStreamState.ConnectionError -> {
                notifyListenerOnMain {
                    it.onConnectionStatusChanged(ConnectionStatus.Error(
                        when (state.error) {
                            is PushlyticError -> state.error
                            else -> PushlyticError.Custom(state.error.message ?: "Unknown error")
                        }
                    ))
                }
            }
            is MessageStreamState.Disconnected -> {
                notifyListenerOnMain {
                    it.onConnectionStatusChanged(ConnectionStatus.Disconnected)
                }
            }
            is MessageStreamState.Timeout -> {
                notifyListenerOnMain {
                    it.onConnectionStatusChanged(ConnectionStatus.Timeout)
                }
            }
        }
    }

    private fun notifyListenerOnMain(block: (PushlyticListener) -> Unit) {
        mainHandler.post {
            listener?.let(block)
        }
    }

    private fun initializeClient(apiKey: String?): Boolean {
        if (apiKey.isNullOrEmpty()) {
            return false
        }
        apiClient = ApiClientImpl(application)
        apiClient?.apiKey = apiKey
        return true
    }

    /**
     * Initializes the lifecycle manager to handle application state transitions.
     *
     * @param application The Application instance used to register lifecycle callbacks
     */
    private fun initializeLifecycleManager() {
        lifecycleManager = LifecycleManagerImpl()
    }

    private fun startMessageStream() {
        if (!isInitialized) {
            notifyListenerOnMain {
                it.onConnectionStatusChanged(ConnectionStatus.Error(PushlyticError.NotConfigured))
            }
            return
        }

        if (apiClient == null) {
            initializeClient(storedApiKey)
        }
        apiClient?.openMessageStream()
        apiClient?.setMessageStreamListener(this@Pushlytic)
    }

    private fun reapplyStoredState() {
        storedUserID?.let { apiClient?.registerUserID(it) }
        storedTags?.let { apiClient?.registerTags(it) }
        storedMetadata?.let { apiClient?.updateMetadata(MetadataOperationType.UPDATE, it) }
    }

    private fun checkInitialization() {
        if (!isInitialized) {
            throw PushlyticError.NotConfigured
        }
    }

    private fun cleanup() {
        lifecycleManager?.cleanup()
        lifecycleManager = null
        apiClient?.endConnection()
        apiClient?.shutdown()

        if (isManuallyDisconnected) {
            apiClient = null
        }
    }

    private fun clearStoredState() {
        storedUserID = null
        storedTags = null
        storedMetadata = null
    }
}