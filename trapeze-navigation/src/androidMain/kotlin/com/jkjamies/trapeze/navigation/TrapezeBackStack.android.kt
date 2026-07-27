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

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeScreen

private const val KEY_SCREENS = "screens"
private const val KEY_IDS = "ids"
private const val KEY_RESULTS = "results"

/**
 * Saver for [TrapezeBackStack] to persist across configuration changes and process death.
 *
 * Screens and entry ids are stored as parallel lists so an entry keeps its identity — and with
 * it its saved UI state and pending results — across process death.
 */
public fun TrapezeBackStack.Companion.saver(): Saver<TrapezeBackStack, *> = Saver(
    save = { backStack ->
        val bundle = Bundle()
        val entries = backStack.entries
        bundle.putParcelableArrayList(KEY_SCREENS, ArrayList(entries.map { it.screen }))
        bundle.putStringArrayList(KEY_IDS, ArrayList(entries.map { it.id }))

        val resultsBundle = Bundle()
        backStack.resultsSnapshot().forEach { (entryId, forEntry) ->
            val entryBundle = Bundle()
            forEntry.forEach { (key, value) -> entryBundle.putParcelable(key, value) }
            resultsBundle.putBundle(entryId, entryBundle)
        }
        bundle.putBundle(KEY_RESULTS, resultsBundle)
        bundle
    },
    restore = { bundle ->
        val screens = bundle.parcelableArrayList(KEY_SCREENS)
        val ids = bundle.getStringArrayList(KEY_IDS)

        // A screen that no longer unparcels — a renamed class, a CREATOR stripped by R8 —
        // must not be silently skipped: dropping an entry from the middle of the stack
        // rewrites the user's history into something they never navigated. Restore the
        // longest valid prefix instead, and say so.
        val entries = buildList {
            if (screens == null || ids == null || screens.size != ids.size) return@buildList
            for (index in screens.indices) {
                val screen = screens[index] as? TrapezeScreen
                if (screen == null) {
                    val dropped = screens.size - index
                    Log.w(
                        "TrapezeBackStack",
                        "Could not restore backstack entry at index $index. Truncating the " +
                            "restored backstack here; $dropped " +
                            "${if (dropped == 1) "entry was" else "entries were"} dropped."
                    )
                    return@buildList
                }
                add(TrapezeBackStackEntry(screen, ids[index]))
            }
        }
        if (entries.isEmpty()) return@Saver null

        val backStack = TrapezeBackStack(entries.first().screen)
        backStack.restore(entries)

        val resultsBundle = bundle.getBundle(KEY_RESULTS)
        if (resultsBundle != null) {
            val liveIds = entries.mapTo(mutableSetOf()) { it.id }
            val restored = mutableMapOf<String, Map<String, TrapezeNavigationResult>>()
            for (entryId in resultsBundle.keySet()) {
                // Results addressed to an entry that did not survive restore have nowhere to go.
                if (entryId !in liveIds) continue
                val entryBundle = resultsBundle.getBundle(entryId) ?: continue
                val forEntry = mutableMapOf<String, TrapezeNavigationResult>()
                for (key in entryBundle.keySet()) {
                    val result = entryBundle.parcelable(key)
                    if (result is TrapezeNavigationResult) forEntry[key] = result
                }
                if (forEntry.isNotEmpty()) restored[entryId] = forEntry
            }
            backStack.restoreResults(restored)
        }
        backStack
    }
)

@Suppress("DEPRECATION")
private fun Bundle.parcelableArrayList(key: String): ArrayList<Parcelable>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, Parcelable::class.java)
    } else {
        getParcelableArrayList(key)
    }

@Suppress("DEPRECATION")
private fun Bundle.parcelable(key: String): Parcelable? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, Parcelable::class.java)
    } else {
        getParcelable(key)
    }

@Composable
public actual fun rememberSaveableBackStack(root: TrapezeScreen): TrapezeBackStack {
    return rememberSaveable(root, saver = TrapezeBackStack.saver()) {
        TrapezeBackStack(root)
    }
}
