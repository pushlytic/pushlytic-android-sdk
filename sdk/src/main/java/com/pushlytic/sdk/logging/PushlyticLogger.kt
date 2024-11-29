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

package com.pushlytic.sdk.logging

/**
 * Interface for logging events and messages in the Pushlytic SDK.
 *
 * Developers can implement this interface to customize logging behavior.
 * The SDK provides a default implementation (`DefaultLogger`) that logs to the console.
 *
 * @since 1.0.0
 */
interface PushlyticLogger {

    /**
     * Logs an informational message.
     *
     * @param tag The tag associated with the log message
     * @param message The message to log
     */
    fun logInfo(tag: String, message: String)

    /**
     * Logs a warning message.
     *
     * @param tag The tag associated with the log message
     * @param message The message to log
     */
    fun logWarning(tag: String, message: String)

    /**
     * Logs an error message.
     *
     * @param tag The tag associated with the log message
     * @param message The message to log
     * @param throwable An optional throwable to log
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null)
}