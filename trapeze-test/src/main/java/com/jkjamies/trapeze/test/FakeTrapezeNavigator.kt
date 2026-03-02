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

import app.cash.turbine.Turbine
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen

/**
 * A fake [TrapezeNavigator] for use in tests.
 *
 * All navigator actions are recorded in [events] for synchronous assertion and also
 * emitted to an internal [Turbine] for async `await*` assertions.
 */
public class FakeTrapezeNavigator : TrapezeNavigator {
    private val _events = mutableListOf<NavigationEvent>()
    private val turbine = Turbine<NavigationEvent>()

    /** All recorded navigation events in order. */
    public val events: List<NavigationEvent> get() = _events.toList()

    /** All screens passed to [navigate], in order. */
    public val navigatedScreens: List<TrapezeScreen>
        get() = _events.filterIsInstance<NavigationEvent.Navigate>().map { it.screen }

    /** Number of times [pop] or [popWithResult] was called. */
    public val popCount: Int
        get() = _events.count { it is NavigationEvent.Pop || it is NavigationEvent.PopWithResult }

    /** Map of all results delivered via [popWithResult], keyed by result key. */
    public val results: Map<String, TrapezeNavigationResult>
        get() = _events.filterIsInstance<NavigationEvent.PopWithResult>()
            .associate { it.key to it.result }

    override fun navigate(screen: TrapezeScreen) {
        val event = NavigationEvent.Navigate(screen)
        _events.add(event)
        turbine.add(event)
    }

    override fun pop() {
        val event = NavigationEvent.Pop
        _events.add(event)
        turbine.add(event)
    }

    override fun <R : TrapezeNavigationResult> popWithResult(key: String, result: R) {
        val event = NavigationEvent.PopWithResult(key, result)
        _events.add(event)
        turbine.add(event)
    }

    /** Awaits the next [NavigationEvent] of any type. */
    public suspend fun awaitEvent(): NavigationEvent = turbine.awaitItem()

    /** Awaits the next [NavigationEvent.Navigate] and returns its screen. */
    public suspend fun awaitNavigate(): TrapezeScreen =
        (turbine.awaitItem() as NavigationEvent.Navigate).screen

    /** Awaits the next [NavigationEvent.Pop]. */
    public suspend fun awaitPop() {
        turbine.awaitItem() as NavigationEvent.Pop
    }

    /** Awaits the next [NavigationEvent.PopWithResult] and returns the key-result pair. */
    public suspend fun awaitPopWithResult(): Pair<String, TrapezeNavigationResult> {
        val event = turbine.awaitItem() as NavigationEvent.PopWithResult
        return event.key to event.result
    }

    /** Asserts that no events have been emitted to the turbine. */
    public fun expectNoEvents() {
        turbine.expectNoEvents()
    }
}
