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
}
