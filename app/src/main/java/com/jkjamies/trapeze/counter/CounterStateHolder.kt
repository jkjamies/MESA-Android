package com.jkjamies.trapeze.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jkjamies.trapeze.TrapezeStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import com.jkjamies.trapeze.TrapezeInterop
import com.jkjamies.trapeze.TrapezeInteropEvent
import kotlinx.coroutines.launch
import com.jkjamies.trapeze.navigation.TrapezeNavigator

class CounterStateHolder(
    private val interop: TrapezeInterop,
    private val navigator: TrapezeNavigator
) : TrapezeStateHolder<CounterScreen, CounterState, CounterIntent>() {

    @Composable
    override fun produceState(screen: CounterScreen): CounterState {
        var email by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        return CounterState(
            email = email,
            eventSink = { intent ->
                when (intent) {
                    is CounterIntent.OnEmailChanged -> email = intent.email
                    is CounterIntent.OnSubmit -> {
                        scope.launch {
                             // Background work
                        }
                    }
                    is CounterIntent.OnHelp -> {
                        interop.send(object : TrapezeInteropEvent {})
                    }
                }
            }
        )
    }
}