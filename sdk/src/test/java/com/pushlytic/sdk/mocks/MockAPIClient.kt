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

import com.pushlytic.sdk.model.MessageStreamState
import com.pushlytic.sdk.model.MetadataOperationType
import com.pushlytic.sdk.server.interfaces.ApiClient
import com.pushlytic.sdk.server.interfaces.MessageStreamListener

class MockApiClient(override var apiKey: String?) : ApiClient {
    private var listener: MessageStreamListener? = null
    override var isConnected = false

    override fun setMessageStreamListener(listener: MessageStreamListener) {
        this.listener = listener
    }

    override fun openMessageStream() {
        isConnected = true
        listener?.onStateChanged(MessageStreamState.Connected)
    }

    override fun endConnection(wasManuallyDisconnected: Boolean) {
        //No-op
    }

    override fun registerUserID(userID: String) {
        //No-op
    }

    override fun registerTags(tags: List<String>) {
        //No-op
    }

    override fun shutdown() {
        //No-op
    }

    override fun sendCustomEvent(name: String, metadata: Map<String, Any>?) {
        //No-op
    }

    override fun updateMetadata(type: MetadataOperationType, metadata: Map<String, Any>?) {
        //No-op
    }
}
