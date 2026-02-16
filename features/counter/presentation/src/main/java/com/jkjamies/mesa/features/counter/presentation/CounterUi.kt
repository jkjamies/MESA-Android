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

package com.jkjamies.mesa.features.counter.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Count: ${state.count}",
                style = MaterialTheme.typography.displayLarge
            )

            state.trapezeMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
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

            Spacer(modifier = Modifier.height(16.dp))

            val trapezeMessage = state.trapezeMessage
            Button(
                onClick = {
                    if (trapezeMessage != null) {
                        state.eventSink(CounterEvent.ClearError(trapezeMessage.id))
                    } else {
                        state.eventSink(CounterEvent.ThrowError)
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (trapezeMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (trapezeMessage != null) "Clear Error" else "Throw Error")
            }
        }
    }
}