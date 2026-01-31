package com.jkjamies.trapeze.counter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CounterUi(modifier: Modifier, state: CounterState) {
    Column(modifier = modifier) {
        TextField(
            value = state.email,
            onValueChange = { state.eventSink(CounterIntent.OnEmailChanged(it)) }
        )
        Button(onClick = { state.eventSink(CounterIntent.OnSubmit) }) {
            Text("Submit")
        }
    }
}