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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * This target has no Activity-style recreation to survive, so the host lives for as long as the
 * composition does. Retained values still outlive recomposition and are still cleared per entry
 * when a screen is popped — only the configuration-change case is a no-op here.
 */
@Composable
public actual fun rememberTrapezeRetainedHost(): TrapezeRetainedHost {
    val host = remember { MapRetainedHost() }
    DisposableEffect(host) {
        onDispose { host.clearAll() }
    }
    return host
}
