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
 * Interface defining the contract for managing heartbeat monitoring.
 *
 * `HeartbeatManaging` enables decoupling of heartbeat monitoring logic from its implementation,
 * facilitating testing and future extensibility. It defines methods for starting and stopping
 * heartbeat monitoring and handling received heartbeats.
 *
 * ## Thread Safety
 * Implementations must ensure thread-safe handling of timers and state transitions.
 *
 * ## Implementation Example
 * ```kotlin
 * class CustomHeartbeatManager(
 *     private val timeoutHandler: () -> Unit
 * ) : HeartbeatManaging {
 *     override fun startMonitoring(intervalSeconds: Long) {
 *         // Custom implementation
 *     }
 *
 *     override fun stopMonitoring() {
 *         // Custom implementation
 *     }
 *
 *     override fun heartbeatReceived() {
 *         // Custom implementation
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
interface HeartbeatManaging {
    /**
     * Starts monitoring heartbeats at the specified interval.
     *
     * If a previous monitoring job exists, it stops the existing job before creating a new one.
     *
     * @param intervalSeconds The time interval (in seconds) between each heartbeat check.
     */
    fun startMonitoring(intervalSeconds: Long)

    /**
     * Stops the heartbeat monitoring.
     *
     * This method must ensure proper cleanup of timers or resources to prevent memory leaks.
     */
    fun stopMonitoring()

    /**
     * Resets the heartbeat timer upon receiving a heartbeat signal.
     *
     * This method should be called whenever a heartbeat is received to reset the monitoring timer.
     */
    fun heartbeatReceived()

    /**
     * Cleans up resources associated with the heartbeat manager.
     *
     * Implementations should ensure all monitoring is stopped and no resources are leaked.
     */
    fun cleanup()
}
