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

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pushlytic.sdk.Pushlytic
import com.pushlytic.sdk.server.interfaces.LifecycleManaging
import com.pushlytic.sdk.server.interfaces.LifecycleState
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages application lifecycle events for the Pushlytic SDK.
 *
 * This manager is responsible for:
 * - Monitoring application state transitions (foreground, background, terminated)
 * - Managing connection state based on application lifecycle events
 * - Ensuring proper cleanup of resources to avoid memory leaks
 * - Maintaining connection state across app process lifecycle
 *
 * The manager uses Android's ProcessLifecycleOwner to track application state changes,
 * enabling the SDK to handle connection management seamlessly based on app state.
 *
 * Thread Safety:
 * - All callbacks are guaranteed to be executed on the main thread
 * - State transitions are atomic and thread-safe
 * - Resource cleanup is synchronized
 *
 * Example internal usage:
 * ```kotlin
 * val lifecycleManager = LifecycleManager()
 *
 * // Cleanup when SDK is shut down
 * lifecycleManager.cleanup()
 * ```
 *
 * @since 1.0.0
 */
class LifecycleManagerImpl : LifecycleManaging, DefaultLifecycleObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val appState = AtomicReference(LifecycleState.Background)

    init {
        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        handleStateTransition(LifecycleState.Foreground)
    }

    override fun onStop(owner: LifecycleOwner) {
        handleStateTransition(LifecycleState.Background)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handleStateTransition(LifecycleState.Terminated)
    }

    /**
     * Handles application state transitions by managing SDK connection accordingly.
     *
     * @param newState The new application state
     */
    override fun handleStateTransition(newState: LifecycleState) {
        val previousState = appState.get()
        if (appState.compareAndSet(previousState, newState) && previousState != newState) {
            mainHandler.post {
                when (newState) {
                    LifecycleState.Foreground -> handleForegroundTransition()
                    LifecycleState.Background -> handleBackgroundTransition()
                    LifecycleState.Terminated -> handleTermination()
                }
            }
        }
    }

    /**
     * Handles foreground transition by reopening the stream if auto-reconnect is enabled.
     */
    private fun handleForegroundTransition() {
        if (Pushlytic.shouldAutoReconnect && Pushlytic.isStreamPreviouslyOpened) {
            Pushlytic.openMessageStream()
        }
    }

    /**
     * Handles background transition by ending the stream if it's currently connected.
     */
    private fun handleBackgroundTransition() {
        Pushlytic.endStream()
    }

    /**
     * Handles application termination by ensuring any active connections are properly closed.
     */
    private fun handleTermination() {
        Pushlytic.endStream()
    }

    /**
     * Cleans up resources and removes lifecycle observers.
     * Should be called when the SDK is being shut down.
     *
     * This method is thread-safe and ensures cleanup happens on the main thread.
     */
    override fun cleanup() {
        mainHandler.post {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }
    }
}