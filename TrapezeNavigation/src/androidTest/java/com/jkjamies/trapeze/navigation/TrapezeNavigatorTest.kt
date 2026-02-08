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
import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import io.kotest.matchers.shouldBe
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class NavScreen(val id: Int) : TrapezeScreen, Parcelable

class TrapezeNavigatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenANavigator_whenNavigateIsCalled_thenTheScreenIsPushedToTheBackStack() {
        val backStack = TrapezeBackStack(NavScreen(1))
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            navigator = rememberTrapezeNavigator(backStack)
        }

        composeTestRule.runOnIdle {
            navigator.navigate(NavScreen(2))
        }

        composeTestRule.runOnIdle {
            backStack.current shouldBe NavScreen(2)
            backStack.size shouldBe 2
        }
    }

    @Test
    fun givenANavigatorWithMultipleScreens_whenPopIsCalled_thenTheTopScreenIsRemoved() {
        val backStack = TrapezeBackStack(NavScreen(1))
        backStack.push(NavScreen(2))
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            navigator = rememberTrapezeNavigator(backStack)
        }

        composeTestRule.runOnIdle {
            navigator.pop()
        }

        composeTestRule.runOnIdle {
            backStack.current shouldBe NavScreen(1)
            backStack.size shouldBe 1
        }
    }

    @Test
    fun givenANavigatorAtRoot_whenPopIsCalled_thenOnRootPopIsInvoked() {
        val backStack = TrapezeBackStack(NavScreen(1))
        var rootPopCalled = false
        lateinit var navigator: TrapezeNavigator

        composeTestRule.setContent {
            navigator = rememberTrapezeNavigator(backStack) { rootPopCalled = true }
        }

        composeTestRule.runOnIdle {
            navigator.pop()
        }

        composeTestRule.runOnIdle {
            rootPopCalled shouldBe true
            backStack.size shouldBe 1
        }
    }
}
