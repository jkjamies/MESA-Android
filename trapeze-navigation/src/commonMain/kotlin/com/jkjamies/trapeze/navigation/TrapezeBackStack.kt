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
 * @param root The initial/root screen of the backstack.
 */
@Stable
public class TrapezeBackStack internal constructor(root: TrapezeScreen) {
    private var _stack by mutableStateOf(listOf(root))
    private var _results by mutableStateOf<Map<String, TrapezeNavigationResult>>(emptyMap())

    /** The root (start) screen of this backstack. */
    public val root: TrapezeScreen get() = _stack.first()

    /** The currently active screen. */
    public val current: TrapezeScreen get() = _stack.last()

    /** The number of screens in the backstack. */
    public val size: Int get() = _stack.size

    internal fun push(screen: TrapezeScreen) {
        _stack = _stack + screen
    }

    internal fun pop(): Boolean {
        if (_stack.size > 1) {
            _stack = _stack.dropLast(1)
            return true
        }
        return false
    }

    internal fun setResult(key: String, result: TrapezeNavigationResult) {
        _results = _results + (key to result)
    }

    internal fun consumeResult(key: String): TrapezeNavigationResult? {
        val result = _results[key]
        if (result != null) {
            _results = _results - key
        }
        return result
    }

    internal fun popWithResult(key: String, result: TrapezeNavigationResult): Boolean {
        setResult(key, result)
        return pop()
    }

    internal fun popToRoot() {
        if (_stack.size > 1) {
            _stack = listOf(_stack.first())
        }
    }

    internal fun popTo(screen: TrapezeScreen): Boolean {
        val index = _stack.lastIndexOf(screen)
        if (index < 0) return false
        if (index < _stack.size - 1) {
            _stack = _stack.take(index + 1)
        }
        return true
    }

    internal fun asList(): List<TrapezeScreen> = _stack

    internal fun resultsSnapshot(): Map<String, TrapezeNavigationResult> = _results

    public companion object
}

/**
 * Creates and remembers a saveable [TrapezeBackStack] with the given [root] screen.
 *
 * On Android, the backstack is persisted across configuration changes and process death
 * via [rememberSaveable]. On other platforms, the backstack is held in-memory.
 */
@Composable
public expect fun rememberSaveableBackStack(root: TrapezeScreen): TrapezeBackStack
