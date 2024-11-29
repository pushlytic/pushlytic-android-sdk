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

import android.os.Looper
import com.pushlytic.sdk.server.HeartbeatManagerImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HeartbeatManagerImplTest {

    private lateinit var heartbeatManager: HeartbeatManagerImpl
    private lateinit var mockTimeoutHandler: () -> Unit
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockTimeoutHandler = mock()
        heartbeatManager = HeartbeatManagerImpl(mockTimeoutHandler)
    }

    @After
    fun cleanup() {
        heartbeatManager.cleanup()
        unmockkAll()
    }

    @Test
    fun `test stopMonitoring prevents timeout`() = runBlocking {
        val intervalSeconds = 1L

        heartbeatManager.startMonitoring(intervalSeconds)
        heartbeatManager.stopMonitoring()
        testDispatcher.scheduler.advanceTimeBy(intervalSeconds * 1000 + 10)

        verify(mockTimeoutHandler, never()).invoke()
    }

    @Test
    fun `test cleanup stops all monitoring`() = runBlocking {
        val intervalSeconds = 1L

        heartbeatManager.startMonitoring(intervalSeconds)
        heartbeatManager.cleanup()
        testDispatcher.scheduler.advanceTimeBy(intervalSeconds * 1000 + 10)

        verify(mockTimeoutHandler, never()).invoke()
    }

    @Test
    fun `test concurrent start and stop monitoring`() = runBlocking {
        val intervalSeconds = 1L
        repeat(100) {
            heartbeatManager.startMonitoring(intervalSeconds)
            heartbeatManager.stopMonitoring()
        }
        testDispatcher.scheduler.advanceTimeBy(intervalSeconds * 1000 + 10)

        verify(mockTimeoutHandler, never()).invoke()
    }
}
