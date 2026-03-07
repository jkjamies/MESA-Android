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

package com.jkjamies.mesa.features.summary.presentation

import com.jkjamies.mesa.features.summary.presentation.fakes.FakeObserveLastSavedValue
import com.jkjamies.mesa.features.summary.presentation.fakes.FakeSaveSummaryValue
import com.jkjamies.trapeze.test.FakeTrapezeNavigator
import com.jkjamies.trapeze.test.NavigationEvent
import com.jkjamies.trapeze.test.test
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SummaryStateHolderTest : BehaviorSpec({

    Given("a SummaryStateHolder") {
        When("produceState is called") {
            Then("initial state shows the final count") {
                val holder = SummaryStateHolder(42, FakeTrapezeNavigator(), lazy { FakeSaveSummaryValue() }, lazy { FakeObserveLastSavedValue() })
                holder.test {
                    awaitItem().finalCount shouldBe 42
                }
            }
        }
    }

    Given("a SummaryStateHolder for navigation") {
        When("Back event is sent") {
            Then("navigator pop is called") {
                val navigator = FakeTrapezeNavigator()
                val holder = SummaryStateHolder(10, navigator, lazy { FakeSaveSummaryValue() }, lazy { FakeObserveLastSavedValue() })
                holder.test {
                    val state = awaitItem()
                    state.eventSink(SummaryEvent.Back)

                    navigator.awaitPop()
                    navigator.events.last() shouldBe NavigationEvent.Pop
                }
            }
        }
    }

    Given("a SummaryStateHolder with a successful save") {
        When("SaveValue event is sent") {
            Then("success message is emitted") {
                val holder = SummaryStateHolder(5, FakeTrapezeNavigator(), lazy { FakeSaveSummaryValue(shouldFail = false) }, lazy { FakeObserveLastSavedValue() })
                holder.test {
                    val state = awaitItem()
                    state.eventSink(SummaryEvent.SaveValue)

                    var updated = awaitItem()
                    while (updated.trapezeMessage == null) {
                        updated = awaitItem()
                    }
                    updated.trapezeMessage?.message shouldContain "successfully"
                }
            }
        }
    }

    Given("a SummaryStateHolder with a failing save") {
        When("SaveValue event is sent") {
            Then("failure message is emitted") {
                val holder = SummaryStateHolder(5, FakeTrapezeNavigator(), lazy { FakeSaveSummaryValue(shouldFail = true) }, lazy { FakeObserveLastSavedValue() })
                holder.test {
                    val state = awaitItem()
                    state.eventSink(SummaryEvent.SaveValue)

                    var updated = awaitItem()
                    while (updated.trapezeMessage == null) {
                        updated = awaitItem()
                    }
                    updated.trapezeMessage?.message shouldContain "failed"
                }
            }
        }
    }
})
