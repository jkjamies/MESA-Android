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

package com.jkjamies.trapeze.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeScreen

/**
 * A saveable backstack of [TrapezeScreen]s for navigation.
 *
 * Each pushed screen occupies a [TrapezeBackStackEntry] with its own identity, so navigating
 * to the same screen twice does not make the two visits share saved UI state or results.
 *
 * @param root The initial/root screen of the backstack.
 */
@Stable
public class TrapezeBackStack internal constructor(root: TrapezeScreen) {
    private var _entries by mutableStateOf(listOf(TrapezeBackStackEntry.create(root)))

    /**
     * Pending navigation results, keyed first by the id of the entry the result is addressed
     * to, then by the caller-supplied result key.
     *
     * Scoping by entry means two features can both use the key `"result"` without colliding,
     * and results belonging to an entry are discarded when that entry leaves the stack — so
     * an unconsumed result cannot outlive the screen that was meant to read it.
     */
    private var _results by mutableStateOf<Map<String, Map<String, TrapezeNavigationResult>>>(emptyMap())

    /** The root (start) screen of this backstack. */
    public val root: TrapezeScreen get() = _entries.first().screen

    /** The currently active screen. */
    public val current: TrapezeScreen get() = _entries.last().screen

    /** The entry for the currently active screen. */
    public val currentEntry: TrapezeBackStackEntry get() = _entries.last()

    /** The entries in this backstack, root first. */
    public val entries: List<TrapezeBackStackEntry> get() = _entries

    /** The number of screens in the backstack. */
    public val size: Int get() = _entries.size

    internal fun push(screen: TrapezeScreen) {
        _entries = _entries + TrapezeBackStackEntry.create(screen)
    }

    internal fun pop(): Boolean {
        if (_entries.size > 1) {
            setEntries(_entries.dropLast(1))
            return true
        }
        return false
    }

    internal fun setResult(entryId: String, key: String, result: TrapezeNavigationResult) {
        val forEntry = _results[entryId].orEmpty() + (key to result)
        _results = _results + (entryId to forEntry)
    }

    /**
     * Reads the pending result for [entryId] and [key] without removing it.
     *
     * Safe to call from composition — unlike [consumeResult] it performs no snapshot write.
     */
    internal fun peekResult(entryId: String, key: String): TrapezeNavigationResult? =
        _results[entryId]?.get(key)

    internal fun consumeResult(entryId: String, key: String): TrapezeNavigationResult? {
        val forEntry = _results[entryId] ?: return null
        val result = forEntry[key] ?: return null
        val remaining = forEntry - key
        _results = if (remaining.isEmpty()) _results - entryId else _results + (entryId to remaining)
        return result
    }

    internal fun popWithResult(key: String, result: TrapezeNavigationResult): Boolean {
        // Only publish the result if there is somewhere for it to go. Storing a result that no
        // screen can ever consume would leak it for the lifetime of the backstack.
        if (_entries.size <= 1) return false
        val target = _entries[_entries.size - 2]
        setResult(target.id, key, result)
        return pop()
    }

    internal fun popToRoot() {
        if (_entries.size > 1) {
            setEntries(listOf(_entries.first()))
        }
    }

    internal fun popTo(screen: TrapezeScreen): Boolean {
        val index = _entries.indexOfLast { it.screen == screen }
        if (index < 0) return false
        if (index < _entries.size - 1) {
            setEntries(_entries.take(index + 1))
        }
        return true
    }

    /**
     * Replaces the entry list, discarding results addressed to entries that are no longer on
     * the stack. Every removal path goes through here so results cannot accumulate.
     */
    private fun setEntries(entries: List<TrapezeBackStackEntry>) {
        val removed = _entries.mapTo(mutableSetOf()) { it.id } - entries.mapTo(mutableSetOf()) { it.id }
        _entries = entries
        if (removed.isNotEmpty() && _results.isNotEmpty()) {
            _results = _results - removed
        }
    }

    internal fun restore(entries: List<TrapezeBackStackEntry>) {
        _entries = entries
    }

    internal fun restoreResults(results: Map<String, Map<String, TrapezeNavigationResult>>) {
        _results = results
    }

    internal fun resultsSnapshot(): Map<String, Map<String, TrapezeNavigationResult>> = _results

    public companion object
}

/**
 * Creates and remembers a saveable [TrapezeBackStack] with the given [root] screen.
 *
 * On Android, the backstack is persisted across configuration changes and process death
 * via `rememberSaveable`. On other platforms, the backstack is held in-memory.
 */
@Composable
public expect fun rememberSaveableBackStack(root: TrapezeScreen): TrapezeBackStack
