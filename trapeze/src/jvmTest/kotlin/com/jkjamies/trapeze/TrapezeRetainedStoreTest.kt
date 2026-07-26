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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TrapezeRetainedStoreTest : BehaviorSpec({

    Given("a retained store") {
        When("getOrPut is called twice for the same key") {
            Then("the factory runs once and the same value is returned") {
                val store = TrapezeRetainedStore()
                var factoryCalls = 0

                val first = store.getOrPut("key") { factoryCalls++; "value" }
                val second = store.getOrPut("key") { factoryCalls++; "other" }

                factoryCalls shouldBe 1
                first shouldBe "value"
                second shouldBe "value"
            }
        }

        When("a null value is stored") {
            Then("it is retained rather than re-created on each access") {
                val store = TrapezeRetainedStore()
                var factoryCalls = 0

                store.getOrPut<String?>("key") { factoryCalls++; null }
                store.getOrPut<String?>("key") { factoryCalls++; null }

                factoryCalls shouldBe 1
            }
        }

        When("a key is forgotten") {
            Then("the next access re-creates it") {
                val store = TrapezeRetainedStore()
                var factoryCalls = 0

                store.getOrPut("key") { factoryCalls++ }
                store.forget("key")
                store.getOrPut("key") { factoryCalls++ }

                factoryCalls shouldBe 2
            }
        }

        When("the coroutine scope is requested repeatedly") {
            Then("it is the same live scope") {
                val store = TrapezeRetainedStore()

                val scope = store.coroutineScope

                store.coroutineScope shouldBe scope
                scope.isActive shouldBe true
            }
        }

        When("one child of the scope fails") {
            Then("the supervisor keeps unrelated work alive") {
                val store = TrapezeRetainedStore()
                val started = CompletableDeferred<Unit>()

                store.coroutineScope.launch { throw RuntimeException("boom") }
                store.coroutineScope.launch { started.complete(Unit) }
                started.await()

                store.coroutineScope.isActive shouldBe true
            }
        }
    }

    Given("a cleared retained store") {
        When("clear is called") {
            Then("the scope is cancelled and values are dropped") {
                val store = TrapezeRetainedStore()
                val scope = store.coroutineScope
                store.getOrPut("key") { "value" }

                store.clear()

                scope.isActive shouldBe false
            }
        }

        When("clear is called twice") {
            Then("it is a no-op the second time") {
                val store = TrapezeRetainedStore()
                store.coroutineScope
                store.clear()
                store.clear()
            }
        }

        When("it is used afterwards") {
            Then("it fails loudly rather than handing back a dead scope") {
                val store = TrapezeRetainedStore()
                store.clear()

                shouldThrow<IllegalStateException> { store.coroutineScope }
                shouldThrow<IllegalStateException> { store.getOrPut("key") { "value" } }
            }
        }
    }
})
