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

package com.jkjamies.trapeze

import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Rule
import org.junit.Test

class TrapezeCompositionLocalsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test(expected = IllegalStateException::class)
    fun givenNoTrapezeProvider_whenLocalTrapezeIsAccessed_thenItThrowsWithClearMessage() {
        composeTestRule.setContent {
            // Access LocalTrapeze.current without a TrapezeCompositionLocals provider
            @Suppress("UNUSED_VARIABLE")
            val trapeze = LocalTrapeze.current
        }
    }

    @Test
    fun givenATrapezeProvider_whenLocalTrapezeIsAccessed_thenItReturnsTheProvidedInstance() {
        val trapeze = Trapeze.Builder().build()
        var resolved: Trapeze? = null

        composeTestRule.setContent {
            TrapezeCompositionLocals(trapeze) {
                resolved = LocalTrapeze.current
            }
        }

        composeTestRule.waitForIdle()
        resolved.shouldBeInstanceOf<Trapeze>()
    }
}
