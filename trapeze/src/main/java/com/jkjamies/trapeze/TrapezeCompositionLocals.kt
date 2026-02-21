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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for accessing the current [Trapeze] instance.
 */
public val LocalTrapeze = staticCompositionLocalOf<Trapeze> {
    error("No Trapeze provided. Wrap your content with TrapezeCompositionLocals.")
}

/**
 * Provides [Trapeze] to the composition tree via [LocalTrapeze].
 *
 * @param trapeze The [Trapeze] instance to provide.
 * @param content The composable content that can access [LocalTrapeze].
 */
@Composable
public fun TrapezeCompositionLocals(trapeze: Trapeze, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTrapeze provides trapeze, content = content)
}
