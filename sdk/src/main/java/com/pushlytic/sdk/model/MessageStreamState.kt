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

package com.pushlytic.sdk.model

/**
 * Represents the various states of the message stream connection in the Pushlytic SDK.
 *
 * This sealed class allows SDK consumers to track the current state of the message stream
 * and handle incoming messages, connection errors, and heartbeat checks seamlessly.
 *
 * Example usage:
 * ```kotlin
 * when (val state = messageStreamState) {
 *     MessageStreamState.Connected -> handleConnection()
 *     is MessageStreamState.MessageReceived -> processMessage(state.message)
 *     is MessageStreamState.HeartbeatReceived -> updateLastHeartbeat(state.status)
 *     is MessageStreamState.ConnectionError -> handleError(state.error)
 *     MessageStreamState.Disconnected -> attemptReconnection()
 *     MessageStreamState.Timeout -> handleTimeout()
 * }
 * ```
 *
 * @since 1.0.0
 */
sealed class MessageStreamState {
    /**
     * The message stream is successfully connected.
     */
    object Connected : MessageStreamState() {
        @JvmStatic
        private fun readResolve(): Any = Connected
    }

    /**
     * A message has been received from the stream.
     *
     * @property message The content of the received message
     */
    data class MessageReceived(
        val message: String
    ) : MessageStreamState()

    /**
     * An error occurred with the message stream connection.
     *
     * @property error The error that caused the connection issue
     */
    data class ConnectionError(
        val error: Throwable
    ) : MessageStreamState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ConnectionError
            return error.message == other.error.message
        }

        override fun hashCode(): Int {
            return error.message?.hashCode() ?: 0
        }
    }

    /**
     * The message stream connection has been disconnected.
     */
    object Disconnected : MessageStreamState() {
        @JvmStatic
        private fun readResolve(): Any = Disconnected
    }

    /**
     * The connection attempt timed out.
     */
    object Timeout : MessageStreamState() {
        @JvmStatic
        private fun readResolve(): Any = Timeout
    }
}

/**
 * Extension function to convert MessageStreamState to a human-readable string.
 * Useful for logging and debugging purposes.
 *
 * @return A string representation of the message stream state
 */
fun MessageStreamState.toDisplayString(): String = when (this) {
    is MessageStreamState.Connected -> "Connected"
    is MessageStreamState.MessageReceived -> "Message Received: ${message.take(100)}..."
    is MessageStreamState.ConnectionError -> "Connection Error: ${error.message}"
    is MessageStreamState.Disconnected -> "Disconnected"
    is MessageStreamState.Timeout -> "Timeout"
}

