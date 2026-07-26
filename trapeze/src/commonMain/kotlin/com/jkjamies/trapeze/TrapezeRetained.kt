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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Holds values and a [CoroutineScope] that outlive recomposition *and* configuration changes.
 *
 * `rememberSaveable` covers state; nothing covered in-flight *work*. A StateHolder that starts a
 * save from an event sink and is then rotated had that save cancelled underneath it, because the
 * scope came from `rememberCoroutineScope()`. A retained store is the missing piece: it survives
 * for as long as its owner is alive, and is cleared exactly once when the owner goes away for
 * good — when its backstack entry is popped, not when the device is turned sideways.
 *
 * Stores are supplied by the navigation layer, one per backstack entry. Reach them through
 * [rememberRetained] and [rememberRetainedCoroutineScope] rather than holding one directly.
 */
@Stable
public class TrapezeRetainedStore {
    private val values = mutableMapOf<String, Any?>()
    private var scope: CoroutineScope? = null
    private var cleared = false

    /**
     * A scope that survives configuration changes and is cancelled when this store is cleared.
     *
     * Uses a [SupervisorJob] so one failed child does not take down unrelated work started from
     * the same screen.
     */
    public val coroutineScope: CoroutineScope
        get() {
            check(!cleared) { "This TrapezeRetainedStore has been cleared and cannot be reused." }
            return scope ?: CoroutineScope(SupervisorJob()).also { scope = it }
        }

    /**
     * Returns the value stored under [key], calling [factory] to create it on first access.
     */
    public fun <T> getOrPut(key: String, factory: () -> T): T {
        check(!cleared) { "This TrapezeRetainedStore has been cleared and cannot be reused." }
        if (key in values) {
            @Suppress("UNCHECKED_CAST")
            return values[key] as T
        }
        return factory().also { values[key] = it }
    }

    /** Drops the value stored under [key], if any. */
    public fun forget(key: String) {
        values.remove(key)
    }

    /**
     * Cancels [coroutineScope] and drops every retained value. Called by the owner when the
     * screen is gone for good; the store must not be used afterwards.
     */
    public fun clear() {
        if (cleared) return
        cleared = true
        scope?.cancel()
        scope = null
        values.clear()
    }
}

/**
 * The [TrapezeRetainedStore] for the screen currently being rendered.
 *
 * `null` when no navigation host is present, in which case [rememberRetained] degrades to plain
 * `remember` and [rememberRetainedCoroutineScope] to `rememberCoroutineScope`. The navigation
 * layer provides a store that genuinely survives configuration changes.
 */
public val LocalTrapezeRetainedStore = staticCompositionLocalOf<TrapezeRetainedStore?> { null }

/**
 * Remembers a value across recomposition *and* configuration changes.
 *
 * Unlike `rememberSaveable` the value is held in memory, so it does not need to be serializable
 * and does not survive process death — pair the two when you need both.
 *
 * ```kotlin
 * val client = rememberRetained("client") { PagingClient(query) }
 * ```
 *
 * The [key] is explicit rather than derived from composition position: MESA prefers a name you
 * can see over one the compiler infers, and it keeps the value stable when surrounding code moves.
 */
@Composable
public fun <T> rememberRetained(key: String, factory: () -> T): T {
    val store = LocalTrapezeRetainedStore.current
    return if (store != null) {
        remember(store, key) { store.getOrPut(key, factory) }
    } else {
        // No retained host in scope — degrade to plain `remember` rather than inventing a
        // lifetime. `TrapezeContent` used standalone behaves exactly as it did before.
        remember(key) { factory() }
    }
}

/**
 * Returns a [CoroutineScope] that survives configuration changes and is cancelled when the screen
 * is gone for good.
 *
 * This is what event sinks should launch from: rotating the device mid-save must not cancel the
 * save, but navigating away from the screen should.
 *
 * Outside a navigation host there is nothing to retain against, so the scope falls back to the
 * lifetime of the composition — matching `rememberCoroutineScope()`.
 */
@Composable
public fun rememberRetainedCoroutineScope(): CoroutineScope {
    val store = LocalTrapezeRetainedStore.current
    return if (store != null) {
        store.coroutineScope
    } else {
        // No retained host in scope. Fall back to the composition's own scope: it inherits the
        // caller's coroutine context, which keeps work on the test scheduler under a headless
        // runtime and matches the behaviour before retained scopes existed.
        rememberCoroutineScope()
    }
}
