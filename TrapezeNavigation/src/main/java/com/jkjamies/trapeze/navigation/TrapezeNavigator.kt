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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen

/**
 * CompositionLocal for accessing the current [TrapezeNavigator].
 */
public val LocalTrapezeNavigator = staticCompositionLocalOf<TrapezeNavigator> {
    error("No TrapezeNavigator provided")
}

/**
 * Creates and remembers a [TrapezeNavigator] backed by the given [backStack].
 *
 * @param backStack The backstack to navigate with.
 * @param onRootPop Optional callback when pop is called on the root screen.
 */
@Composable
public fun rememberTrapezeNavigator(
    backStack: TrapezeBackStack,
    onRootPop: (() -> Unit)? = null
): TrapezeNavigator {
    val saveableStateHolder = rememberSaveableStateHolder()
    return remember(backStack) {
        object : TrapezeNavigator {
            override fun navigate(screen: TrapezeScreen) {
                backStack.push(screen)
            }

            override fun pop() {
                if (backStack.size > 1) {
                    val popped = backStack.current
                    backStack.pop()
                    saveableStateHolder.removeState(popped)
                } else {
                    onRootPop?.invoke()
                }
            }
        }
    }
}
