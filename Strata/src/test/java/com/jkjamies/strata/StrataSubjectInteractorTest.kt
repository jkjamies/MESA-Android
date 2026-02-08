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
            Then("distinctUntilChanged prevents re-subscription") {
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
})
