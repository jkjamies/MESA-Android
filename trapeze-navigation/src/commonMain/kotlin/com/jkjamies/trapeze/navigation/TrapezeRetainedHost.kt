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
import com.jkjamies.trapeze.TrapezeRetainedStore

/**
 * Owns one [TrapezeRetainedStore] per backstack entry, outliving configuration changes.
 *
 * This is the piece that makes "survives rotation, dies on pop" possible: the host lives above
 * the composition, so recreating the composition does not recreate the stores.
 */
public interface TrapezeRetainedHost {
    /** The store for [entryId], created on first request. */
    public fun storeFor(entryId: String): TrapezeRetainedStore

    /** Clears and drops the store for [entryId]. Called when its entry leaves the backstack. */
    public fun clear(entryId: String)
}

/**
 * Returns a [TrapezeRetainedHost] that survives configuration changes.
 *
 * On Android this is backed by a `ViewModel`, which is the only thing on the platform that
 * reliably outlives Activity recreation. On targets that have no equivalent recreation to
 * survive, the host simply lives for as long as the composition does.
 */
@Composable
public expect fun rememberTrapezeRetainedHost(): TrapezeRetainedHost

/**
 * Shared implementation: a plain map of stores, cleared together when the host goes away.
 */
internal class MapRetainedHost : TrapezeRetainedHost {
    private val stores = mutableMapOf<String, TrapezeRetainedStore>()

    override fun storeFor(entryId: String): TrapezeRetainedStore =
        stores.getOrPut(entryId) { TrapezeRetainedStore() }

    override fun clear(entryId: String) {
        stores.remove(entryId)?.clear()
    }

    fun clearAll() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}
