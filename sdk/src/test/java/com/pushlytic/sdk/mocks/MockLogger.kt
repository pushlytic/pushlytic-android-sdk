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

package com.pushlytic.sdk.mocks

import com.pushlytic.sdk.logging.PushlyticLogger

/**
 * Mock implementation of the PushlyticLogger interface for testing purposes.
 */
class MockLogger : PushlyticLogger {

    data class LogEntry(
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )

    private val infoLogs = mutableListOf<LogEntry>()
    private val warningLogs = mutableListOf<LogEntry>()
    private val errorLogs = mutableListOf<LogEntry>()

    override fun logInfo(tag: String, message: String) {
        infoLogs.add(LogEntry(tag, message))
    }

    override fun logWarning(tag: String, message: String) {
        warningLogs.add(LogEntry(tag, message))
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        errorLogs.add(LogEntry(tag, message, throwable))
    }

    fun getInfoLogs(): List<LogEntry> = infoLogs.toList()

    fun getWarningLogs(): List<LogEntry> = warningLogs.toList()

    fun getErrorLogs(): List<LogEntry> = errorLogs.toList()

    fun clearLogs() {
        infoLogs.clear()
        warningLogs.clear()
        errorLogs.clear()
    }
}
