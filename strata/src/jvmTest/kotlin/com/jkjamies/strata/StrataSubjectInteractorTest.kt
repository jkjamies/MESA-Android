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

package com.jkjamies.strata

import app.cash.turbine.test
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class StrataSubjectInteractorTest : BehaviorSpec({

    coroutineTestScope = true

    Given("a subject interactor that returns a static flow") {
        val interactor = object : StrataSubjectInteractor<String, Int>() {
            override fun createObservable(params: String): Flow<Int> = flowOf(params.length)
        }

        When("invoked with params and flow is collected") {
            Then("it emits the value from createObservable") {
                interactor.flow.test {
                    interactor("hello")
                    awaitItem() shouldBe 5
                }
            }
        }
    }

    Given("a subject interactor with changing params") {
        val interactor = object : StrataSubjectInteractor<String, Int>() {
            override fun createObservable(params: String): Flow<Int> = flowOf(params.length)
        }

        When("invoked with different params") {
            Then("it switches to the new observable via flatMapLatest") {
                interactor.flow.test {
                    interactor("hi")
                    awaitItem() shouldBe 2

                    interactor("hello")
                    awaitItem() shouldBe 5
                }
            }
        }
    }

    Given("a subject interactor invoked with duplicate params") {
        var createCount = 0
        val interactor = object : StrataSubjectInteractor<String, Int>() {
            override fun createObservable(params: String): Flow<Int> {
                createCount++
                return flowOf(params.length)
            }
        }

        When("invoked with the same params twice") {
            Then("equal params are conflated and do not re-subscribe") {
                interactor.flow.test {
                    interactor("hi")
                    awaitItem() shouldBe 2

                    interactor("hi") // duplicate
                    expectNoEvents()
                }
                createCount shouldBe 1
            }
        }
    }

    Given("a subject interactor backed by a mutable flow") {
        val backingFlow = MutableStateFlow(0)
        val interactor = object : StrataSubjectInteractor<Unit, Int>() {
            override fun createObservable(params: Unit): Flow<Int> = backingFlow
        }

        When("the backing flow emits new values") {
            Then("the interactor flow reflects them") {
                interactor.flow.test {
                    interactor(Unit)
                    awaitItem() shouldBe 0

                    backingFlow.value = 42
                    awaitItem() shouldBe 42
                }
            }
        }
    }

    Given("a subject interactor that has not been invoked") {
        val interactor = object : StrataSubjectInteractor<String, Int>() {
            override fun createObservable(params: String): Flow<Int> = flowOf(params.length)
        }

        When("flow is collected") {
            Then("it stays idle and emits nothing") {
                interactor.isActive shouldBe false
                interactor.flow.test { expectNoEvents() }
            }
        }
    }

    Given("an active subject interactor") {
        val backingFlow = MutableStateFlow(0)
        val interactor = object : StrataSubjectInteractor<Unit, Int>() {
            override fun createObservable(params: Unit): Flow<Int> = backingFlow
        }

        When("stop is called") {
            Then("it unsubscribes and stops emitting") {
                interactor.flow.test {
                    interactor(Unit)
                    awaitItem() shouldBe 0
                    interactor.isActive shouldBe true

                    interactor.stop()
                    interactor.isActive shouldBe false

                    backingFlow.value = 99
                    expectNoEvents()
                }
            }
        }

        When("invoked again with the same params after stop") {
            Then("it re-subscribes") {
                backingFlow.value = 99
                interactor.flow.test {
                    interactor(Unit)
                    awaitItem() shouldBe 99

                    interactor.stop()
                    interactor(Unit)
                    awaitItem() shouldBe 99
                }
            }
        }
    }

    // Both of these use a SharedFlow rather than a StateFlow: a StateFlow conflates equal
    // assignments, so `value = 2` twice never produces a consecutive duplicate and the tests
    // would pass whether or not `distinctValues` did anything. `replay = 1` because the first
    // emit can land before flatMapLatest has subscribed, and a replay-0 SharedFlow drops
    // emissions made while it has no subscribers.
    Given("a subject interactor emitting consecutive duplicate values") {
        val backingFlow = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 8)
        val emitting = object : StrataSubjectInteractor<Unit, Int>() {
            override fun createObservable(params: Unit): Flow<Int> = backingFlow
        }

        When("distinctValues is left at its default") {
            Then("repeat emissions are delivered rather than silently dropped") {
                emitting.flow.test {
                    emitting(Unit)

                    backingFlow.emit(2)
                    awaitItem() shouldBe 2
                    backingFlow.emit(2)
                    awaitItem() shouldBe 2
                }
            }
        }
    }

    Given("a subject interactor that opts into value de-duplication") {
        val backingFlow = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 8)
        val deduping = object : StrataSubjectInteractor<Unit, Int>() {
            override val distinctValues: Boolean = true
            override fun createObservable(params: Unit): Flow<Int> = backingFlow
        }

        When("the source emits a repeated value") {
            Then("the override is honoured and consecutive duplicates are filtered") {
                deduping.flow.test {
                    deduping(Unit)

                    backingFlow.emit(2)
                    awaitItem() shouldBe 2
                    backingFlow.emit(2)
                    expectNoEvents()
                }
            }
        }
    }

    Given("a subject interactor receiving rapid invocations") {
        val interactor = object : StrataSubjectInteractor<Int, String>() {
            override fun createObservable(params: Int): Flow<String> = flowOf("value-$params")
        }

        When("invoked rapidly with different params") {
            Then("it settles on the last value via DROP_OLDEST") {
                interactor.flow.test {
                    // Rapid-fire different params — DROP_OLDEST ensures the latest wins
                    for (i in 1..100) {
                        interactor(i)
                    }
                    // Due to flatMapLatest, intermediate observables are cancelled.
                    // The most recent emission must correspond to the final param.
                    expectMostRecentItem() shouldBe "value-100"
                }
            }
        }
    }
})
