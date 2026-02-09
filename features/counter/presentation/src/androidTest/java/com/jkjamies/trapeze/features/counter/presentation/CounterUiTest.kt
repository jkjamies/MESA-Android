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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jkjamies.trapeze.TrapezeMessage
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class CounterUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenACounterState_whenDisplayed_thenItShowsTheCount() {
        val state = CounterState(count = 7, eventSink = {})

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Count: 7").assertExists()
    }

    @Test
    fun givenACounterState_whenIncrementButtonIsClicked_thenIncrementEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("+").performClick()
        event shouldBe CounterEvent.Increment
    }

    @Test
    fun givenACounterState_whenDecrementButtonIsClicked_thenDecrementEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("-").performClick()
        event shouldBe CounterEvent.Decrement
    }

    @Test
    fun givenACounterState_whenDivideButtonIsClicked_thenDivideEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("/ 2").performClick()
        event shouldBe CounterEvent.Divide
    }

    @Test
    fun givenACounterState_whenGoToSummaryButtonIsClicked_thenGoToSummaryEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Go to Summary").performClick()
        event shouldBe CounterEvent.GoToSummary
    }

    @Test
    fun givenACounterState_whenGetHelpButtonIsClicked_thenGetHelpEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Get Help").performClick()
        event shouldBe CounterEvent.GetHelp
    }

    @Test
    fun givenACounterStateWithNoError_whenThrowErrorButtonIsClicked_thenThrowErrorEventIsEmitted() {
        var event: CounterEvent? = null
        val state = CounterState(count = 0, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Throw Error").performClick()
        event shouldBe CounterEvent.ThrowError
    }

    @Test
    fun givenACounterStateWithAnError_whenClearErrorButtonIsClicked_thenClearErrorEventIsEmitted() {
        val message = TrapezeMessage("test error")
        var event: CounterEvent? = null
        val state = CounterState(count = 0, trapezeMessage = message, eventSink = { event = it })

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Clear Error").performClick()
        (event as CounterEvent.ClearError).id shouldBe message.id
    }

    @Test
    fun givenACounterStateWithAnError_whenDisplayed_thenItShowsTheErrorMessage() {
        val state = CounterState(
            count = 0,
            trapezeMessage = TrapezeMessage("Something went wrong"),
            eventSink = {}
        )

        composeTestRule.setContent {
            CounterUi(state = state)
        }

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }
}
