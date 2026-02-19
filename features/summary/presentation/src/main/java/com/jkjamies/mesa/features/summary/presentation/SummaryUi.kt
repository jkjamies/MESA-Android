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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
fun SummaryUi(modifier: Modifier = Modifier, state: SummaryState) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Final Count: ${state.finalCount}",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Last Saved: ${state.lastSavedValue ?: "None"}",
                style = MaterialTheme.typography.bodyLarge
            )
            state.trapezeMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.message.startsWith("Save failed"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                Button(onClick = { state.eventSink(SummaryEvent.ClearMessage(message.id)) }) {
                    Text("Dismiss")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { state.eventSink(SummaryEvent.Back) }) {
                Text("Back")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { state.eventSink(SummaryEvent.SaveValue) }) {
                Text("Save Value")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { state.eventSink(SummaryEvent.PrintValue) }) {
                Text("Print Value")
            }
        }
    }
}
