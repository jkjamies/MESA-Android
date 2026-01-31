package com.jkjamies.trapeze.counter

import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import kotlinx.parcelize.Parcelize

@Parcelize
data class CounterScreen(val hint: String) : TrapezeScreen

sealed interface CounterIntent : TrapezeEvent {
    data class OnEmailChanged(val email: String) : CounterIntent
    object OnSubmit : CounterIntent
    object OnHelp : CounterIntent
}

data class CounterState(
    val email: String,
    val eventSink: (CounterIntent) -> Unit
) : TrapezeState