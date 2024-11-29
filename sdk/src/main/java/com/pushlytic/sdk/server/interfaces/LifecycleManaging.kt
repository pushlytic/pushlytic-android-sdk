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

/**
 * Interface for managing application lifecycle events for the Pushlytic SDK.
 *
 * This interface enables custom implementations of lifecycle management logic.
 * It abstracts lifecycle-related responsibilities to ensure that:
 * - Application state transitions are monitored (foreground, background, termination)
 * - Connection state is managed based on lifecycle events
 * - Resources are cleaned up to prevent memory leaks
 *
 * ## Thread Safety
 * Implementations should guarantee thread-safe handling of state transitions and resources.
 *
 * ## Implementation Example
 * ```kotlin
 * class CustomLifecycleManager : LifecycleManaging {
 *     override fun handleStateTransition(newState: LifecycleState) {
 *         when (newState) {
 *             LifecycleState.Foreground -> startConnection()
 *             LifecycleState.Background -> pauseConnection()
 *             LifecycleState.Terminated -> closeConnection()
 *         }
 *     }
 *
 *     override fun cleanup() {
 *         // Clean up resources
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
interface LifecycleManaging {
    /**
     * Handles application state transitions.
     *
     * @param newState The new application state
     */
    fun handleStateTransition(newState: LifecycleState)

    /**
     * Cleans up resources and removes lifecycle observers.
     */
    fun cleanup()
}

/**
 * Enum representing possible application states relevant to lifecycle management.
 *
 * @since 1.0.0
 */
enum class LifecycleState {
    /**
     * Application is in the foreground and visible to the user.
     */
    Foreground,

    /**
     * Application is in the background.
     */
    Background,

    /**
     * Application is being terminated.
     */
    Terminated
}
