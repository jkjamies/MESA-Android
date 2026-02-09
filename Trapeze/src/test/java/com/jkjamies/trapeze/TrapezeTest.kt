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

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

// Manual Parcelable implementation because @Parcelize is not available in JVM unit tests —
// it requires the Android Gradle plugin's codegen which only runs for androidTest/main source sets.
private data class ScreenA(val id: Int = 0) : TrapezeScreen {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}

private data class ScreenB(val id: Int = 0) : TrapezeScreen {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}

private class FakeStateHolder : TrapezeStateHolder<TrapezeScreen, TrapezeState, TrapezeEvent>() {
    @Composable
    override fun produceState(screen: TrapezeScreen): TrapezeState {
        return object : TrapezeState {}
    }
}

private val FakeUi: TrapezeUi<TrapezeState> = @Composable { _: Modifier, _: TrapezeState -> }

class TrapezeTest : BehaviorSpec({

    Given("a Trapeze built with factories for ScreenA") {
        val stateHolder = FakeStateHolder()
        val trapeze = Trapeze.Builder()
            .addStateHolderFactory { screen, _ ->
                if (screen is ScreenA) stateHolder else null
            }
            .addUiFactory { screen ->
                if (screen is ScreenA) FakeUi else null
            }
            .build()

        When("stateHolder is resolved for ScreenA") {
            Then("it returns the registered state holder") {
                trapeze.stateHolder(ScreenA(), null).shouldNotBeNull()
            }
        }

        When("stateHolder is resolved for ScreenB") {
            Then("it returns null") {
                trapeze.stateHolder(ScreenB(), null).shouldBeNull()
            }
        }

        When("ui is resolved for ScreenA") {
            Then("it returns the registered UI") {
                trapeze.ui(ScreenA()).shouldNotBeNull()
            }
        }

        When("ui is resolved for ScreenB") {
            Then("it returns null") {
                trapeze.ui(ScreenB()).shouldBeNull()
            }
        }
    }

    Given("a Trapeze with multiple factories") {
        val firstHolder = FakeStateHolder()
        val secondHolder = FakeStateHolder()
        val trapeze = Trapeze.Builder()
            .addStateHolderFactory { screen, _ ->
                if (screen is ScreenA) firstHolder else null
            }
            .addStateHolderFactory { screen, _ ->
                if (screen is ScreenA) secondHolder else null
            }
            .build()

        When("both factories match the same screen") {
            Then("the first registered factory wins") {
                trapeze.stateHolder(ScreenA(), null) shouldBe firstHolder
            }
        }
    }

    Given("a Trapeze with no factories") {
        val trapeze = Trapeze.Builder().build()

        When("stateHolder is resolved") {
            Then("it returns null") {
                trapeze.stateHolder(ScreenA(), null).shouldBeNull()
            }
        }

        When("ui is resolved") {
            Then("it returns null") {
                trapeze.ui(ScreenA()).shouldBeNull()
            }
        }
    }
})
