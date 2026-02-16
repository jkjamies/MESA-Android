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
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.junit4.createComposeRule
import com.jkjamies.trapeze.TrapezeScreen
import io.kotest.matchers.shouldBe
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

@Parcelize
private data class TestScreen(val id: Int) : TrapezeScreen, Parcelable

class TrapezeBackStackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenANewBackStack_whenCreated_thenRootIsTheInitialScreen() {
        val backStack = TrapezeBackStack(TestScreen(1))

        backStack.root shouldBe TestScreen(1)
        backStack.current shouldBe TestScreen(1)
        backStack.size shouldBe 1
    }

    @Test
    fun givenABackStack_whenAScreenIsPushed_thenCurrentChangesToTheNewScreen() {
        val backStack = TrapezeBackStack(TestScreen(1))

        backStack.push(TestScreen(2))

        backStack.current shouldBe TestScreen(2)
        backStack.size shouldBe 2
        backStack.root shouldBe TestScreen(1)
    }

    @Test
    fun givenABackStackWithMultipleScreens_whenPopped_thenCurrentReturnsToPrevious() {
        val backStack = TrapezeBackStack(TestScreen(1))
        backStack.push(TestScreen(2))
        backStack.push(TestScreen(3))

        val result = backStack.pop()

        result shouldBe true
        backStack.current shouldBe TestScreen(2)
        backStack.size shouldBe 2
    }

    @Test
    fun givenABackStackAtRoot_whenPopped_thenItReturnsFalseAndStaysAtRoot() {
        val backStack = TrapezeBackStack(TestScreen(1))

        val result = backStack.pop()

        result shouldBe false
        backStack.current shouldBe TestScreen(1)
        backStack.size shouldBe 1
    }

    @Test
    fun givenABackStack_whenSavedAndRestored_thenStateIsPreserved() {
        val original = TrapezeBackStack(TestScreen(1))
        original.push(TestScreen(2))
        original.push(TestScreen(3))

        @Suppress("UNCHECKED_CAST")
        val saver = TrapezeBackStack.saver() as androidx.compose.runtime.saveable.Saver<TrapezeBackStack, Any>
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(original) }

        val restored = saved?.let { saver.restore(it) }

        restored!!.size shouldBe 3
        restored.root shouldBe TestScreen(1)
        restored.current shouldBe TestScreen(3)
    }
}
