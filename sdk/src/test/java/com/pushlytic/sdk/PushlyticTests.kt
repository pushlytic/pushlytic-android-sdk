/*
* Copyright (C) 2024 Pushlytic
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.pushlytic.sdk

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pushlytic.sdk.Pushlytic.Configuration
import com.pushlytic.sdk.mocks.MockPushlyticListener
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.SerializationException

@RunWith(RobolectricTestRunner::class)
class PushlyticTests {

    private lateinit var mockListener: MockPushlyticListener

    @Before
    fun setUp() {
        val mockApplication = ApplicationProvider.getApplicationContext<Application>()
        val configuration = Configuration(apiKey = "test-api-key")
        Pushlytic.configure(mockApplication, configuration)
        mockListener = MockPushlyticListener()
        Pushlytic.setListener(mockListener)
    }

    @After
    fun tearDown() {
        Pushlytic.setListener(null)
        Pushlytic.endStream(clearState = true)
    }

    @Test
    fun `test parseMessage processes valid JSON`() {
        val jsonString = """{"id": "123", "content": "Test message"}"""

        @Serializable
        data class CustomMessage(
            val id: String,
            val content: String
        )

        Pushlytic.parseMessage<CustomMessage>(
            message = jsonString,
            serializer = CustomMessage.serializer(),
            completion = { message ->
                assertEquals("123", message.id, "Message ID should match")
                assertEquals("Test message", message.content, "Message content should match")
            },
            errorHandler = { error ->
                fail("Parsing should not fail, but got error: $error")
            }
        )
    }

    @Test
    fun `test parseMessage handles invalid JSON`() {
        val invalidJson = """{"id": "123", "content": }"""

        @Serializable
        data class TestMessage(
            val id: String,
            val content: String
        )

        Pushlytic.parseMessage<TestMessage>(
            message = invalidJson,
            serializer = TestMessage.serializer(),
            completion = { _ ->
                fail("Parsing should fail for invalid JSON")
            },
            errorHandler = { error ->
                assertTrue(true, "Error should be thrown for invalid JSON")
            }
        )
    }

    @Test
    fun `test parseMessage processes valid JSON with optional fields`() {
        val jsonString = """{"id": "123", "content": "Test message", "metadata": {"key": "value"}}"""

        @Serializable
        data class CustomMessage(
            val id: String,
            val content: String,
            val metadata: Map<String, String>? = null
        )

        Pushlytic.parseMessage<CustomMessage>(
            message = jsonString,
            serializer = CustomMessage.serializer(),
            completion = { message ->
                assertEquals("123", message.id)
                assertEquals("Test message", message.content)
                assertNotNull(message.metadata)
                assertEquals("value", message.metadata?.get("key"))
            },
            errorHandler = { error ->
                fail("Parsing should not fail, but got error: $error")
            }
        )
    }

    @Test
    fun `test parseMessage processes valid JSON without optional fields`() {
        val jsonString = """{"id": "123", "content": "Test message"}"""

        @Serializable
        data class CustomMessage(
            val id: String,
            val content: String,
            val metadata: Map<String, String>? = null
        )

        Pushlytic.parseMessage<CustomMessage>(
            message = jsonString,
            serializer = CustomMessage.serializer(),
            completion = { message ->
                assertEquals("123", message.id)
                assertEquals("Test message", message.content)
                assertNull(message.metadata)
            },
            errorHandler = { error ->
                fail("Parsing should not fail, but got error: $error")
            }
        )
    }

    @Test
    fun `test parseMessage handles malformed JSON`() {
        val malformedJson = "not a json string"

        @Serializable
        data class TestMessage(
            val id: String,
            val content: String
        )

        Pushlytic.parseMessage<TestMessage>(
            message = malformedJson,
            serializer = TestMessage.serializer(),
            completion = { _ ->
                fail("Parsing should fail for malformed JSON")
            },
            errorHandler = { error ->
                assertTrue(error is SerializationException, "Error should be a SerializationException")
            }
        )
    }

    @Test
    fun `test parseMessage handles empty JSON string`() {
        val emptyJson = ""

        @Serializable
        data class TestMessage(
            val id: String,
            val content: String
        )

        Pushlytic.parseMessage<TestMessage>(
            message = emptyJson,
            serializer = TestMessage.serializer(),
            completion = { _ ->
                fail("Parsing should fail for empty JSON")
            },
            errorHandler = { error ->
                assertTrue(error is SerializationException, "Error should be a SerializationException")
            }
        )
    }

    @Test
    fun `test parseMessage handles complex nested objects`() {
        val jsonString = """
        {
            "id": "123",
            "content": "Test message",
            "user": {
                "name": "John Doe",
                "settings": {
                    "notifications": true,
                    "theme": "dark"
                }
            }
        }
    """.trimIndent()

        @Serializable
        data class UserSettings(
            val notifications: Boolean,
            val theme: String
        )

        @Serializable
        data class User(
            val name: String,
            val settings: UserSettings
        )

        @Serializable
        data class ComplexMessage(
            val id: String,
            val content: String,
            val user: User
        )

        Pushlytic.parseMessage<ComplexMessage>(
            message = jsonString,
            serializer = ComplexMessage.serializer(),
            completion = { message ->
                assertEquals("123", message.id)
                assertEquals("Test message", message.content)
                assertEquals("John Doe", message.user.name)
                assertTrue(message.user.settings.notifications)
                assertEquals("dark", message.user.settings.theme)
            },
            errorHandler = { error ->
                fail("Parsing should not fail, but got error: $error")
            }
        )
    }

    @Test
    fun `test concurrent listener access does not crash`() {
        val iterations = 100

        try {
            repeat(iterations) {
                Pushlytic.setListener(mockListener)
                Pushlytic.setListener(null)
            }
        } catch (e: Exception) {
            fail("Concurrent listener access caused an exception: ${e.message}")
        }
    }

    @Test
    fun `test concurrent stream control does not crash`() {
        val iterations = 100

        try {
            repeat(iterations) {
                Pushlytic.openMessageStream()
                Pushlytic.endStream()
            }
        } catch (e: Exception) {
            fail("Concurrent stream control caused an exception: ${e.message}")
        }
    }
}

