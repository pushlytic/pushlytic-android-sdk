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
 * Represents possible errors that can occur in the Pushlytic SDK.
 *
 * This sealed class provides detailed error cases to describe various issues that might arise
 * when using the SDK, including configuration problems, network issues, and server rejections.
 *
 * Example usage:
 * ```kotlin
 * when (val error = sdkError) {
 *     PushlyticError.NotConfigured -> promptUserToConfigureSDK()
 *     PushlyticError.NotAuthorized -> refreshApiKey()
 *     is PushlyticError.Custom -> log(error.message)
 * }
 * ```
 *
 * @since 1.0.0
 */
sealed class PushlyticError : Exception() {
    /**
     * SDK not properly configured (e.g., missing or invalid API key).
     */
    object NotConfigured : PushlyticError() {
        @JvmStatic
        private fun readResolve(): Any = NotConfigured
    }

    /**
     * Authorization failed (e.g., invalid API key).
     */
    object NotAuthorized : PushlyticError() {
        @JvmStatic
        private fun readResolve(): Any = NotAuthorized
    }

    /**
     * Network connection lost, possibly due to connectivity issues.
     */
    object ConnectionLost : PushlyticError() {
        @JvmStatic
        private fun readResolve(): Any = ConnectionLost
    }

    /**
     * Invalid message format received by the SDK.
     */
    object InvalidMessage : PushlyticError() {
        @JvmStatic
        private fun readResolve(): Any = InvalidMessage
    }

    /**
     * Server rejected the connection request.
     */
    object ConnectionRejected : PushlyticError() {
        @JvmStatic
        private fun readResolve(): Any = ConnectionRejected
    }

    /**
     * Custom error with an associated description message.
     *
     * @property message The detailed error description
     */
    data class Custom(override val message: String) : PushlyticError()

    /**
     * Returns a human-readable error description.
     */
    override val message: String
        get() = when (this) {
            is NotConfigured -> "SDK not properly configured. Please ensure you've called configure() with a valid API key."
            is NotAuthorized -> "Authorization failed. Please check your API key and try again."
            is ConnectionLost -> "Network connection lost. Please check your internet connection and try again."
            is InvalidMessage -> "Invalid message format received. Please verify the message content."
            is ConnectionRejected -> "Connection rejected by the server. Please try again later or contact support if the issue persists."
            is Custom -> message
        }
}