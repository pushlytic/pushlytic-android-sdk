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

package com.pushlytic.sdk.server

import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper
import com.pushlytic.sdk.server.interfaces.HeartbeatManaging

/**
 * Manages periodic heartbeat signals for monitoring connectivity.
 *
 * HeartbeatManager periodically invokes a timeout handler to check for connectivity
 * or heartbeat reception, enabling clients to handle timeout events if the expected
 * heartbeat signal isn't received within the specified interval.
 *
 * Example usage:
 * ```kotlin
 * val heartbeatManager = HeartbeatManagerImpl {
 *     // Handle timeout
 *     Log.w(TAG, "Heartbeat timeout occurred")
 *     disconnectConnection()
 * }
 *
 * // Start monitoring with 60-second interval
 * heartbeatManager.startMonitoring(intervalSeconds = 60)
 *
 * // Stop monitoring when needed
 * heartbeatManager.stopMonitoring()
 * ```
 *
 * @property timeoutHandler Callback to be invoked when a heartbeat timeout occurs
 * @since 1.0.0
 */
class HeartbeatManagerImpl(
    private val timeoutHandler: () -> Unit
) : HeartbeatManaging {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    private var lastHeartbeatTime: Long = System.currentTimeMillis()

    /**
     * Starts monitoring heartbeats at the specified interval.
     *
     * If a previous monitoring job exists, it stops the existing job before creating a new one.
     * The timeout handler will be invoked on the main thread to ensure safe UI updates.
     *
     * @param intervalSeconds The time interval (in seconds) between each heartbeat check
     */
    override fun startMonitoring(intervalSeconds: Long) {
        stopMonitoring()
        lastHeartbeatTime = System.currentTimeMillis()

        monitoringJob = scope.launch {
            while (isActive) {
                delay(ApiConstants.HEARTBEAT_TIMEOUT_SECONDS)
                val timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime
                if (timeSinceLastHeartbeat > intervalSeconds * ApiConstants.HEARTBEAT_TIMEOUT_SECONDS) {
                    mainHandler.post {
                        timeoutHandler()
                    }
                    break
                }
            }
        }
    }

    /**
     * Resets the heartbeat timer upon receiving a heartbeat signal.
     *
     * This method updates the last heartbeat timestamp to the current time.
     */
    override fun heartbeatReceived() {
        lastHeartbeatTime = System.currentTimeMillis()
    }

    /**
     * Stops the heartbeat monitoring.
     *
     * This method cancels the monitoring coroutine job if it exists.
     */
    override fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Cleans up resources when the HeartbeatManager is no longer needed.
     * This should be called from a lifecycle method (e.g., onDestroy) or when
     * the manager is no longer needed.
     */
    override fun cleanup() {
        stopMonitoring()
        scope.cancel()
    }
}