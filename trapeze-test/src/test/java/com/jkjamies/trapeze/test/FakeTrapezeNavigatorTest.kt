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

import android.os.Parcel
import android.os.Parcelable
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeScreen
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class FakeTrapezeNavigatorTest : BehaviorSpec({

    Given("a FakeTrapezeNavigator") {
        val navigator = FakeTrapezeNavigator()

        When("navigate is called") {
            navigator.navigate(TestScreen("home"))

            Then("the screen is recorded in navigatedScreens") {
                navigator.navigatedScreens shouldBe listOf(TestScreen("home"))
            }

            Then("the events list contains a Navigate event") {
                navigator.events.size shouldBe 1
                navigator.events.first().shouldBeInstanceOf<NavigationEvent.Navigate>()
                (navigator.events.first() as NavigationEvent.Navigate).screen shouldBe TestScreen("home")
            }
        }

        When("pop is called") {
            navigator.pop()

            Then("popCount increases") {
                navigator.popCount shouldBe 1
            }

            Then("the events list contains a Pop event") {
                navigator.events.last().shouldBeInstanceOf<NavigationEvent.Pop>()
            }
        }

        When("popWithResult is called") {
            val result = TestResult("saved")
            navigator.popWithResult("edit_result", result)

            Then("the result is recorded in results map") {
                navigator.results["edit_result"] shouldBe result
            }

            Then("popCount increases") {
                navigator.popCount shouldBe 2
            }

            Then("the events list contains a PopWithResult event") {
                navigator.events.last().shouldBeInstanceOf<NavigationEvent.PopWithResult>()
            }
        }

        When("popToRoot is called") {
            navigator.popToRoot()

            Then("the events list contains a PopToRoot event") {
                navigator.events.last().shouldBeInstanceOf<NavigationEvent.PopToRoot>()
            }

            Then("popCount increases") {
                navigator.popCount shouldBe 3
            }
        }

        When("popTo is called") {
            val result = navigator.popTo(TestScreen("home"))

            Then("it returns true by default") {
                result shouldBe true
            }

            Then("the events list contains a PopTo event with the correct screen") {
                navigator.events.last().shouldBeInstanceOf<NavigationEvent.PopTo>()
                (navigator.events.last() as NavigationEvent.PopTo).screen shouldBe TestScreen("home")
            }

            Then("popCount increases") {
                navigator.popCount shouldBe 4
            }
        }

        When("events are recorded in sequence") {
            Then("events list preserves order") {
                navigator.events.size shouldBe 5
                navigator.events[0].shouldBeInstanceOf<NavigationEvent.Navigate>()
                navigator.events[1].shouldBeInstanceOf<NavigationEvent.Pop>()
                navigator.events[2].shouldBeInstanceOf<NavigationEvent.PopWithResult>()
                navigator.events[3].shouldBeInstanceOf<NavigationEvent.PopToRoot>()
                navigator.events[4].shouldBeInstanceOf<NavigationEvent.PopTo>()
            }
        }
    }

    Given("a fresh FakeTrapezeNavigator for async assertions") {
        val navigator = FakeTrapezeNavigator()

        When("navigate is called") {
            Then("awaitNavigate returns the screen") {
                navigator.navigate(TestScreen("async"))
                navigator.awaitNavigate() shouldBe TestScreen("async")
            }
        }

        When("pop is called") {
            Then("awaitPop completes without error") {
                navigator.pop()
                navigator.awaitPop()
            }
        }

        When("popWithResult is called") {
            Then("awaitPopWithResult returns the key-result pair") {
                val result = TestResult("data")
                navigator.popWithResult("key", result)
                navigator.awaitPopWithResult() shouldBe ("key" to result)
            }
        }

        When("popToRoot is called") {
            Then("awaitPopToRoot completes without error") {
                navigator.popToRoot()
                navigator.awaitPopToRoot()
            }
        }

        When("popTo is called") {
            Then("awaitPopTo returns the target screen") {
                navigator.popTo(TestScreen("target"))
                navigator.awaitPopTo() shouldBe TestScreen("target")
            }
        }

        When("awaitEvent is called") {
            Then("it returns the next event regardless of type") {
                navigator.navigate(TestScreen("any"))
                navigator.awaitEvent().shouldBeInstanceOf<NavigationEvent.Navigate>()
            }
        }
    }
    Given("a FakeTrapezeNavigator with popToReturns set to false") {
        val navigator = FakeTrapezeNavigator(popToReturns = false)

        When("popTo is called") {
            val result = navigator.popTo(TestScreen("missing"))

            Then("it returns false") {
                result shouldBe false
            }

            Then("the event is still recorded") {
                navigator.events.last().shouldBeInstanceOf<NavigationEvent.PopTo>()
                (navigator.events.last() as NavigationEvent.PopTo).screen shouldBe TestScreen("missing")
            }
        }
    }
}) {
    companion object {
        data class TestScreen(val name: String) : TrapezeScreen {
            override fun describeContents(): Int = 0
            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        data class TestResult(val value: String) : TrapezeNavigationResult {
            override fun describeContents(): Int = 0
            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }
    }
}
