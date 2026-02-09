/*
 * Copyright 2026 Jason Jamieson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkjamies.trapeze.features.counter.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.trapeze.features.counter.presentation.test.FakeAppInterop
import com.jkjamies.trapeze.features.counter.presentation.test.FakeTrapezeNavigator
import com.jkjamies.trapeze.features.summary.presentation.SummaryScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Rule
import org.junit.Test

class CounterStateHolderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createHolder(
        navigator: FakeTrapezeNavigator = FakeTrapezeNavigator(),
        interop: FakeAppInterop = FakeAppInterop()
    ) = Triple(CounterStateHolder(interop, navigator), navigator, interop)

    @Test
    fun givenACounterScreen_whenProduceStateIsCalled_thenInitialCountMatchesScreen() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen(initialCount = 5))
        }

        state.count shouldBe 5
    }

    @Test
    fun givenACounterScreen_whenIncrementEventIsSent_thenCountIncreases() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen(initialCount = 0))
        }

        state.eventSink(CounterEvent.Increment)
        composeTestRule.waitForIdle()

        state.count shouldBe 1
    }

    @Test
    fun givenACounterScreen_whenDecrementEventIsSent_thenCountDecreases() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen(initialCount = 5))
        }

        state.eventSink(CounterEvent.Decrement)
        composeTestRule.waitForIdle()

        state.count shouldBe 4
    }

    @Test
    fun givenACounterScreen_whenDivideEventIsSent_thenCountIsHalved() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen(initialCount = 10))
        }

        state.eventSink(CounterEvent.Divide)
        composeTestRule.waitForIdle()

        state.count shouldBe 5
    }

    @Test
    fun givenACounterScreen_whenGoToSummaryEventIsSent_thenNavigatorReceivesSummaryScreen() {
        val navigator = FakeTrapezeNavigator()
        val holder = CounterStateHolder(FakeAppInterop(), navigator)

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen(initialCount = 42))
        }

        state.eventSink(CounterEvent.GoToSummary)
        composeTestRule.waitForIdle()

        navigator.screens.last().shouldBeInstanceOf<SummaryScreen>()
        (navigator.screens.last() as SummaryScreen).finalCount shouldBe 42
    }

    @Test
    fun givenACounterScreen_whenGetHelpEventIsSent_thenInteropReceivesEvent() {
        val interop = FakeAppInterop()
        val holder = CounterStateHolder(interop, FakeTrapezeNavigator())

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen())
        }

        state.eventSink(CounterEvent.GetHelp)
        composeTestRule.waitForIdle()

        interop.events.size shouldBe 1
    }

    @Test
    fun givenACounterScreen_whenThrowErrorEventIsSent_thenTrapezeMessageAppears() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen())
        }

        state.trapezeMessage.shouldBeNull()

        state.eventSink(CounterEvent.ThrowError)
        composeTestRule.waitForIdle()

        state.trapezeMessage.shouldNotBeNull()
        state.trapezeMessage!!.message shouldBe "Simulated Failure"
    }

    @Test
    fun givenACounterScreenWithAnError_whenClearErrorEventIsSent_thenMessageIsCleared() {
        val (holder, _, _) = createHolder()

        lateinit var state: CounterState
        composeTestRule.setContent {
            state = holder.produceState(CounterScreen())
        }

        state.eventSink(CounterEvent.ThrowError)
        composeTestRule.waitForIdle()

        val messageId = state.trapezeMessage!!.id
        state.eventSink(CounterEvent.ClearError(messageId))
        composeTestRule.waitForIdle()

        state.trapezeMessage.shouldBeNull()
    }
}
