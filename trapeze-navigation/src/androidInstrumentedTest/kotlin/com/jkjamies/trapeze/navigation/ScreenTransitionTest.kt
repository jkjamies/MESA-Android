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

package com.jkjamies.trapeze.navigation

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeCompositionLocals
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import com.jkjamies.trapeze.TrapezeStateHolder
import io.kotest.matchers.shouldBe
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class TransitionScreen(val name: String) : TrapezeScreen, Parcelable

private data class NamedState(val name: String, val entryId: String) : TrapezeState

private class NamedHolder(private val name: String) :
    TrapezeStateHolder<TransitionScreen, NamedState, TrapezeEvent>() {

    @Composable
    override fun produceState(): NamedState =
        NamedState(name, LocalTrapezeBackStackEntry.current.id)
}

/**
 * A function reference, not a composable lambda — `TrapezeContent` emits a real `CHECKCAST` to
 * the `TrapezeUi` function type, which `ComposableLambdaImpl` does not satisfy.
 *
 * Tags carry both the screen's name and the id of the entry it was rendered for, so a test can
 * assert *which visit* is on screen. Screen equality cannot answer that.
 */
@Composable
private fun NamedUi(modifier: Modifier, state: NamedState) {
    Box(modifier.fillMaxSize().testTag("screen:${state.name}")) {
        Box(Modifier.fillMaxSize().testTag("entry:${state.entryId}"))
    }
}

private fun trapeze(): Trapeze = Trapeze.Builder()
    .addStateHolderFactory { screen, _ ->
        if (screen is TransitionScreen) NamedHolder(screen.name) else null
    }
    .addUiFactory { screen -> if (screen is TransitionScreen) ::NamedUi else null }
    .build()

class ScreenTransitionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenATransition_whenNavigating_thenTheNewScreenIsDisplayedOnceItSettles() {
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze()) {
                val backStack = rememberSaveableBackStack(TransitionScreen("A"))
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }

        composeTestRule.onNodeWithTag("screen:A").assertIsDisplayed()

        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("B")) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("screen:B").assertIsDisplayed()
        composeTestRule.onNodeWithTag("screen:A").assertDoesNotExist()
    }

    @Test
    fun givenAnInFlightTransition_whenItIsHalfway_thenBothScreensAreStillComposed() {
        lateinit var navigator: TrapezeNavigator

        // Hand-drive the clock so the animation can be observed mid-flight. With autoAdvance on,
        // `waitForIdle` runs it to completion and there is nothing left to see.
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze()) {
                val backStack = rememberSaveableBackStack(TransitionScreen("A"))
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("B")) }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(HALFWAY_MILLIS)

        // The outgoing screen has not been torn down yet — this is what makes the animation an
        // animation rather than a swap.
        composeTestRule.onNodeWithTag("screen:A").assertExists()
        composeTestRule.onNodeWithTag("screen:B").assertExists()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("screen:A").assertDoesNotExist()
    }

    @Test
    fun givenAnInFlightTransition_whenBothScreensAreComposed_thenEachSeesItsOwnBackStackEntry() {
        // The reason `LocalTrapezeBackStackEntry` is provided inside the animation rather than
        // around it. Were the outgoing screen to see the incoming entry, it could consume
        // navigation results addressed to its successor.
        lateinit var navigator: TrapezeNavigator
        lateinit var backStack: TrapezeBackStack

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze()) {
                backStack = rememberSaveableBackStack(TransitionScreen("A"))
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        val outgoingId = backStack.currentEntry.id

        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("B")) }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(HALFWAY_MILLIS)

        val incomingId = backStack.currentEntry.id
        (outgoingId == incomingId) shouldBe false

        // Each rendered screen tagged itself with the entry it was actually handed.
        composeTestRule.onNodeWithTag("entry:$outgoingId").assertExists()
        composeTestRule.onNodeWithTag("entry:$incomingId").assertExists()

        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun givenTheNoneTransition_whenNavigating_thenTheSwapIsImmediate() {
        lateinit var navigator: TrapezeNavigator

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze()) {
                val backStack = rememberSaveableBackStack(TransitionScreen("A"))
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(
                    navigator = navigator,
                    backStack = backStack,
                    transition = TrapezeTransitions.None,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("B")) }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        // Same point in the clock at which the default transition still had both on screen.
        composeTestRule.onNodeWithTag("screen:B").assertExists()
        composeTestRule.onNodeWithTag("screen:A").assertDoesNotExist()

        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun givenAMultiEntryPop_whenTheStackMovesBack_thenTheTransitionIsToldItWentBackward() {
        val directions = mutableListOf<TrapezeNavigationDirection>()
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze()) {
                val backStack = rememberSaveableBackStack(TransitionScreen("A"))
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack) { direction ->
                    directions += direction
                    TrapezeTransitions.None(this, direction)
                }
            }
        }

        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("B")) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { navigator.navigate(TransitionScreen("C")) }
        composeTestRule.waitForIdle()

        // Asserted as a set rather than a sequence: `transitionSpec` may be evaluated more than
        // once per change, so counting invocations would be testing Compose, not MESA.
        directions.toSet() shouldBe setOf(TrapezeNavigationDirection.Forward)

        composeTestRule.runOnUiThread { navigator.popToRoot() }
        composeTestRule.waitForIdle()

        // Two entries left the stack at once; the move is still reported as one step backward.
        directions.last() shouldBe TrapezeNavigationDirection.Backward
    }

    private companion object {
        /** Half of `TrapezeTransitions`' 300ms, so the animation is genuinely mid-flight. */
        const val HALFWAY_MILLIS = 150L
    }
}
