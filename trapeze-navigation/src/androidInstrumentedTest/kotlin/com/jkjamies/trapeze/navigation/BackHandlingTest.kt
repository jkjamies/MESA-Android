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
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeCompositionLocals
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.TrapezeUi
import io.kotest.matchers.shouldBe
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class BackScreen(val id: Int) : TrapezeScreen, Parcelable

private object EmptyState : TrapezeState

private class EmptyHolder : TrapezeStateHolder<BackScreen, TrapezeState, TrapezeEvent>() {
    @Composable
    override fun produceState(): TrapezeState = EmptyState
}

class BackHandlingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun trapeze(): Trapeze {
        val ui: TrapezeUi<TrapezeState> = @Composable { _: Modifier, _: TrapezeState -> }
        return Trapeze.Builder()
            .addStateHolderFactory { screen, _ -> if (screen is BackScreen) EmptyHolder() else null }
            .addUiFactory { screen -> if (screen is BackScreen) ui else null }
            .build()
    }

    private fun pressBack() {
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun givenMoreThanOneScreen_whenBackIsPressed_thenTheBackStackPops() {
        val backStack = TrapezeBackStack(BackScreen(1))
        val trapeze = trapeze()

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                val navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }

        composeTestRule.runOnIdle { backStack.push(BackScreen(2)) }
        composeTestRule.runOnIdle { backStack.size shouldBe 2 }

        pressBack()

        composeTestRule.runOnIdle {
            backStack.size shouldBe 1
            backStack.current shouldBe BackScreen(1)
        }
    }

    @Test
    fun givenTheRootScreen_whenBackIsPressed_thenTrapezeDoesNotConsumeIt() {
        val backStack = TrapezeBackStack(BackScreen(1))
        val trapeze = trapeze()
        var hostHandledBack = false

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                val navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }

        // Registered after Trapeze's handler, so it wins only if Trapeze stays disabled at root.
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.addCallback(
                activity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        hostHandledBack = true
                    }
                }
            )
        }

        pressBack()

        composeTestRule.runOnIdle {
            hostHandledBack shouldBe true
            backStack.size shouldBe 1
        }
    }

    @Test
    fun givenBackHandlingIsDisabled_whenBackIsPressed_thenTheBackStackIsUntouched() {
        val backStack = TrapezeBackStack(BackScreen(1))
        val trapeze = trapeze()

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                val navigator: TrapezeNavigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack, handleBack = false)
            }
        }

        composeTestRule.runOnIdle { backStack.push(BackScreen(2)) }

        pressBack()

        composeTestRule.runOnIdle { backStack.size shouldBe 2 }
    }
}
