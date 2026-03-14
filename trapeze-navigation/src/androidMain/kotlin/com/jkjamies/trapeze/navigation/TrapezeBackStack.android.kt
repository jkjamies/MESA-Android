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

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeScreen

/**
 * Saver for [TrapezeBackStack] to persist across configuration changes and process death.
 */
public fun TrapezeBackStack.Companion.saver(): Saver<TrapezeBackStack, *> = Saver(
    save = { backStack ->
        val bundle = Bundle()
        bundle.putParcelableArrayList(
            "stack",
            ArrayList(backStack.asList())
        )
        val resultsBundle = Bundle()
        backStack.resultsSnapshot().forEach { (key, value) ->
            resultsBundle.putParcelable(key, value)
        }
        bundle.putBundle("results", resultsBundle)
        bundle
    },
    restore = { bundle ->
        @Suppress("DEPRECATION")
        val stack = bundle.getParcelableArrayList<Parcelable>("stack")
            ?.filterIsInstance<TrapezeScreen>()
            ?.takeIf { it.isNotEmpty() }
            ?: return@Saver null
        val backStack = TrapezeBackStack(stack.first())
        stack.drop(1).forEach { backStack.push(it) }
        val resultsBundle = bundle.getBundle("results")
        if (resultsBundle != null) {
            for (key in resultsBundle.keySet()) {
                @Suppress("DEPRECATION")
                val result = resultsBundle.getParcelable<Parcelable>(key)
                if (result is TrapezeNavigationResult) {
                    backStack.setResult(key, result)
                }
            }
        }
        backStack
    }
)

@Composable
public actual fun rememberSaveableBackStack(root: TrapezeScreen): TrapezeBackStack {
    return rememberSaveable(root, saver = TrapezeBackStack.saver()) {
        TrapezeBackStack(root)
    }
}
