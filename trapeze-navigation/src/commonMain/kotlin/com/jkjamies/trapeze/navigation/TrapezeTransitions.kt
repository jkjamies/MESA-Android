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

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

/**
 * Which way the backstack moved, so a transition can face the right direction.
 *
 * [Backward] is reported when the entry being navigated *to* was already on the stack — a `pop`,
 * `popTo`, or `popToRoot`. Everything else is [Forward], including a `navigate` that happens to
 * land on a screen equal to one already below it: screens are values, so that push is a genuinely
 * new position in the stack (see `TrapezeBackStackEntry`).
 */
public enum class TrapezeNavigationDirection {
    Forward,
    Backward,
}

/**
 * How one screen gives way to the next.
 *
 * The receiver is Compose's own [AnimatedContentTransitionScope], so the full animation vocabulary
 * — `slideIntoContainer`, `scaleIn`, `togetherWith` — is available without MESA wrapping any of it.
 *
 * ```kotlin
 * NavigableTrapezeContent(navigator, backStack) { direction ->
 *     if (direction == TrapezeNavigationDirection.Forward) {
 *         slideIntoContainer(SlideDirection.Left) togetherWith fadeOut()
 *     } else {
 *         fadeIn() togetherWith slideOutOfContainer(SlideDirection.Right)
 *     }
 * }
 * ```
 */
public typealias TrapezeTransitionSpec =
    AnimatedContentTransitionScope<TrapezeBackStackEntry>.(TrapezeNavigationDirection) -> ContentTransform

/** Ready-made [TrapezeTransitionSpec]s. */
public object TrapezeTransitions {

    /**
     * The default: the incoming screen slides in from the edge it is travelling from, while the
     * outgoing one slides out the other way, both crossfading.
     *
     * Sliding is expressed with `slideIntoContainer`/`slideOutOfContainer` rather than a fixed
     * offset so the distance is the size of the container the screens are actually laid out in.
     */
    public val SlideHorizontally: TrapezeTransitionSpec = { direction ->
        val towards = when (direction) {
            TrapezeNavigationDirection.Forward -> AnimatedContentTransitionScope.SlideDirection.Left
            TrapezeNavigationDirection.Backward -> AnimatedContentTransitionScope.SlideDirection.Right
        }
        val spec = tween<Float>(durationMillis = TRANSITION_MILLIS)
        (slideIntoContainer(towards) + fadeIn(spec)) togetherWith
            (slideOutOfContainer(towards) + fadeOut(spec))
    }

    /** A plain crossfade, for hosts where lateral movement reads as the wrong metaphor. */
    public val Fade: TrapezeTransitionSpec = {
        val spec = tween<Float>(durationMillis = TRANSITION_MILLIS)
        fadeIn(spec) togetherWith fadeOut(spec)
    }

    /**
     * No animation at all — the next screen simply replaces the current one.
     *
     * Worth reaching for in tests and screenshot suites, where an in-flight animation is a source
     * of flakiness rather than a thing under test.
     */
    public val None: TrapezeTransitionSpec = {
        EnterTransition.None togetherWith ExitTransition.None
    }

    private const val TRANSITION_MILLIS = 300
}
