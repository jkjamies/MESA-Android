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

package com.jkjamies.trapeze.test

import com.jkjamies.trapeze.TrapezeEvent

/**
 * A test double that records events for assertion, usable as an `eventSink` lambda.
 *
 * Usage:
 * ```kotlin
 * val sink = TestEventSink<MyEvent>()
 * state.copy(eventSink = sink)
 *
 * sink(MyEvent.Click)
 * sink.events shouldBe listOf(MyEvent.Click)
 * ```
 */
public class TestEventSink<E : TrapezeEvent> : (E) -> Unit {
    private val _events = mutableListOf<E>()

    /** All recorded events in order. */
    public val events: List<E> get() = _events.toList()

    /** The most recently recorded event, or null if none. */
    public val last: E? get() = _events.lastOrNull()

    /** The number of recorded events. */
    public val size: Int get() = _events.size

    override fun invoke(event: E) {
        _events.add(event)
    }

    /** Clears all recorded events. */
    public fun clear() {
        _events.clear()
    }
}
