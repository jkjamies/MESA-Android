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

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.jkjamies.trapeze.LocalTrapeze
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
 * Screens animate between one another using [transition], which is told which way the stack
 * moved. Pass [TrapezeTransitions.None] to render changes instantly.
 *
 * @param navigator The navigator for handling navigation events.
 * @param backStack The backstack containing screens to render. The root is the start destination.
 * @param modifier Modifier to apply to the content.
 * @param trapeze The [Trapeze] instance to resolve factories from. Defaults to [LocalTrapeze].
 * @param handleBack Whether to intercept the platform back affordance to pop the backstack.
 * @param transition How one screen gives way to the next.
 */
@Composable
public fun NavigableTrapezeContent(
    navigator: TrapezeNavigator,
    backStack: TrapezeBackStack,
    modifier: Modifier = Modifier,
    trapeze: Trapeze = LocalTrapeze.current,
    handleBack: Boolean = true,
    transition: TrapezeTransitionSpec = TrapezeTransitions.SlideHorizontally,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentEntry = backStack.currentEntry

    TrapezeBackHandler(enabled = handleBack && backStack.size > 1) {
        navigator.pop()
    }

    // Clean up saved state for entries that have been removed from the backstack.
    // Keyed on `backStack` so swapping backstacks restarts tracking, and driven by set
    // difference rather than size: a push and a pop between two snapshot emissions leaves
    // the size unchanged while still removing an entry.
    LaunchedEffect(backStack, saveableStateHolder) {
        var previousIds = backStack.entries.mapTo(mutableSetOf()) { it.id }
        snapshotFlow { backStack.entries }
            .collect { entries ->
                val currentIds = entries.mapTo(mutableSetOf()) { it.id }
                previousIds.forEach { id ->
                    if (id !in currentIds) saveableStateHolder.removeState(id)
                }
                previousIds = currentIds
            }
    }

    CompositionLocalProvider(
        LocalTrapezeNavigator provides navigator,
        LocalTrapezeBackStack provides backStack,
    ) {
        AnimatedContent(
            targetState = currentEntry,
            modifier = modifier,
            transitionSpec = {
                // Direction is *derived*, not tracked: the outgoing entry is still on the stack
                // exactly when we moved forward onto something new, and gone exactly when we
                // came back to something older. That holds for a single `pop` and for the
                // several entries `popTo` and `popToRoot` remove at once, and it needs no
                // remembered history — so nothing here mutates state during composition.
                val movedBack = backStack.entries.none { it.id == initialState.id }
                transition(
                    if (movedBack) {
                        TrapezeNavigationDirection.Backward
                    } else {
                        TrapezeNavigationDirection.Forward
                    }
                )
            },
            // Entries already compare by id, but stating it keeps the animation keyed on the
            // visit rather than on anything a screen's own equality might imply.
            contentKey = { entry -> entry.id },
            label = "TrapezeScreen",
        ) { entry ->
            // Provided *inside* the animation, per rendered entry. During a transition two
            // entries are composed at once, and the outgoing screen must keep seeing its own —
            // `rememberNavigationResult` addresses results by entry, so handing the incoming
            // entry to the outgoing screen would let it consume results meant for its successor.
            CompositionLocalProvider(LocalTrapezeBackStackEntry provides entry) {
                // Keyed on the entry id, not the screen: two visits to an equal screen are
                // distinct positions in the stack and must not share saved UI state.
                saveableStateHolder.SaveableStateProvider(key = entry.id) {
                    TrapezeContent(
                        screen = entry.screen,
                        trapeze = trapeze,
                        navigator = navigator,
                    )
                }
            }
        }
    }
}
