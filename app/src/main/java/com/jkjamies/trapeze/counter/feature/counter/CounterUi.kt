package com.jkjamies.trapeze.counter.feature.counter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CounterUi(modifier: Modifier = Modifier, state: CounterState) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Count: ${state.count}",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { state.eventSink(CounterEvent.Decrement) }) {
                Text("-")
            }
            Button(onClick = { state.eventSink(CounterEvent.Divide) }) {
                Text("/ 2")
            }
            Button(onClick = { state.eventSink(CounterEvent.Increment) }) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { state.eventSink(CounterEvent.GoToSummary) }) {
            Text("Go to Summary")
        }

        Spacer(modifier = Modifier.height(16.dp))



        Button(onClick = { state.eventSink(CounterEvent.GetHelp) }) {
            Text("Get Help")
        }
    }
}