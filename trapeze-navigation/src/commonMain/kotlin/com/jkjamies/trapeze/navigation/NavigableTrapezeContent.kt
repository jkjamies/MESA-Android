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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.jkjamies.trapeze.LocalTrapeze
import com.jkjamies.trapeze.LocalTrapezeRetainedStore
import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeContent
import com.jkjamies.trapeze.TrapezeNavigator

/**
 * Renders navigable content from a [TrapezeBackStack], resolving screens via [Trapeze].
 *
 * This is the primary entry point for navigation with a backstack.
 * For rendering a single screen without navigation, use [TrapezeContent].
 *
 * While more than one screen is on the stack, the platform back affordance pops the backstack.
 * At the root it is left alone, so the host (an Activity, say) handles it as usual. Set
 * [handleBack] to `false` to take over back handling yourself.
 *
 * @param navigator The navigator for handling navigation events.
 * @param backStack The backstack containing screens to render. The root is the start destination.
 * @param modifier Modifier to apply to the content.
 * @param trapeze The [Trapeze] instance to resolve factories from. Defaults to [LocalTrapeze].
 * @param handleBack Whether to intercept the platform back affordance to pop the backstack.
 */
@Composable
public fun NavigableTrapezeContent(
    navigator: TrapezeNavigator,
    backStack: TrapezeBackStack,
    modifier: Modifier = Modifier,
    trapeze: Trapeze = LocalTrapeze.current,
    handleBack: Boolean = true
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val retainedHost = rememberTrapezeRetainedHost()
    val currentEntry = backStack.currentEntry

    TrapezeBackHandler(enabled = handleBack && backStack.size > 1) {
        navigator.pop()
    }

    // Release both kinds of per-entry state when an entry leaves the backstack: the saved UI
    // state, and the retained store (which cancels any work still running for that screen).
    // Keyed on `backStack` so swapping backstacks restarts tracking, and driven by set
    // difference rather than size: a push and a pop between two snapshot emissions leaves
    // the size unchanged while still removing an entry.
    //
    // Nothing here fires on a configuration change — the entries are unchanged and the host
    // outlives the composition — which is exactly what keeps in-flight work alive across one.
    LaunchedEffect(backStack, saveableStateHolder, retainedHost) {
        var previousIds = backStack.entries.mapTo(mutableSetOf()) { it.id }
        snapshotFlow { backStack.entries }
            .collect { entries ->
                val currentIds = entries.mapTo(mutableSetOf()) { it.id }
                previousIds.forEach { id ->
                    if (id !in currentIds) {
                        saveableStateHolder.removeState(id)
                        retainedHost.clear(id)
                    }
                }
                previousIds = currentIds
            }
    }

    CompositionLocalProvider(
        LocalTrapezeNavigator provides navigator,
        LocalTrapezeBackStack provides backStack,
        LocalTrapezeBackStackEntry provides currentEntry,
        LocalTrapezeRetainedStore provides retainedHost.storeFor(currentEntry.id)
    ) {
        // Keyed on the entry id, not the screen: two visits to an equal screen are distinct
        // positions in the stack and must not share saved UI state.
        saveableStateHolder.SaveableStateProvider(key = currentEntry.id) {
            TrapezeContent(
                screen = currentEntry.screen,
                modifier = modifier,
                trapeze = trapeze,
                navigator = navigator
            )
        }
    }
}
