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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.pushlytic.sdk.server.LifecycleManagerImpl
import com.pushlytic.sdk.server.interfaces.LifecycleState
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LifecycleManagerTests {
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val lifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
    private lateinit var lifecycleManager: LifecycleManagerImpl
    private val mockProcessLifecycleOwner = mockk<ProcessLifecycleOwner>()

    @Before
    fun setup() {
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
        every { lifecycleOwner.lifecycle } returns lifecycleRegistry

        mockkObject(ProcessLifecycleOwner)
        every { ProcessLifecycleOwner.get() } returns mockProcessLifecycleOwner
        every { mockProcessLifecycleOwner.lifecycle } returns lifecycleRegistry

        lifecycleManager = spyk(LifecycleManagerImpl())
        lifecycleRegistry.addObserver(lifecycleManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test onStart triggers foreground state`() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify { lifecycleManager.handleStateTransition(LifecycleState.Foreground) }
    }

    @Test
    fun `test onStop triggers background state`() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        verify { lifecycleManager.handleStateTransition(LifecycleState.Background) }
    }

    @Test
    fun `test onDestroy triggers foreground state`() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        verify { lifecycleManager.handleStateTransition(LifecycleState.Terminated) }
    }
}



