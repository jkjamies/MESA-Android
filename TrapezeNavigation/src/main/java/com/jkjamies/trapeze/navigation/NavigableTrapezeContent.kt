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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
 * @param navigator The navigator for handling navigation events.
 * @param backStack The backstack containing screens to render. The root is the start destination.
 * @param modifier Modifier to apply to the content.
 * @param trapeze The [Trapeze] instance to resolve factories from. Defaults to [LocalTrapeze].
 */
@Composable
public fun NavigableTrapezeContent(
    navigator: TrapezeNavigator,
    backStack: TrapezeBackStack,
    modifier: Modifier = Modifier,
    trapeze: Trapeze = LocalTrapeze.current
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentScreen = backStack.current

    CompositionLocalProvider(LocalTrapezeNavigator provides navigator) {
        saveableStateHolder.SaveableStateProvider(key = currentScreen) {
            TrapezeContent(
                screen = currentScreen,
                modifier = modifier,
                trapeze = trapeze,
                navigator = navigator
            )
        }
    }
}
