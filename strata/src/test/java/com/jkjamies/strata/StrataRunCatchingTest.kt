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
import kotlin.coroutines.cancellation.CancellationException

class StrataRunCatchingTest : BehaviorSpec({

    Given("a block that succeeds") {
        When("strataRunCatching is called") {
            val result = strataRunCatching { "value" }

            Then("it returns Success with the value") {
                result.shouldBeInstanceOf<StrataResult.Success<String>>()
                result.data shouldBe "value"
            }
        }
    }

    Given("a block that throws a StrataException") {
        val exception = object : StrataException("domain error") {}

        When("strataRunCatching is called") {
            val result = strataRunCatching { throw exception }

            Then("it returns Failure with the original exception") {
                result.shouldBeInstanceOf<StrataResult.Failure>()
                result.error shouldBe exception
            }
        }
    }

    Given("a block that throws an unknown Throwable") {
        When("strataRunCatching is called") {
            val result = strataRunCatching { throw RuntimeException("unexpected") }

            Then("it returns Failure wrapped in StrataExecutionException") {
                result.shouldBeInstanceOf<StrataResult.Failure>()
                result.error.shouldBeInstanceOf<StrataExecutionException>()
                result.error.message shouldBe "unexpected"
            }
        }
    }

    Given("a block that throws CancellationException") {
        When("strataRunCatching is called") {
            Then("it rethrows the CancellationException") {
                shouldThrow<CancellationException> {
                    strataRunCatching { throw CancellationException("cancelled") }
                }
            }
        }
    }
})
