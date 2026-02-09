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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders a [TrapezeScreen] by resolving its [TrapezeStateHolder] and [TrapezeUi] from [Trapeze].
 *
 * This is the primary entry point for rendering a single screen. For navigation with a backstack,
 * use [com.jkjamies.trapeze.navigation.NavigableTrapezeContent].
 *
 * @param screen The screen to render.
 * @param modifier Modifier to apply to the UI.
 * @param trapeze The [Trapeze] instance to resolve factories from. Defaults to [LocalTrapeze].
 * @param navigator Optional navigator for screens that need navigation capabilities.
 */
@Composable
public fun TrapezeContent(
    screen: TrapezeScreen,
    modifier: Modifier = Modifier,
    trapeze: Trapeze = LocalTrapeze.current,
    navigator: TrapezeNavigator? = null
) {
    // Navigator is intentionally excluded from the remember key — navigators are stable
    // instances tied to the backstack, so they never change for a given composition.
    val stateHolder = remember(screen) { trapeze.stateHolder(screen, navigator) }
    val ui = remember(screen) { trapeze.ui(screen) }
    if (stateHolder == null || ui == null) {
        Log.w("TrapezeContent", "No factory found for screen: $screen")
        return
    }
    @Suppress("UNCHECKED_CAST")
    TrapezeRenderer(
        modifier = modifier,
        screen = screen,
        stateHolder = stateHolder as TrapezeStateHolder<TrapezeScreen, TrapezeState, TrapezeEvent>,
        ui = ui as TrapezeUi<TrapezeState>
    )
}

/**
 * Internal: Wires a [TrapezeStateHolder] to a [TrapezeUi].
 * Produces state from the StateHolder and passes it to the UI.
 */
@Composable
internal fun <T : TrapezeScreen, S : TrapezeState, E : TrapezeEvent> TrapezeRenderer(
    modifier: Modifier,
    screen: T,
    stateHolder: TrapezeStateHolder<T, S, E>,
    ui: TrapezeUi<S>
) {
    val state = stateHolder.produceState(screen)
    ui(modifier, state)
}