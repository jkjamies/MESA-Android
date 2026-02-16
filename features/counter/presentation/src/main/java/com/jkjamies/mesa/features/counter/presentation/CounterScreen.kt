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

package com.jkjamies.mesa.features.counter.presentation

import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.jkjamies.trapeze.TrapezeMessage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Parcelize
data class CounterScreen(
    val initialCount: Int = 0 // Keeping it simple, no user name for now based on requirements
) : TrapezeScreen, Parcelable

data class CounterState(
    val count: Int,
    val trapezeMessage: TrapezeMessage? = null,
    val eventSink: (CounterEvent) -> Unit
) : TrapezeState

sealed interface CounterEvent : TrapezeEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
    data object Divide : CounterEvent
    data object GoToSummary : CounterEvent
    data object GetHelp : CounterEvent
    data object ThrowError : CounterEvent
    data class ClearError(val id: Uuid) : CounterEvent
}