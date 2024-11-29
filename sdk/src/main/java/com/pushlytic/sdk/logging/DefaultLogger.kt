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

import android.util.Log

/**
 * Default implementation of the `PushlyticLogger` interface.
 * Logs messages using Android's `Log` class.
 *
 * @since 1.0.0
 */
class DefaultLogger : PushlyticLogger {

    override fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
