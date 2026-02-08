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

package com.jkjamies.trapeze.features.summary.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class SummaryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenASummaryState_whenDisplayed_thenItShowsTheFinalCount() {
        val state = SummaryState(
            finalCount = 42,
            lastSavedValue = null,
            saveInProgress = false,
            eventSink = {}
        )

        composeTestRule.setContent {
            SummaryUi(state = state)
        }

        composeTestRule.onNodeWithText("Final Count: 42").assertExists()
    }

    @Test
    fun givenASummaryStateWithLastSavedValue_whenDisplayed_thenItShowsTheLastSavedValue() {
        val state = SummaryState(
            finalCount = 10,
            lastSavedValue = 7,
            saveInProgress = false,
            eventSink = {}
        )

        composeTestRule.setContent {
            SummaryUi(state = state)
        }

        composeTestRule.onNodeWithText("Last Saved: 7").assertExists()
    }

    @Test
    fun givenASummaryState_whenBackButtonIsClicked_thenBackEventIsEmitted() {
        var event: SummaryEvent? = null
        val state = SummaryState(
            finalCount = 0,
            lastSavedValue = null,
            saveInProgress = false,
            eventSink = { event = it }
        )

        composeTestRule.setContent {
            SummaryUi(state = state)
        }

        composeTestRule.onNodeWithText("Back").performClick()
        event shouldBe SummaryEvent.Back
    }

    @Test
    fun givenASummaryState_whenSaveValueButtonIsClicked_thenSaveValueEventIsEmitted() {
        var event: SummaryEvent? = null
        val state = SummaryState(
            finalCount = 0,
            lastSavedValue = null,
            saveInProgress = false,
            eventSink = { event = it }
        )

        composeTestRule.setContent {
            SummaryUi(state = state)
        }

        composeTestRule.onNodeWithText("Save Value").performClick()
        event shouldBe SummaryEvent.SaveValue
    }

    @Test
    fun givenASummaryState_whenPrintValueButtonIsClicked_thenPrintValueEventIsEmitted() {
        var event: SummaryEvent? = null
        val state = SummaryState(
            finalCount = 0,
            lastSavedValue = null,
            saveInProgress = false,
            eventSink = { event = it }
        )

        composeTestRule.setContent {
            SummaryUi(state = state)
        }

        composeTestRule.onNodeWithText("Print Value").performClick()
        event shouldBe SummaryEvent.PrintValue
    }
}
