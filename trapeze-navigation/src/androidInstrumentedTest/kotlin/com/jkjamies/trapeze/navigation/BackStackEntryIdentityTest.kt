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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeCompositionLocals
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.TrapezeUi
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class IdScreen(val id: Int) : TrapezeScreen, Parcelable

private data class CounterState(val count: Int, val bump: () -> Unit) : TrapezeState

/** Holds a `rememberSaveable` counter, so shared saveable state is directly observable. */
private class CounterHolder : TrapezeStateHolder<IdScreen, CounterState, TrapezeEvent>() {
    @Composable
    override fun produceState(): CounterState {
        var count by rememberSaveable { mutableIntStateOf(0) }
        return CounterState(count) { count++ }
    }
}

class BackStackEntryIdentityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var latest: CounterState? = null

    private fun trapeze(): Trapeze {
        val ui: TrapezeUi<CounterState> = @Composable { _: Modifier, state: CounterState ->
            latest = state
        }
        return Trapeze.Builder()
            .addStateHolderFactory { screen, _ -> if (screen is IdScreen) CounterHolder() else null }
            .addUiFactory { screen -> if (screen is IdScreen) ui else null }
            .build()
    }

    @Test
    fun givenTwoPushesOfAnEqualScreen_thenTheEntriesHaveDistinctIds() {
        val backStack = TrapezeBackStack(IdScreen(1))
        backStack.push(IdScreen(1))

        val (first, second) = backStack.entries
        first.screen shouldBe second.screen
        first.id shouldNotBe second.id
        first shouldNotBe second
    }

    @Test
    fun givenEqualScreensOnTheStack_whenNavigatingBetweenThem_thenSavedUiStateIsNotShared() {
        val backStack = TrapezeBackStack(IdScreen(1))
        lateinit var navigator: TrapezeNavigator
        val trapeze = trapeze()

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }

        composeTestRule.runOnIdle { latest!!.bump() }
        composeTestRule.runOnIdle { latest!!.count shouldBe 1 }

        // Navigate to an *equal* screen. Keyed by screen value this would resume the root
        // entry's saved state; keyed by entry id it must start fresh.
        composeTestRule.runOnIdle { navigator.navigate(IdScreen(1)) }
        composeTestRule.runOnIdle { latest!!.count shouldBe 0 }

        // Going back restores the root entry's own state.
        composeTestRule.runOnIdle { navigator.pop() }
        composeTestRule.runOnIdle { latest!!.count shouldBe 1 }
    }

    @Test
    fun givenAPoppedEntry_whenItsScreenIsPushedAgain_thenSavedStateWasReleased() {
        val backStack = TrapezeBackStack(IdScreen(1))
        lateinit var navigator: TrapezeNavigator
        val trapeze = trapeze()

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                navigator = rememberTrapezeNavigator(backStack)
                NavigableTrapezeContent(navigator, backStack)
            }
        }

        composeTestRule.runOnIdle { navigator.navigate(IdScreen(2)) }
        composeTestRule.runOnIdle { latest!!.bump() }
        composeTestRule.runOnIdle { latest!!.count shouldBe 1 }

        composeTestRule.runOnIdle { navigator.pop() }
        composeTestRule.runOnIdle { navigator.navigate(IdScreen(2)) }

        // A fresh entry: the popped one's saved state must have been released.
        composeTestRule.runOnIdle { latest!!.count shouldBe 0 }
    }

    @Test
    fun givenABackStack_whenSavedAndRestored_thenEntryIdsArePreserved() {
        val original = TrapezeBackStack(IdScreen(1))
        original.push(IdScreen(1))
        val originalIds = original.entries.map { it.id }

        @Suppress("UNCHECKED_CAST")
        val saver = TrapezeBackStack.saver() as androidx.compose.runtime.saveable.Saver<TrapezeBackStack, Any>
        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saved?.let { saver.restore(it) }

        restored!!.entries.map { it.id } shouldBe originalIds
    }
}
