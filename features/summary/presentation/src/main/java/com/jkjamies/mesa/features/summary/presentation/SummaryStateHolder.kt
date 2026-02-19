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

package com.jkjamies.mesa.features.summary.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.jkjamies.trapeze.TrapezeMessage
import com.jkjamies.trapeze.TrapezeMessageManager
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.mesa.features.summary.api.ObserveLastSavedValue
import com.jkjamies.mesa.features.summary.api.SaveSummaryValue
import com.jkjamies.strata.getOrDefault
import com.jkjamies.strata.strataLaunch
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * StateHolder for the Summary screen.
 *
 * Manages saving and observing summary values via [SaveSummaryValue] and [ObserveLastSavedValue].
 */
@AssistedInject
class SummaryStateHolder constructor(
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
        val messageManager = remember { TrapezeMessageManager() }
        val trapezeMessage by messageManager.message.collectAsState(initial = null)

        val eventSink = wrapEventSink<SummaryEvent> { event ->
            when (event) {
                SummaryEvent.Back -> navigator.pop()
                SummaryEvent.PrintValue -> {
                    println("Value: ${screen.finalCount}")
                }
                SummaryEvent.SaveValue -> {
                    strataLaunch {
                        val result = saveSummaryValue.value.invoke(screen.finalCount)
                        // Demonstrate map: transform Success<Unit> into Success<Int> carrying the saved count
                        val savedCount = result.map { screen.finalCount }.getOrDefault(0)
                        // Demonstrate fold: produce a user-facing message for both outcomes
                        val message = result.fold(
                            onSuccess = { "Saved $savedCount successfully!" },
                            onFailure = { error -> "Save failed: ${error.message ?: "Unknown error"}" }
                        )
                        messageManager.emitMessage(TrapezeMessage(message))
                    }
                }
                is SummaryEvent.ClearMessage -> {
                    messageManager.clearMessage(event.id)
                }
            }
        }

        return SummaryState(
            finalCount = screen.finalCount,
            lastSavedValue = lastSavedValue,
            saveInProgress = saveSummaryLoading,
            trapezeMessage = trapezeMessage,
            eventSink = eventSink
        )
    }
}
