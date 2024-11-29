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
 * Represents the connection states for the Pushlytic Android SDK.
 *
 * This sealed class provides a type-safe way to handle different connection states within the SDK.
 * It's designed to be used throughout the SDK to maintain consistency in connection state management
 * and error handling.
 *
 * Example usage:
 * ```kotlin
 * when (val status = connectionStatus) {
 *     ConnectionStatus.Connected -> handleConnectedState()
 *     ConnectionStatus.Disconnected -> initiateReconnection()
 *     ConnectionStatus.Timeout -> handleTimeout()
 *     is ConnectionStatus.Error -> handleError(status.error)
 * }
 * ```
 *
 * @see MHError
 * @since 1.0.0
 */
sealed class ConnectionStatus {
    /**
     * Represents a successful connection to the Pushlytic message stream.
     * In this state, the SDK is fully operational and can send/receive messages.
     */
    object Connected : ConnectionStatus()

    /**
     * Indicates that the SDK is currently disconnected from the message stream.
     * This can occur due to:
     * - Intentional disconnection by the client
     * - Network connectivity issues
     * - Server-side disconnection
     */
    object Disconnected : ConnectionStatus()

    /**
     * Indicates that a connection attempt has timed out.
     * This typically occurs when:
     * - Network conditions are poor
     * - The server is unreachable
     * - The client's network is blocking the connection
     */
    object Timeout : ConnectionStatus()

    /**
     * Represents an error state with associated error details.
     *
     * @property error The specific error that caused the connection failure
     */
    data class Error(val error: PushlyticError) : ConnectionStatus()
}

/**
 * Extension function to convert ConnectionStatus to a human-readable string.
 * Useful for logging and debugging purposes.
 *
 * @return A string representation of the connection status
 */
fun ConnectionStatus.toDisplayString(): String = when (this) {
    is ConnectionStatus.Connected -> "Connected"
    is ConnectionStatus.Disconnected -> "Disconnected"
    is ConnectionStatus.Timeout -> "Timeout"
    is ConnectionStatus.Error -> "Error: ${error.message}"
}