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

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class StrataResultTest : BehaviorSpec({

    Given("a Success result") {
        val result: StrataResult<String> = StrataResult.Success("hello")

        When("getOrNull is called") {
            Then("it returns the value") {
                result.getOrNull() shouldBe "hello"
            }
        }

        When("onSuccess is called") {
            Then("it invokes the action with the value") {
                var captured: String? = null
                result.onSuccess { captured = it }
                captured shouldBe "hello"
            }

            Then("it returns the original result for chaining") {
                val returned = result.onSuccess { }
                returned shouldBe result
            }
        }

        When("onFailure is called") {
            Then("it does not invoke the action") {
                var called = false
                result.onFailure { called = true }
                called shouldBe false
            }
        }
    }

    Given("a Failure result") {
        val error = object : StrataException("test error") {}
        val result: StrataResult<String> = StrataResult.Failure(error)

        When("getOrNull is called") {
            Then("it returns null") {
                result.getOrNull() shouldBe null
            }
        }

        When("onFailure is called") {
            Then("it invokes the action with the error") {
                var captured: StrataException? = null
                result.onFailure { captured = it }
                captured shouldBe error
            }

            Then("it returns the original result for chaining") {
                val returned = result.onFailure { }
                returned shouldBe result
            }
        }

        When("onSuccess is called") {
            Then("it does not invoke the action") {
                var called = false
                result.onSuccess { called = true }
                called shouldBe false
            }
        }
    }

    Given("a result with chained callbacks") {
        val error = object : StrataException("chained") {}
        val result: StrataResult<Int> = StrataResult.Failure(error)

        When("onSuccess and onFailure are chained") {
            Then("only the matching callback is invoked") {
                var successCalled = false
                var failureCalled = false

                result
                    .onSuccess { successCalled = true }
                    .onFailure { failureCalled = true }

                successCalled shouldBe false
                failureCalled shouldBe true
            }
        }
    }

    Given("getOrDefault") {
        When("result is Success") {
            Then("it returns the value") {
                val result: StrataResult<String> = StrataResult.Success("hello")
                result.getOrDefault("fallback") shouldBe "hello"
            }
        }

        When("result is Failure") {
            Then("it returns the default") {
                val result: StrataResult<String> = StrataResult.Failure(
                    object : StrataException("err") {}
                )
                result.getOrDefault("fallback") shouldBe "fallback"
            }
        }
    }

    Given("getOrElse") {
        When("result is Success") {
            Then("it returns the value without calling transform") {
                val result: StrataResult<String> = StrataResult.Success("hello")
                result.getOrElse { "recovered" } shouldBe "hello"
            }
        }

        When("result is Failure") {
            Then("it returns the transformed error") {
                val error = object : StrataException("specific") {}
                val result: StrataResult<String> = StrataResult.Failure(error)
                result.getOrElse { it.message ?: "unknown" } shouldBe "specific"
            }
        }
    }

    Given("map") {
        When("result is Success") {
            Then("it transforms the value") {
                val result: StrataResult<Int> = StrataResult.Success(5)
                val mapped = result.map { it * 2 }
                mapped.getOrNull() shouldBe 10
            }
        }

        When("result is Failure") {
            Then("it returns the original failure") {
                val error = object : StrataException("err") {}
                val result: StrataResult<Int> = StrataResult.Failure(error)
                val mapped = result.map { it * 2 }
                mapped.getOrNull() shouldBe null
                (mapped as StrataResult.Failure).error shouldBe error
            }
        }
    }

    Given("fold") {
        When("result is Success") {
            Then("it applies onSuccess") {
                val result: StrataResult<Int> = StrataResult.Success(5)
                val folded = result.fold(
                    onSuccess = { "value=$it" },
                    onFailure = { "error=${it.message}" }
                )
                folded shouldBe "value=5"
            }
        }

        When("result is Failure") {
            Then("it applies onFailure") {
                val error = object : StrataException("boom") {}
                val result: StrataResult<Int> = StrataResult.Failure(error)
                val folded = result.fold(
                    onSuccess = { "value=$it" },
                    onFailure = { "error=${it.message}" }
                )
                folded shouldBe "error=boom"
            }
        }
    }
})
