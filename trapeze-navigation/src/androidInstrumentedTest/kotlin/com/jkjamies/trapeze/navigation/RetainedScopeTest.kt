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
import com.jkjamies.trapeze.rememberRetained
import com.jkjamies.trapeze.rememberRetainedCoroutineScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class RetainScreen(val id: Int) : TrapezeScreen, Parcelable

private class RetainState(
    val instanceId: Int,
    val scope: CoroutineScope
) : TrapezeState

/** Retains a counter-stamped value so re-creation is directly observable. */
private class RetainHolder(private val nextId: () -> Int) :
    TrapezeStateHolder<RetainScreen, RetainState, TrapezeEvent>() {
    @Composable
    override fun produceState(): RetainState {
        val instanceId = rememberRetained("instance") { nextId() }
        return RetainState(instanceId, rememberRetainedCoroutineScope())
    }
}

class RetainedScopeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var created = 0
    private val states = mutableListOf<RetainState>()

    /**
     * Built once per test and held in a field — rebuilding it inside `setContent` would hand
     * `TrapezeContent` a new registry on every recomposition and defeat its remember keys.
     */
    private fun trapeze(): Trapeze {
        val ui: TrapezeUi<RetainState> = @Composable { _: Modifier, state: RetainState ->
            if (states.none { it === state }) states += state
        }
        return Trapeze.Builder()
            .addStateHolderFactory { screen, _ ->
                if (screen is RetainScreen) RetainHolder { ++created } else null
            }
            .addUiFactory { screen -> if (screen is RetainScreen) ui else null }
            .build()
    }

    private fun setContent(backStack: TrapezeBackStack, onNavigator: (TrapezeNavigator) -> Unit) {
        val trapeze = trapeze()
        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                val navigator = rememberTrapezeNavigator(backStack)
                onNavigator(navigator)
                NavigableTrapezeContent(navigator, backStack)
            }
        }
    }

    @Test
    fun givenARetainedValue_whenNavigatingAwayAndBack_thenTheEntryKeepsIt() {
        val backStack = TrapezeBackStack(RetainScreen(1))
        val rootId = backStack.currentEntry.id
        lateinit var navigator: TrapezeNavigator

        setContent(backStack) { navigator = it }

        composeTestRule.runOnIdle { created shouldBe 1 }
        composeTestRule.runOnIdle { navigator.navigate(RetainScreen(2)) }
        composeTestRule.runOnIdle { created shouldBe 2 }

        composeTestRule.runOnIdle { navigator.pop() }

        composeTestRule.runOnIdle {
            // The root's StateHolder is rebuilt when it comes back into view, but its retained
            // store survived, so the retained value is not recomputed.
            created shouldBe 2
            backStack.currentEntry.id shouldBe rootId
        }
    }

    @Test
    fun givenARetainedScope_whenItsEntryIsPopped_thenTheScopeIsCancelled() {
        val backStack = TrapezeBackStack(RetainScreen(1))
        lateinit var navigator: TrapezeNavigator

        setContent(backStack) { navigator = it }

        composeTestRule.runOnIdle { navigator.navigate(RetainScreen(2)) }

        val pushedScope = composeTestRule.runOnIdle {
            states.size shouldBe 2
            states[1].scope.also { it.isActive shouldBe true }
        }

        composeTestRule.runOnIdle { navigator.pop() }

        composeTestRule.runOnIdle {
            // Popping is the screen going away for good — in-flight work must be cancelled.
            pushedScope.isActive shouldBe false
        }
    }

    @Test
    fun givenARetainedScope_whenTheScreenMerelyLeavesComposition_thenItStaysAlive() {
        val backStack = TrapezeBackStack(RetainScreen(1))
        lateinit var navigator: TrapezeNavigator

        setContent(backStack) { navigator = it }

        val rootScope = composeTestRule.runOnIdle { states[0].scope }

        composeTestRule.runOnIdle { navigator.navigate(RetainScreen(2)) }

        composeTestRule.runOnIdle {
            // The root is off-screen but still on the backstack, so work it started keeps running.
            // This is the same mechanism that carries work across a configuration change.
            rootScope.isActive shouldBe true
        }
    }

    @Test
    fun givenTwoEntries_thenEachGetsItsOwnRetainedScope() {
        val backStack = TrapezeBackStack(RetainScreen(1))
        lateinit var navigator: TrapezeNavigator

        setContent(backStack) { navigator = it }

        composeTestRule.runOnIdle { navigator.navigate(RetainScreen(2)) }

        composeTestRule.runOnIdle {
            states.size shouldBe 2
            (states[0].scope === states[1].scope) shouldBe false
        }
    }
}
