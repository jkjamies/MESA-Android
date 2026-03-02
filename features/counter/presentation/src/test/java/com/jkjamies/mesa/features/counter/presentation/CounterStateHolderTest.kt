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

package com.jkjamies.mesa.features.counter.presentation

import com.jkjamies.mesa.features.counter.presentation.test.FakeAppInterop
import com.jkjamies.mesa.features.summary.presentation.SummaryScreen
import com.jkjamies.trapeze.test.FakeTrapezeNavigator
import com.jkjamies.trapeze.test.NavigationEvent
import com.jkjamies.trapeze.test.test
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CounterStateHolderTest : BehaviorSpec({

    Given("a CounterStateHolder with initial count") {
        When("produceState is called") {
            Then("initial count matches the screen") {
                val holder = CounterStateHolder(5, FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    awaitItem().count shouldBe 5
                }
            }
        }
    }

    Given("a CounterStateHolder at count 0") {
        When("Increment event is sent") {
            Then("count increases by 1") {
                val holder = CounterStateHolder(0, FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    val state = awaitItem()
                    state.count shouldBe 0

                    state.eventSink(CounterEvent.Increment)
                    awaitItem().count shouldBe 1
                }
            }
        }
    }

    Given("a CounterStateHolder at count 5") {
        When("Decrement event is sent") {
            Then("count decreases by 1") {
                val holder = CounterStateHolder(5, FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    val state = awaitItem()
                    state.count shouldBe 5

                    state.eventSink(CounterEvent.Decrement)
                    awaitItem().count shouldBe 4
                }
            }
        }
    }

    Given("a CounterStateHolder at count 10") {
        When("Divide event is sent") {
            Then("count is halved") {
                val holder = CounterStateHolder(10, FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    val state = awaitItem()
                    state.count shouldBe 10

                    state.eventSink(CounterEvent.Divide)
                    awaitItem().count shouldBe 5
                }
            }
        }
    }

    Given("a CounterStateHolder at count 42") {
        When("GoToSummary event is sent") {
            Then("navigator receives SummaryScreen with correct count") {
                val navigator = FakeTrapezeNavigator()
                val holder = CounterStateHolder(42, FakeAppInterop(), navigator)
                holder.test {
                    val state = awaitItem()
                    state.eventSink(CounterEvent.GoToSummary)

                    val screen = navigator.awaitNavigate()
                    screen.shouldBeInstanceOf<SummaryScreen>()
                    screen.finalCount shouldBe 42
                }
            }
        }
    }

    Given("a CounterStateHolder for interop") {
        When("GetHelp event is sent") {
            Then("interop receives the event") {
                val interop = FakeAppInterop()
                val holder = CounterStateHolder(0,interop, FakeTrapezeNavigator())
                holder.test {
                    val state = awaitItem()
                    state.eventSink(CounterEvent.GetHelp)

                    interop.events.size shouldBe 1
                }
            }
        }
    }

    Given("a CounterStateHolder for error handling") {
        When("ThrowError event is sent") {
            Then("trapezeMessage appears with the error message") {
                val holder = CounterStateHolder(0,FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    val initial = awaitItem()
                    initial.trapezeMessage.shouldBeNull()

                    initial.eventSink(CounterEvent.ThrowError)
                    val errorState = awaitItem()
                    errorState.trapezeMessage.shouldNotBeNull().message shouldBe "Simulated Failure"
                }
            }
        }

        When("ClearError event is sent after an error") {
            Then("trapezeMessage is cleared") {
                val holder = CounterStateHolder(0,FakeAppInterop(), FakeTrapezeNavigator())
                holder.test {
                    val initial = awaitItem()
                    initial.eventSink(CounterEvent.ThrowError)

                    val errorState = awaitItem()
                    val messageId = errorState.trapezeMessage!!.id
                    errorState.eventSink(CounterEvent.ClearError(messageId))

                    awaitItem().trapezeMessage.shouldBeNull()
                }
            }
        }
    }
})
