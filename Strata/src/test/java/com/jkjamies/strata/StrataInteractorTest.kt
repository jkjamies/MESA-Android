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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StrataInteractorTest : BehaviorSpec({

    Given("an interactor that succeeds") {
        val interactor = object : StrataInteractor<String, Int>() {
            override suspend fun doWork(params: String): Int = params.length
        }

        When("invoked with params") {
            Then("it returns Success with the result") {
                val result = interactor("hello")
                result.shouldBeInstanceOf<StrataResult.Success<Int>>()
                result.data shouldBe 5
            }
        }
    }

    Given("an interactor that throws a StrataException") {
        val error = object : StrataException("domain failure") {}
        val interactor = object : StrataInteractor<Unit, Nothing>() {
            override suspend fun doWork(params: Unit): Nothing = throw error
        }

        When("invoked") {
            Then("it returns Failure with the exception") {
                val result = interactor(Unit)
                result.shouldBeInstanceOf<StrataResult.Failure>()
                result.error shouldBe error
            }
        }
    }

    Given("an interactor that throws an unexpected exception") {
        val interactor = object : StrataInteractor<Unit, Nothing>() {
            override suspend fun doWork(params: Unit): Nothing =
                throw RuntimeException("crash")
        }

        When("invoked") {
            Then("it returns Failure wrapped in StrataExecutionException") {
                val result = interactor(Unit)
                result.shouldBeInstanceOf<StrataResult.Failure>()
                result.error.shouldBeInstanceOf<StrataExecutionException>()
                result.error.message shouldBe "crash"
            }
        }
    }

    Given("an interactor that exceeds the timeout") {
        val interactor = object : StrataInteractor<Unit, Nothing>() {
            override suspend fun doWork(params: Unit): Nothing {
                delay(1.seconds)
                throw AssertionError("unreachable")
            }
        }

        When("invoked with a shorter timeout") {
            Then("TimeoutCancellationException propagates") {
                // strataRunCatching rethrows CancellationException (including timeout)
                shouldThrow<TimeoutCancellationException> {
                    interactor(Unit, timeout = 1.milliseconds)
                }
            }
        }
    }

    Given("an interactor with inProgress tracking") {
        When("invoked as user-initiated") {
            Then("inProgress emits true during execution then false after") {
                val interactor = object : StrataInteractor<Unit, Unit>() {
                    override suspend fun doWork(params: Unit) {
                        delay(100.milliseconds)
                    }
                }

                interactor.inProgress.test {
                    awaitItem() shouldBe false // initial state
                    val job = launch { interactor(Unit, userInitiated = true) }
                    awaitItem() shouldBe true
                    job.join()
                    awaitItem() shouldBe false
                }
            }
        }
    }

    Given("an interactor whose call is cancelled") {
        When("the coroutine is cancelled during execution") {
            Then("the loader is cleaned up and inProgress returns to false") {
                val interactor = object : StrataInteractor<Unit, Unit>() {
                    override suspend fun doWork(params: Unit) {
                        delay(10.seconds)
                    }
                }

                interactor.inProgress.test {
                    awaitItem() shouldBe false // initial state
                    val job = launch { interactor(Unit, userInitiated = true) }
                    awaitItem() shouldBe true
                    job.cancelAndJoin()
                    awaitItem() shouldBe false
                }
            }
        }
    }

    Given("the Unit params extension") {
        val interactor = object : StrataInteractor<Unit, String>() {
            override suspend fun doWork(params: Unit): String = "done"
        }

        When("invoked without params") {
            Then("it delegates to invoke(Unit)") {
                val result = interactor()
                result.shouldBeInstanceOf<StrataResult.Success<String>>()
                result.data shouldBe "done"
            }
        }
    }
})
