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
 * Interface for receiving real-time updates from the Pushlytic SDK.
 *
 * This listener interface enables your application to respond to:
 * - Connection state changes
 * - Incoming messages
 *
 * ## Thread Safety
 * All callback methods are guaranteed to be called on the main thread for UI safety.
 * The SDK internally handles thread dispatching.
 *
 * ## Implementation Example
 * ```kotlin
 * class MessageHandler : PushlyticListener {
 *     override fun onConnectionStatusChanged(status: ConnectionStatus) {
 *         when (status) {
 *             ConnectionStatus.Connected -> {
 *                 Log.d(TAG, "Connected to message stream")
 *             }
 *             ConnectionStatus.Disconnected -> {
 *                 Log.d(TAG, "Disconnected from message stream")
 *             }
 *             is ConnectionStatus.Error -> {
 *                 handleError(status.error)
 *             }
 *             ConnectionStatus.Timeout -> {
 *                 handleTimeout()
 *             }
 *         }
 *     }
 *
 *     override fun onMessageReceived(message: String) {
 *         Log.d(TAG, "Received message: $message")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
interface PushlyticListener {
    /**
     * Called when the connection status changes.
     * This callback is guaranteed to be executed on the main thread.
     *
     * @param status The new connection status
     */
    fun onConnectionStatusChanged(status: ConnectionStatus)

    /**
     * Called when a new message is received.
     * This callback is guaranteed to be executed on the main thread.
     *
     * @param message The received message content
     */
    fun onMessageReceived(message: String)
}