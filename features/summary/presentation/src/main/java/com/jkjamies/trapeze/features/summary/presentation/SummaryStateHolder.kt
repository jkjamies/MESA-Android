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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.features.summary.api.ObserveLastSavedValue
import com.jkjamies.trapeze.features.summary.api.SaveSummaryValue
import com.jkjamies.strata.strataLaunch
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * StateHolder for the Summary screen.
 *
 * Manages saving and observing summary values via [SaveSummaryValue] and [ObserveLastSavedValue].
 */
class SummaryStateHolder @AssistedInject constructor(
    @Assisted private val navigator: TrapezeNavigator,
    private val saveSummaryValue: Lazy<SaveSummaryValue>,
    private val observeLastSavedValue: Lazy<ObserveLastSavedValue>
) : TrapezeStateHolder<SummaryScreen, SummaryState, SummaryEvent>() {

    @AssistedFactory
    fun interface Factory {
        fun create(navigator: TrapezeNavigator): SummaryStateHolder
    }

    /**
     * Produces the [SummaryState] for the given [screen].
     */
    @Composable
    override fun produceState(screen: SummaryScreen): SummaryState {
        LaunchedEffect(Unit) {
            observeLastSavedValue.value.invoke(Unit)
        }

        val lastSavedValue by observeLastSavedValue.value.flow.collectAsState(initial = null)
        val saveSummaryLoading by saveSummaryValue.value.inProgress.collectAsState(initial = false)

        val eventSink = wrapEventSink<SummaryEvent> { event ->
            when (event) {
                SummaryEvent.Back -> navigator.pop()
                SummaryEvent.PrintValue -> {
                    println("Value: ${screen.finalCount}")
                }
                SummaryEvent.SaveValue -> {
                    strataLaunch {
                        saveSummaryValue.value.invoke(screen.finalCount).onFailure {
                            it.printStackTrace()
                        }
                    }
                }
            }
        }

        return SummaryState(
            finalCount = screen.finalCount,
            lastSavedValue = lastSavedValue,
            saveInProgress = saveSummaryLoading,
            eventSink = eventSink
        )
    }
}
