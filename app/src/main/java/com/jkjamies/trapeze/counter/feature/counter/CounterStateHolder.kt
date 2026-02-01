package com.jkjamies.trapeze.counter.feature.counter

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.counter.common.AppInterop
import com.jkjamies.trapeze.counter.common.AppInteropEvent
import com.jkjamies.trapeze.counter.feature.summary.SummaryScreen
import com.jkjamies.trapeze.navigation.TrapezeNavigator

class CounterStateHolder(
    private val interop: AppInterop,
    private val navigator: TrapezeNavigator
) : TrapezeStateHolder<CounterScreen, CounterState, CounterEvent>() {

    @Composable
    override fun produceState(screen: CounterScreen): CounterState {
        var count by rememberSaveable { mutableIntStateOf(screen.initialCount) }

        return CounterState(
            count = count,
            eventSink = { event ->
                when (event) {
                    CounterEvent.Increment -> count++
                    CounterEvent.Decrement -> count--
                    CounterEvent.Divide -> count /= 2
                    CounterEvent.GoToSummary -> {
                        navigator.navigate(SummaryScreen(count))
                    }
                    CounterEvent.GetHelp -> {
                        interop.send(object : AppInteropEvent {
                            override fun toString(): String = "Help Requested!"
                        })
                    }
                }
            }
        )
    }
}