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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.jkjamies.trapeze.TrapezeNavigationResult

/**
 * Remembers the navigation result delivered under the given [key].
 *
 * When another screen calls [com.jkjamies.trapeze.TrapezeNavigator.popWithResult] with the
 * same [key], the result is taken off the backstack exactly once and returned here. The
 * value is then *latched*: it keeps being returned until this composable leaves the
 * composition, or until a newer result arrives under the same key.
 *
 * ```kotlin
 * val editResult = rememberNavigationResult("edit_result")
 * LaunchedEffect(editResult) {
 *     (editResult as? EditResult)?.let { name = it.name }
 * }
 * ```
 *
 * Prefer [NavigationResultEffect] when the result should trigger an action exactly once
 * rather than be read as state.
 *
 * @param key The unique key matching the one used in `popWithResult`.
 * @param backStack The backstack to read results from. Defaults to [LocalTrapezeBackStack].
 * @return The most recently delivered result, or `null` if none has arrived.
 */
@Composable
public fun rememberNavigationResult(
    key: String,
    backStack: TrapezeBackStack = LocalTrapezeBackStack.current
): TrapezeNavigationResult? {
    var result by remember(backStack, key) { mutableStateOf<TrapezeNavigationResult?>(null) }
    NavigationResultEffect(key, backStack) { result = it }
    return result
}

/**
 * Invokes [onResult] once for each navigation result delivered under the given [key].
 *
 * The result is removed from the backstack as it is delivered, so it fires exactly once
 * per `popWithResult` call even across recompositions.
 *
 * ```kotlin
 * NavigationResultEffect("edit_result") { result ->
 *     (result as? EditResult)?.let { state.eventSink(NameChanged(it.name)) }
 * }
 * ```
 *
 * @param key The unique key matching the one used in `popWithResult`.
 * @param backStack The backstack to read results from. Defaults to [LocalTrapezeBackStack].
 * @param onResult Called with each delivered result.
 */
@Composable
public fun NavigationResultEffect(
    key: String,
    backStack: TrapezeBackStack = LocalTrapezeBackStack.current,
    onResult: (TrapezeNavigationResult) -> Unit
) {
    val currentOnResult by rememberUpdatedState(onResult)
    // Consumption is a snapshot write, so it must happen in an effect rather than in
    // composition — writing state that was read during composition would invalidate the
    // calling scope and deliver the result for only a single, racy composition pass.
    LaunchedEffect(backStack, key) {
        snapshotFlow { backStack.peekResult(key) }
            .collect { pending ->
                if (pending != null) {
                    backStack.consumeResult(key)?.let(currentOnResult)
                }
            }
    }
}
