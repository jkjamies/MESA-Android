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

/**
 * Interface for navigating between [TrapezeScreen]s.
 *
 * Implementations handle screen navigation logic such as pushing to and popping from a backstack.
 */
public interface TrapezeNavigator {
    /**
     * Navigates to the given [screen].
     */
    public fun navigate(screen: TrapezeScreen)

    /**
     * Pops the current screen from the navigation stack.
     */
    public fun pop()

    /**
     * Pops the current screen and delivers [result] to the previous screen under the given [key].
     *
     * The previous screen can consume the result using `rememberNavigationResult<R>(key)`.
     *
     * @param key A unique key identifying this result. Must match between producer and consumer.
     * @param result The result data to pass back.
     */
    public fun <R : TrapezeNavigationResult> popWithResult(key: String, result: R)

    /**
     * Pops the entire backstack to the root screen.
     */
    public fun popToRoot() {}

    /**
     * Pops the backstack to the given [screen].
     *
     * If the screen is not found in the backstack, this is a no-op.
     * If the screen appears multiple times, pops to the most recent (last) occurrence.
     *
     * @return `true` if the screen was found and the backstack was updated, `false` otherwise.
     */
    public fun popTo(screen: TrapezeScreen): Boolean = false
}
