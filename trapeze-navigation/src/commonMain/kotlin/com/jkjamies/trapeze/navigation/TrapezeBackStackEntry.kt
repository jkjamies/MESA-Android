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

import androidx.compose.runtime.Immutable
import com.jkjamies.trapeze.TrapezeScreen
import kotlin.uuid.Uuid

/**
 * One occupied position in a [TrapezeBackStack].
 *
 * Screens are values: navigating to `HomeScreen` twice produces two *equal* screens. An entry
 * gives each of those positions its own identity, so the two visits keep separate saved UI
 * state and separate navigation results instead of silently sharing them.
 *
 * Entries are created by the backstack; the [id] is generated on push and preserved across
 * configuration changes and process death.
 */
@Immutable
public class TrapezeBackStackEntry internal constructor(
    /** The screen this entry renders. */
    public val screen: TrapezeScreen,
    /** Stable identity for this position in the stack, unique across the backstack's lifetime. */
    public val id: String
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is TrapezeBackStackEntry && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "TrapezeBackStackEntry(screen=$screen, id=$id)"

    internal companion object {
        fun create(screen: TrapezeScreen): TrapezeBackStackEntry =
            TrapezeBackStackEntry(screen, Uuid.random().toString())
    }
}
