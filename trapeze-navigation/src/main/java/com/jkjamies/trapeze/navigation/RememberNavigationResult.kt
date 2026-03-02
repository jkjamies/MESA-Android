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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jkjamies.trapeze.TrapezeNavigationResult

/**
 * Remembers and consumes a navigation result for the given [key].
 *
 * When a previous screen calls [com.jkjamies.trapeze.TrapezeNavigator.popWithResult] with
 * the same [key], this composable returns the result on the next recomposition.
 * The result is consumed on first read — subsequent calls return `null` until a new result
 * is delivered.
 *
 * @param key The unique key matching the one used in `popWithResult`.
 * @param backStack The backstack to read results from. Defaults to [LocalTrapezeBackStack].
 * @return The result if one is pending, or `null` otherwise.
 */
@Composable
public fun rememberNavigationResult(
    key: String,
    backStack: TrapezeBackStack = LocalTrapezeBackStack.current
): TrapezeNavigationResult? {
    val result by remember { mutableStateOf<TrapezeNavigationResult?>(null) }
    val consumed = backStack.consumeResult(key)
    if (consumed != null) {
        return consumed
    }
    return result
}
