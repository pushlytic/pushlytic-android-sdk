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

package com.pushlytic.sdk.server.interfaces

import com.pushlytic.sdk.model.MetadataOperationType

/**
 * Interface for handling communication with Pushlytic's backend services.
 *
 * This interface defines the contract for managing:
 * - Real-time message streaming
 * - Custom event handling
 * - Metadata management
 * - User registration
 *
 * ## Thread Safety
 * - All methods are thread-safe.
 * - State updates and callbacks are guaranteed to be executed on the main thread.
 *
 * ## Implementation
 * Concrete implementations should manage connection state, ensure thread-safe operations,
 * and handle reconnection logic as required.
 *
 * @since 1.0.0
 */
interface ApiClient {
    /**
     * Indicates whether the client is currently connected to the message stream.
     */
    val isConnected: Boolean

    /**
     * The API key used to authenticate the client.
     */
    var apiKey: String?

    /**
     * Sets the listener for message stream state changes.
     * Only one listener can be active at a time.
     *
     * @param listener The listener to receive state updates
     */
    fun setMessageStreamListener(listener: MessageStreamListener)

    /**
     * Opens a message stream to communicate with the server.
     *
     * @param metadata Optional metadata to include with the initial connection. Can contain
     * additional information about the connection or user that may be useful for server-side
     * processing or workflows.
     *
     * Example usage:
     * ```kotlin
     * // Basic connection
     * Pushlytic.openMessageStream()
     *
     * // Connection with metadata
     * Pushlytic.openMessageStream(metadata = mapOf(
     *     "user_type" to "premium",
     *     "app_version" to "2.1.0",
     *     "device_language" to "en-US"
     * ))
     * ```
     */
    fun openMessageStream(metadata: Map<String, Any>? = null)

    /**
     * Ends the current message stream connection.
     *
     * @param wasManuallyDisconnected If `true`, reconnection attempts are disabled.
     */
    fun endConnection(wasManuallyDisconnected: Boolean = false)

    /**
     * Sends a custom event to the server.
     *
     * @param name The name of the event.
     * @param metadata A map of metadata to associate with the event.
     */
    fun sendCustomEvent(name: String, metadata: Map<String, Any>?)

    /**
     * Registers a user identifier for tracking on the server.
     *
     * @param newUserID The unique identifier of the user.
     */
    fun registerUserID(newUserID: String)

    /**
     * Registers tags to associate with the current session.
     *
     * @param tags A list of tags for segmentation and personalization.
     */
    fun registerTags(tags: List<String>)

    /**
     * Updates or clears metadata for the session.
     *
     * @param operation The metadata operation type (update or clear).
     * @param metadata Optional metadata map to update (only for update operations).
     */
    fun updateMetadata(operation: MetadataOperationType, metadata: Map<String, Any>? = null)

    /**
     * Shuts down the client and releases all resources.
     */
    fun shutdown()
}
