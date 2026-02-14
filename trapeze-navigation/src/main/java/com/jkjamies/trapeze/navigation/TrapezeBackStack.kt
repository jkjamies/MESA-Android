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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jkjamies.trapeze.TrapezeScreen

/**
 * A saveable backstack of [TrapezeScreen]s for navigation.
 *
 * @param root The initial/root screen of the backstack.
 */
@Stable
public class TrapezeBackStack internal constructor(root: TrapezeScreen) {
    private var _stack by mutableStateOf(listOf(root))
    
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

    internal fun asList(): List<TrapezeScreen> = _stack

    public companion object {
        /**
         * Saver for [TrapezeBackStack] to persist across configuration changes.
         */
        public fun saver(): Saver<TrapezeBackStack, *> = listSaver(
            save = { backStack -> backStack.asList() },
            restore = { list ->
                TrapezeBackStack(list.first()).apply {
                    list.drop(1).forEach { push(it) }
                }
            }
        )
    }
}

/**
 * Creates and remembers a saveable [TrapezeBackStack] with the given [root] screen.
 */
@Composable
public fun rememberSaveableBackStack(root: TrapezeScreen): TrapezeBackStack {
    return rememberSaveable(saver = TrapezeBackStack.saver()) {
        TrapezeBackStack(root)
    }
}
