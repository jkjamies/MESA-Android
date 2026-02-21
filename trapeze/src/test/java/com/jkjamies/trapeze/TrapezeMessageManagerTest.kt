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

import app.cash.turbine.test
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TrapezeMessageManagerTest : BehaviorSpec({

    coroutineTestScope = true

    Given("a new TrapezeMessageManager") {
        When("no messages have been emitted") {
            Then("message flow emits null") {
                val manager = TrapezeMessageManager()
                manager.message.test {
                    awaitItem() shouldBe null
                }
            }
        }
    }

    Given("a manager with one emitted message") {
        When("a message is emitted") {
            Then("message flow emits that message") {
                val manager = TrapezeMessageManager()
                val msg = TrapezeMessage("error occurred")

                manager.message.test {
                    awaitItem() shouldBe null

                    manager.emitMessage(msg)
                    awaitItem() shouldBe msg
                }
            }
        }
    }

    Given("a manager with a message to clear") {
        When("clearMessage is called with the message ID") {
            Then("message flow returns to null") {
                val manager = TrapezeMessageManager()
                val msg = TrapezeMessage("error occurred")

                manager.message.test {
                    awaitItem() shouldBe null

                    manager.emitMessage(msg)
                    awaitItem() shouldBe msg

                    manager.clearMessage(msg.id)
                    awaitItem() shouldBe null
                }
            }
        }
    }

    Given("a manager with multiple queued messages") {
        When("the first message is cleared") {
            Then("the second message becomes current") {
                val manager = TrapezeMessageManager()
                val msg1 = TrapezeMessage("first")
                val msg2 = TrapezeMessage("second")

                manager.message.test {
                    awaitItem() shouldBe null

                    manager.emitMessage(msg1)
                    awaitItem() shouldBe msg1

                    manager.emitMessage(msg2)
                    // Still shows first (queue order)
                    expectNoEvents()

                    manager.clearMessage(msg1.id)
                    awaitItem() shouldBe msg2
                }
            }
        }
    }

    Given("a TrapezeMessage created from a Throwable") {
        When("the throwable has a message") {
            Then("the TrapezeMessage uses the throwable message") {
                val msg = TrapezeMessage(RuntimeException("something broke"))
                msg.message shouldBe "something broke"
            }
        }

        When("the throwable has no message") {
            Then("the TrapezeMessage uses a fallback") {
                val throwable = object : Throwable() {
                    override val message: String? = null
                    override fun toString(): String = "CustomError"
                }
                val msg = TrapezeMessage(throwable)
                msg.message shouldBe "Error occurred: CustomError"
            }
        }
    }
})
