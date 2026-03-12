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

package com.jkjamies.trapeze.test

import com.jkjamies.trapeze.TrapezeNavigationResult
import com.jkjamies.trapeze.TrapezeScreen

/**
 * Sealed hierarchy recording all navigator actions for assertion in tests.
 */
public sealed interface NavigationEvent {
    /**
     * Records a [TrapezeNavigator.navigate] call.
     */
    public data class Navigate(val screen: TrapezeScreen) : NavigationEvent

    /**
     * Records a [TrapezeNavigator.pop] call.
     */
    public data object Pop : NavigationEvent

    /**
     * Records a [TrapezeNavigator.popWithResult] call.
     */
    public data class PopWithResult(
        val key: String,
        val result: TrapezeNavigationResult,
    ) : NavigationEvent

    /**
     * Records a [TrapezeNavigator.popToRoot] call.
     */
    public data object PopToRoot : NavigationEvent

    /**
     * Records a [TrapezeNavigator.popTo] call.
     */
    public data class PopTo(val screen: TrapezeScreen) : NavigationEvent
}
