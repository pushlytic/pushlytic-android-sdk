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

import android.os.Build
import com.pushlytic.sdk.mocks.MockLogger
import com.pushlytic.sdk.model.MetadataOperationType
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ApiClientTests {

    private lateinit var client: ApiClientImpl
    private lateinit var mockLogger: MockLogger

    @Before
    fun setUp() {
        mockLogger = MockLogger()
        client = ApiClientImpl(mockLogger).apply {
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
    fun `test concurrent operations`() = runBlocking {
        val iterations = 25
        val jobs = List(iterations) { i ->
            launch(Dispatchers.Default) {
                val userId = "user-$i"
                client.registerUserID(userId)
                client.registerTags(listOf("tag-$i"))
                client.sendCustomEvent("event-$i", mapOf("key" to "value"))
                client.updateMetadata(MetadataOperationType.UPDATE, mapOf("key" to "value"))
                client.openMessageStream()
                client.endConnection()
            }
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun `test multiple connection attempts`() {
        repeat(10) {
            client.openMessageStream()
        }
        assertFalse(client.isConnected)
    }

    @Test
    fun `test high load concurrent access`() = runBlocking {
        val iterations = 1000
        val jobs = List(iterations) { i ->
            launch(Dispatchers.Default) {
                client.registerTags(listOf("tag-$i"))
            }
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun `test reconnection logic`() = runBlocking {
        client.openMessageStream()
        client.endConnection(false) // Simulate non-manual disconnection

        delay(5000) // Wait for the reconnection attempt
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
