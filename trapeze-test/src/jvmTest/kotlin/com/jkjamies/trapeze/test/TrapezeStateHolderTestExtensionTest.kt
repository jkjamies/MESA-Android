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

package com.jkjamies.trapeze.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import com.jkjamies.trapeze.TrapezeStateHolder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TrapezeStateHolderTestExtensionTest : BehaviorSpec({

    Given("a StateHolder under the test extension") {

        When("test is called") {
            Then("the initial state is emitted via awaitItem") {
                SimpleCounterStateHolder(7).test {
                    awaitItem().count shouldBe 7
                }
            }
        }

        When("an event is sent through the eventSink") {
            Then("the updated state is emitted") {
                SimpleCounterStateHolder(0).test {
                    val initial = awaitItem()
                    initial.count shouldBe 0

                    initial.eventSink(SimpleEvent.Increment)
                    awaitItem().count shouldBe 1
                }
            }
        }

        When("multiple events are sent in sequence") {
            Then("each distinct state change is emitted") {
                SimpleCounterStateHolder(10).test {
                    val initial = awaitItem()
                    initial.count shouldBe 10

                    initial.eventSink(SimpleEvent.Increment)
                    val after1 = awaitItem()
                    after1.count shouldBe 11

                    after1.eventSink(SimpleEvent.Decrement)
                    awaitItem().count shouldBe 10
                }
            }
        }
    }


}) {
    companion object {
        data class SimpleScreen(val initialCount: Int) : TrapezeScreen

        data class SimpleState(
            val count: Int,
            val eventSink: (SimpleEvent) -> Unit,
        ) : TrapezeState

        sealed interface SimpleEvent : TrapezeEvent {
            data object Increment : SimpleEvent
            data object Decrement : SimpleEvent
        }

        class SimpleCounterStateHolder(private val initialCount: Int) :
            TrapezeStateHolder<SimpleScreen, SimpleState, SimpleEvent>() {

            @Composable
            override fun produceState(): SimpleState {
                var count by remember { mutableIntStateOf(initialCount) }

                return SimpleState(
                    count = count,
                    eventSink = { event ->
                        when (event) {
                            SimpleEvent.Increment -> count++
                            SimpleEvent.Decrement -> count--
                        }
                    },
                )
            }
        }
    }
}
