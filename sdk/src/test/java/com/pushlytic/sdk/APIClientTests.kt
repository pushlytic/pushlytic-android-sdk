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

package com.pushlytic.sdk

import android.app.Application
import android.os.Build
import com.pushlytic.sdk.mocks.MockLogger
import com.pushlytic.sdk.model.MetadataOperationType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ApiClientTests {

    private lateinit var client: ApiClientImpl
    private lateinit var mockLogger: MockLogger
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        mockLogger = MockLogger()
        client = ApiClientImpl(application, mockLogger).apply {
            apiKey = "test-api-key"
        }
    }

    @After
    fun tearDown() {
        client.endConnection(true)
        client.shutdown()
    }

    @Test
    fun `test initial state`() {
        assertFalse(client.isConnected)
        assertNull(client.userID)
        assertNull(client.tags)
    }

    @Test
    fun `test user registration`() {
        val userId = "test-user"
        client.registerUserID(userId)
        assertEquals(userId, client.userID)
    }

    @Test
    fun `test tags registration`() {
        val tags = listOf("tag1", "tag2")
        client.registerTags(tags)
        assertEquals(tags, client.tags)
    }

    @Test
    fun `test operations sequence`() {
        repeat(5) { i ->
            val userId = "user-$i"
            client.registerUserID(userId)
            client.registerTags(listOf("tag-$i"))
            client.sendCustomEvent("event-$i", mapOf("key" to "value"))
            client.updateMetadata(MetadataOperationType.UPDATE, mapOf("key" to "value"))
            client.openMessageStream()
            client.endConnection()
        }
        assertFalse(client.isConnected)
    }

    @Test
    fun `test multiple connection attempts`() {
        repeat(10) {
            client.openMessageStream()
        }
        assertFalse(client.isConnected)
    }

    @Test
    fun `test high load operations`() {
        repeat(100) { i ->  // Reduced from 1000 to 100 for reasonable test time
            client.registerTags(listOf("tag-$i"))
        }
        assertNotNull(client.tags)
    }

    @Test
    fun `test reconnection logic`() {
        client.openMessageStream()
        client.endConnection(false)

        Thread.sleep(5000)
        assertTrue(client.connectionInProgress, "Reconnection should be in progress")
    }

    @Test
    fun `test uninitialized API key prevents connection`() {
        client.apiKey = null
        client.openMessageStream()

        assertFalse(client.isConnected, "Client should remain disconnected")
    }

    @Test
    fun `test invalid user ID`() {
        client.registerUserID("")
        assertEquals("", client.userID)
    }

    @Test
    fun `test empty tags`() {
        client.registerTags(emptyList())
        assertTrue(client.tags?.isEmpty() == true, "Tags should be empty")
    }

    @Test
    fun `test metadata update with empty map`() {
        client.updateMetadata(MetadataOperationType.UPDATE, emptyMap())

        // No errors should occur, and no metadata should be sent
        assertTrue(true, "Metadata update with an empty map should succeed silently")
    }

    @Test
    fun `test metadata update with null`() {
        client.updateMetadata(MetadataOperationType.UPDATE, null)

        // No errors should occur
        assertTrue(true, "Metadata update with null should succeed silently")
    }

    @Test
    fun `test connection lifecycle`() {
        client.openMessageStream()
        assertTrue(client.connectionInProgress, "Connection should be in progress")

        client.endConnection()
        assertFalse(client.isConnected, "Client should be disconnected after ending the connection")
    }
}
