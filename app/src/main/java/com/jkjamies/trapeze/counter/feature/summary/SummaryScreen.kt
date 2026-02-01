package com.jkjamies.trapeze.counter.feature.summary

import android.os.Parcelable
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SummaryScreen(val finalCount: Int) : TrapezeScreen, Parcelable

data class SummaryState(
    val finalCount: Int,
    val eventSink: (SummaryEvent) -> Unit
) : TrapezeState

sealed interface SummaryEvent : TrapezeEvent {
    // No events for now, just a display screen, but good practice to have the interface
    data object Back : SummaryEvent
    data object PrintValue : SummaryEvent
}
