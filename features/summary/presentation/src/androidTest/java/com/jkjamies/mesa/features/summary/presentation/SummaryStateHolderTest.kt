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

package com.jkjamies.mesa.features.summary.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.mesa.features.summary.presentation.test.FakeSaveSummaryValue
import com.jkjamies.mesa.features.summary.presentation.test.FakeObserveLastSavedValue
import com.jkjamies.mesa.features.summary.presentation.test.FakeTrapezeNavigator
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class SummaryStateHolderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenASummaryScreen_whenProduceStateIsCalled_thenInitialStateShowsFinalCount() {
        val navigator = FakeTrapezeNavigator()
        val save = FakeSaveSummaryValue()
        val observe = FakeObserveLastSavedValue()
        val holder = SummaryStateHolder(navigator, lazy { save }, lazy { observe })

        lateinit var state: SummaryState
        composeTestRule.setContent {
            state = holder.produceState(SummaryScreen(finalCount = 42))
        }

        state.finalCount shouldBe 42
    }

    @Test
    fun givenASummaryScreen_whenBackEventIsSent_thenNavigatorPopIsCalled() {
        val navigator = FakeTrapezeNavigator()
        val save = FakeSaveSummaryValue()
        val observe = FakeObserveLastSavedValue()
        val holder = SummaryStateHolder(navigator, lazy { save }, lazy { observe })

        lateinit var state: SummaryState
        composeTestRule.setContent {
            state = holder.produceState(SummaryScreen(finalCount = 10))
        }

        state.eventSink(SummaryEvent.Back)
        composeTestRule.waitForIdle()

        navigator.events.last() shouldBe "Pop"
    }

    @Test
    fun givenASummaryScreen_whenSaveValueSucceeds_thenSuccessMessageIsEmitted() {
        val navigator = FakeTrapezeNavigator()
        val save = FakeSaveSummaryValue(shouldFail = false)
        val observe = FakeObserveLastSavedValue()
        val holder = SummaryStateHolder(navigator, lazy { save }, lazy { observe })

        lateinit var state: SummaryState
        composeTestRule.setContent {
            state = holder.produceState(SummaryScreen(finalCount = 5))
        }

        state.eventSink(SummaryEvent.SaveValue)
        composeTestRule.waitUntil(timeoutMillis = 3_000) { state.trapezeMessage != null }

        state.trapezeMessage!!.message.contains("successfully") shouldBe true
    }

    @Test
    fun givenASummaryScreen_whenSaveValueFails_thenFailureMessageIsEmitted() {
        val navigator = FakeTrapezeNavigator()
        val save = FakeSaveSummaryValue(shouldFail = true)
        val observe = FakeObserveLastSavedValue()
        val holder = SummaryStateHolder(navigator, lazy { save }, lazy { observe })

        lateinit var state: SummaryState
        composeTestRule.setContent {
            state = holder.produceState(SummaryScreen(finalCount = 5))
        }

        state.eventSink(SummaryEvent.SaveValue)
        composeTestRule.waitUntil(timeoutMillis = 3_000) { state.trapezeMessage != null }

        state.trapezeMessage!!.message.contains("failed") shouldBe true
    }
}
