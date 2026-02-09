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

package com.jkjamies.trapeze

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

/**
 * A transient UI message that complies with UDF by flowing through state.
 *
 * Each message has a unique [id] used for targeted dismissal via [TrapezeMessageManager.clearMessage].
 *
 * @param message The human-readable message text.
 * @param id Unique identifier for this message instance.
 */
public data class TrapezeMessage(
    val message: String,
    val id: Uuid = Uuid.random()
)

/**
 * Creates a [TrapezeMessage] from a [Throwable], using its message or a formatted fallback.
 */
public fun TrapezeMessage(
    t: Throwable,
    id: Uuid = Uuid.random(),
): TrapezeMessage = TrapezeMessage(
    message = t.message ?: "Error occurred: $t",
    id = id,
)

/**
 * Manages a FIFO queue of [TrapezeMessage]s, exposing the head as a [Flow].
 *
 * Messages are emitted via [emitMessage] and dismissed via [clearMessage].
 * When the current message is cleared, the next queued message becomes current.
 */
public class TrapezeMessageManager {
    private val _message = MutableStateFlow(emptyList<TrapezeMessage>())

    /**
     * A flow emitting the current message to display, or null when the queue is empty.
     */
    public val message: Flow<TrapezeMessage?> = _message.map { it.firstOrNull() }.distinctUntilChanged()

    /**
     * Adds a [message] to the end of the queue.
     */
    public fun emitMessage(message: TrapezeMessage) {
        _message.update { it + message }
    }

    /**
     * Removes the message with the given [id] from the queue.
     */
    public fun clearMessage(id: Uuid) {
        _message.update { messages ->
            messages.filterNot { it.id == id }
        }
    }
}
