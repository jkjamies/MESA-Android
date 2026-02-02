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

package com.jkjamies.trapeze.features.summary.presentation

import android.os.Parcelable
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SummaryScreen(val finalCount: Int) : TrapezeScreen, Parcelable

data class SummaryState(
    val finalCount: Int,
    val lastSavedValue: Int?,
    val saveInProgress: Boolean,
    val eventSink: (SummaryEvent) -> Unit
) : TrapezeState

sealed interface SummaryEvent : TrapezeEvent {
    // No events for now, just a display screen, but good practice to have the interface
    data object Back : SummaryEvent
    data object PrintValue : SummaryEvent
    data object SaveValue : SummaryEvent
}
