package com.jkjamies.trapeze.counter.feature.counter

import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class CounterScreen(
    val initialCount: Int = 0 // Keeping it simple, no user name for now based on requirements
) : TrapezeScreen, Parcelable

data class CounterState(
    val count: Int,
    val eventSink: (CounterEvent) -> Unit
) : TrapezeState

sealed interface CounterEvent : TrapezeEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
    data object Divide : CounterEvent
    data object GoToSummary : CounterEvent
    data object GetHelp : CounterEvent
}