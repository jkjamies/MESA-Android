package com.jkjamies.trapeze.counter.feature.summary

import android.util.Log
import androidx.compose.runtime.Composable
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.navigation.TrapezeNavigator

class SummaryStateHolder(
    private val navigator: TrapezeNavigator
) : TrapezeStateHolder<SummaryScreen, SummaryState, SummaryEvent>() {

    @Composable
    override fun produceState(screen: SummaryScreen): SummaryState {
        return SummaryState(
            finalCount = screen.finalCount,
            eventSink = { event ->
                when (event) {
                    SummaryEvent.Back -> navigator.pop()
                    SummaryEvent.PrintValue -> Log.d("Summary", "Final Count: ${screen.finalCount}")
                }
            }
        )
    }
}
