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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestScope

class StrataLaunchTest : BehaviorSpec({

    coroutineTestScope = true

    Given("an active coroutine scope") {
        When("strataLaunch is called") {
            Then("it runs the block successfully") {
                coroutineScope {
                    var ran = false
                    val job = strataLaunch {
                        ran = true
                    }
                    job.join()
                    ran shouldBe true
                }
            }
        }
    }

    Given("a cancelled coroutine scope") {
        When("strataLaunch is called") {
            Then("it returns a cancelled job without running the block or throwing") {
                val scope = TestScope()
                scope.cancel()
                var ran = false

                val job = scope.strataLaunch { ran = true }

                job.isCancelled shouldBe true
                ran shouldBe false
            }
        }
    }

    Given("a context carrying a Job") {
        When("strataLaunch is called") {
            Then("it rejects the context rather than detaching from the scope") {
                coroutineScope {
                    shouldThrow<IllegalArgumentException> {
                        strataLaunch(Job()) { }
                    }
                }
            }
        }

        When("strataLaunchWithResult is called") {
            Then("it rejects the context rather than detaching from the scope") {
                coroutineScope {
                    shouldThrow<IllegalArgumentException> {
                        strataLaunchWithResult(Job()) { 1 }
                    }
                }
            }
        }
    }

    Given("strataLaunchWithResult") {
        When("the block succeeds") {
            Then("it returns a Success result") {
                coroutineScope {
                    val deferred = strataLaunchWithResult { 42 }
                    val result = deferred.await()
                    result.shouldBeInstanceOf<StrataResult.Success<Int>>()
                    result.data shouldBe 42
                }
            }
        }

        When("the block throws a StrataException") {
            Then("it returns a Failure with the exception") {
                coroutineScope {
                    val error = object : StrataException("domain error") {}
                    val deferred = strataLaunchWithResult<Int> { throw error }
                    val result = deferred.await()
                    result.shouldBeInstanceOf<StrataResult.Failure>()
                    result.error shouldBe error
                }
            }
        }

        When("the block throws an unexpected exception") {
            Then("it returns a Failure with StrataExecutionException") {
                coroutineScope {
                    val deferred = strataLaunchWithResult<Int> {
                        throw RuntimeException("unexpected")
                    }
                    val result = deferred.await()
                    result.shouldBeInstanceOf<StrataResult.Failure>()
                    result.error.shouldBeInstanceOf<StrataExecutionException>()
                }
            }
        }

        When("the scope is cancelled") {
            Then("it returns a cancelled deferred without running the block or throwing") {
                val scope = TestScope()
                scope.cancel()
                var ran = false

                val deferred = scope.strataLaunchWithResult { ran = true; 1 }

                deferred.isCancelled shouldBe true
                ran shouldBe false
            }
        }
    }
})
