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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaverScope
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

    // --- TrapezeBackStack result tests ---

    @Test
    fun givenABackStack_whenPopWithResultIsCalled_thenResultIsStoredAndScreenIsPopped() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))

        val popped = backStack.popWithResult("key", TestResult("hello"))

        popped shouldBe true
        backStack.current shouldBe ResultScreen(1)
        backStack.consumeResult("key").shouldBeInstanceOf<TestResult>().value shouldBe "hello"
    }

    @Test
    fun givenAResultIsSet_whenConsumeResultIsCalled_thenItReturnsAndRemovesTheResult() {
        val backStack = TrapezeBackStack(ResultScreen(1))

        backStack.setResult("key", TestResult("data"))

        backStack.consumeResult("key").shouldBeInstanceOf<TestResult>().value shouldBe "data"
    }

    @Test
    fun givenAResultWasConsumed_whenConsumeResultIsCalledAgain_thenItReturnsNull() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.setResult("key", TestResult("data"))

        backStack.consumeResult("key") // first consumption

        backStack.consumeResult("key").shouldBeNull()
    }

    @Test
    fun givenABackStackWithResults_whenSavedAndRestored_thenResultsSurvive() {
        val original = TrapezeBackStack(ResultScreen(1))
        original.push(ResultScreen(2))
        original.setResult("key", TestResult("persisted"))

        @Suppress("UNCHECKED_CAST")
        val saver = TrapezeBackStack.saver() as androidx.compose.runtime.saveable.Saver<TrapezeBackStack, Any>
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(original) }
        val restored = saved?.let { saver.restore(it) }

        restored!!.size shouldBe 2
        restored.consumeResult("key").shouldBeInstanceOf<TestResult>().value shouldBe "persisted"
    }

    // --- Navigator popWithResult tests ---

    @Test
    fun givenANavigator_whenPopWithResultIsCalled_thenItDelegatesToBackStack() {
        val backStack = TrapezeBackStack(ResultScreen(1))
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
            backStack.consumeResult("key").shouldBeInstanceOf<TestResult>().value shouldBe "nav_result"
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
            // Result is still set even when at root
            backStack.consumeResult("key").shouldBeInstanceOf<TestResult>().value shouldBe "root"
        }
    }

    // --- rememberNavigationResult tests ---

    @Test
    fun givenAResult_whenRememberNavigationResultIsCalled_thenItReturnsTheResult() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.push(ResultScreen(2))
        var capturedResult: TrapezeNavigationResult? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTrapezeBackStack provides backStack) {
                // Simulate popWithResult having been called
                backStack.popWithResult("edit_result", TestResult("composed"))
                capturedResult = rememberNavigationResult("edit_result")
            }
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeInstanceOf<TestResult>().value shouldBe "composed"
        }
    }

    @Test
    fun givenNoResult_whenRememberNavigationResultIsCalled_thenItReturnsNull() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        var capturedResult: TrapezeNavigationResult? = TestResult("sentinel")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTrapezeBackStack provides backStack) {
                capturedResult = rememberNavigationResult("no_such_key")
            }
        }

        composeTestRule.runOnIdle {
            capturedResult.shouldBeNull()
        }
    }

    @Test
    fun givenAResult_whenRememberNavigationResultIsCalledTwice_thenItConsumesOnFirstRead() {
        val backStack = TrapezeBackStack(ResultScreen(1))
        backStack.setResult("key", TestResult("once"))
        var lastResult: TrapezeNavigationResult? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTrapezeBackStack provides backStack) {
                lastResult = rememberNavigationResult("key")
            }
        }

        composeTestRule.runOnIdle {
            lastResult.shouldBeInstanceOf<TestResult>().value shouldBe "once"
            // The backstack result should be consumed
            backStack.consumeResult("key").shouldBeNull()
        }
    }
}
