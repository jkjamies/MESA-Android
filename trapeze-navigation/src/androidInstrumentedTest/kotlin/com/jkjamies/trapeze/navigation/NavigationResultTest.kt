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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class ResultScreen(val id: Int) : TrapezeScreen, Parcelable

@Parcelize
private data class TestResult(val value: String) : TrapezeNavigationResult, Parcelable

class NavigationResultTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Renders result consumers as `NavigableTrapezeContent` would, scoped to an entry. */
    @Composable
    private fun WithBackStack(
        backStack: TrapezeBackStack,
        entry: TrapezeBackStackEntry = backStack.currentEntry,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalTrapezeBackStack provides backStack,
            LocalTrapezeBackStackEntry provides entry,
            content = content
        )
    }

    // --- TrapezeBackStack result tests ---

    @Test
    fun givenABackStack_whenPopWithResultIsCalled_thenResultIsAddressedToTheRevealedEntry() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val target = backStack.currentEntry
        backStack.push(ResultScreen(2))

        val popped = backStack.popWithResult("key", TestResult("hello"))

        popped shouldBe true
        backStack.current shouldBe ResultScreen(1)
        backStack.consumeResult(target.id, "key")
            .shouldBeInstanceOf<TestResult>().value shouldBe "hello"
    }

    @Test
    fun givenAResultIsSet_whenConsumeResultIsCalled_thenItReturnsAndRemovesTheResult() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val entryId = backStack.currentEntry.id

        backStack.setResult(entryId, "key", TestResult("data"))

        backStack.consumeResult(entryId, "key")
            .shouldBeInstanceOf<TestResult>().value shouldBe "data"
        backStack.consumeResult(entryId, "key").shouldBeNull()
    }

    @Test
    fun givenTwoEntriesUsingTheSameKey_whenResultsAreSet_thenTheyDoNotCollide() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val first = backStack.currentEntry
        backStack.push(ResultScreen(2))
        val second = backStack.currentEntry

        backStack.setResult(first.id, "result", TestResult("for-first"))
        backStack.setResult(second.id, "result", TestResult("for-second"))

        backStack.consumeResult(first.id, "result")
            .shouldBeInstanceOf<TestResult>().value shouldBe "for-first"
        backStack.consumeResult(second.id, "result")
            .shouldBeInstanceOf<TestResult>().value shouldBe "for-second"
    }

    @Test
    fun givenAnUnconsumedResult_whenItsEntryIsPopped_thenTheResultIsDiscarded() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))
        val doomed = backStack.currentEntry
        backStack.setResult(doomed.id, "key", TestResult("orphan"))

        backStack.pop()

        backStack.resultsSnapshot() shouldBe emptyMap()
    }

    @Test
    fun givenUnconsumedResults_whenPopToRootDropsEntries_thenTheirResultsAreDiscarded() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val root = backStack.currentEntry
        backStack.push(ResultScreen(2))
        backStack.setResult(backStack.currentEntry.id, "key", TestResult("orphan"))
        backStack.push(ResultScreen(3))
        backStack.setResult(backStack.currentEntry.id, "key", TestResult("orphan2"))
        backStack.setResult(root.id, "keep", TestResult("kept"))

        backStack.popToRoot()

        backStack.resultsSnapshot().keys shouldBe setOf(root.id)
        backStack.consumeResult(root.id, "keep")
            .shouldBeInstanceOf<TestResult>().value shouldBe "kept"
    }

    @Test
    fun givenABackStackWithResults_whenSavedAndRestored_thenResultsSurvive() {
        val original = TrapezeBackStack(ResultScreen(1))
        val target = original.currentEntry
        original.push(ResultScreen(2))
        original.setResult(target.id, "key", TestResult("persisted"))

        @Suppress("UNCHECKED_CAST")
        val saver = TrapezeBackStack.saver() as androidx.compose.runtime.saveable.Saver<TrapezeBackStack, Any>
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(original) }
        val restored = saved?.let { saver.restore(it) }

        restored!!.size shouldBe 2
        // Entry identity survives, so the restored result is still addressed correctly.
        restored.entries.first().id shouldBe target.id
        restored.consumeResult(target.id, "key")
            .shouldBeInstanceOf<TestResult>().value shouldBe "persisted"
    }

    // --- Navigator popWithResult tests ---

    @Test
    fun givenANavigator_whenPopWithResultIsCalled_thenItDelegatesToBackStack() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val target = backStack.currentEntry
        backStack.push(ResultScreen(2))
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            navigator = rememberTrapezeNavigator(backStack)
        }

        composeTestRule.runOnIdle {
            navigator.popWithResult("key", TestResult("nav_result"))
        }

        composeTestRule.runOnIdle {
            backStack.current shouldBe ResultScreen(1)
            backStack.consumeResult(target.id, "key")
                .shouldBeInstanceOf<TestResult>().value shouldBe "nav_result"
        }
    }

    @Test
    fun givenANavigatorAtRoot_whenPopWithResultIsCalled_thenOnRootPopIsInvoked() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        var rootPopCalled = false
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            navigator = rememberTrapezeNavigator(backStack) { rootPopCalled = true }
        }

        composeTestRule.runOnIdle {
            navigator.popWithResult("key", TestResult("root"))
        }

        composeTestRule.runOnIdle {
            rootPopCalled shouldBe true
            backStack.size shouldBe 1
            // The result is dropped rather than retained: with nothing left to pop to,
            // no screen could ever consume it.
            backStack.resultsSnapshot() shouldBe emptyMap()
        }
    }

    @Test
    fun givenABackStackAtRoot_whenPopWithResultIsCalled_thenNothingIsStored() {
        val backStack = TrapezeBackStack(ResultScreen(1))

        val popped = backStack.popWithResult("key", TestResult("root"))

        popped shouldBe false
        backStack.size shouldBe 1
        backStack.resultsSnapshot() shouldBe emptyMap()
    }

    // --- rememberNavigationResult tests ---

    @Test
    fun givenAResult_whenRememberNavigationResultIsCalled_thenItReturnsTheResult() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))
        // Simulate Screen B popping back to Screen A with a result.
        backStack.popWithResult("edit_result", TestResult("composed"))
        var capturedResult: TrapezeNavigationResult? = null

        composeTestRule.setContent {
            WithBackStack(backStack) {
                capturedResult = rememberNavigationResult("edit_result")
            }
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeInstanceOf<TestResult>().value shouldBe "composed"
            // Delivery removes it from the backstack.
            backStack.resultsSnapshot() shouldBe emptyMap()
        }
    }

    @Test
    fun givenAResultArrivesLater_whenRememberNavigationResultIsComposed_thenItIsDelivered() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))
        val consumer = backStack.entries.first()
        var capturedResult: TrapezeNavigationResult? = null

        composeTestRule.setContent {
            WithBackStack(backStack, entry = consumer) {
                capturedResult = rememberNavigationResult("edit_result")
            }
        }

        composeTestRule.runOnIdle { capturedResult.shouldBeNull() }

        composeTestRule.runOnIdle {
            backStack.popWithResult("edit_result", TestResult("late"))
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeInstanceOf<TestResult>().value shouldBe "late"
        }
    }

    @Test
    fun givenAResultForAnotherEntry_whenRememberNavigationResultIsCalled_thenItIsNotDelivered() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        val root = backStack.currentEntry
        backStack.push(ResultScreen(2))
        val top = backStack.currentEntry
        backStack.setResult(root.id, "shared_key", TestResult("not-yours"))
        var capturedResult: TrapezeNavigationResult? = null

        composeTestRule.setContent {
            WithBackStack(backStack, entry = top) {
                capturedResult = rememberNavigationResult("shared_key")
            }
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeNull()
            // Still waiting for the entry it was actually addressed to.
            backStack.peekResult(root.id, "shared_key").shouldBeInstanceOf<TestResult>()
        }
    }

    @Test
    fun givenAResult_whenNavigationResultEffectIsUsed_thenItFiresExactlyOnce() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))
        backStack.popWithResult("edit_result", TestResult("once"))
        val received = mutableListOf<TrapezeNavigationResult>()

        composeTestRule.setContent {
            WithBackStack(backStack) {
                NavigationResultEffect("edit_result") { received += it }
            }
        }

        composeTestRule.runOnIdle {
            received.size shouldBe 1
            received.first().shouldBeInstanceOf<TestResult>().value shouldBe "once"
        }
    }

    @Test
    fun givenNoResult_whenRememberNavigationResultIsCalled_thenItReturnsNull() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        var capturedResult: TrapezeNavigationResult? = TestResult("sentinel")

        composeTestRule.setContent {
            WithBackStack(backStack) {
                capturedResult = rememberNavigationResult("no_such_key")
            }
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeNull()
        }
    }

    @Test
    fun givenAResult_whenRememberNavigationResultRecomposes_thenTheValueIsLatched() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.setResult(backStack.currentEntry.id, "key", TestResult("once"))
        var lastResult: TrapezeNavigationResult? = null
        var recomposeTrigger by mutableStateOf(0)

        composeTestRule.setContent {
            WithBackStack(backStack) {
                @Suppress("UNUSED_EXPRESSION")
                recomposeTrigger
                lastResult = rememberNavigationResult("key")
            }
        }

        composeTestRule.runOnIdle {
            lastResult.shouldBeInstanceOf<TestResult>().value shouldBe "once"
            // Taken off the backstack exactly once.
            backStack.resultsSnapshot() shouldBe emptyMap()
        }

        composeTestRule.runOnIdle { recomposeTrigger++ }

        composeTestRule.runOnIdle {
            // The delivered value survives recomposition instead of flipping back to null.
            lastResult.shouldBeInstanceOf<TestResult>().value shouldBe "once"
        }
    }
}
