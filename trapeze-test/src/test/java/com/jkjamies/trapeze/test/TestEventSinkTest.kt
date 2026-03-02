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

import com.jkjamies.trapeze.TrapezeEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class TestEventSinkTest : BehaviorSpec({

    Given("a TestEventSink") {
        val sink = TestEventSink<TestEvent>()

        When("no events have been sent") {
            Then("events is empty") {
                sink.events shouldBe emptyList()
            }

            Then("last is null") {
                sink.last.shouldBeNull()
            }

            Then("size is 0") {
                sink.size shouldBe 0
            }
        }

        When("an event is invoked") {
            sink(TestEvent.Click)

            Then("events contains the event") {
                sink.events shouldBe listOf(TestEvent.Click)
            }

            Then("last returns the event") {
                sink.last shouldBe TestEvent.Click
            }

            Then("size is 1") {
                sink.size shouldBe 1
            }
        }

        When("multiple events are invoked") {
            sink(TestEvent.Submit("data"))

            Then("events preserves order") {
                sink.events shouldBe listOf(TestEvent.Click, TestEvent.Submit("data"))
            }

            Then("last returns the most recent event") {
                sink.last shouldBe TestEvent.Submit("data")
            }

            Then("size reflects the total count") {
                sink.size shouldBe 2
            }
        }

        When("clear is called") {
            sink.clear()

            Then("events is empty") {
                sink.events shouldBe emptyList()
            }

            Then("last is null") {
                sink.last.shouldBeNull()
            }

            Then("size is 0") {
                sink.size shouldBe 0
            }
        }
    }
}) {
    companion object {
        sealed interface TestEvent : TrapezeEvent {
            data object Click : TestEvent
            data class Submit(val value: String) : TestEvent
        }
    }
}
