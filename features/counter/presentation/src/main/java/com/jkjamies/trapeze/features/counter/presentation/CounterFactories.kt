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

package com.jkjamies.trapeze.features.counter.presentation

import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.TrapezeUi
import com.jkjamies.trapeze.core.presentation.AppInterop
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Factory that creates [CounterStateHolder] for [CounterScreen].
 */
@ContributesIntoSet(AppScope::class)
class CounterStateHolderFactory @Inject constructor(
    private val factory: CounterStateHolder.Factory,
    private val appInterop: AppInterop
) : Trapeze.StateHolderFactory {
    override fun create(
        screen: TrapezeScreen,
        navigator: TrapezeNavigator?
    ): TrapezeStateHolder<*, *, *>? {
        return if (screen is CounterScreen && navigator != null) {
            factory.create(appInterop, navigator)
        } else null
    }
}

/**
 * Factory that provides [CounterUi] for [CounterScreen].
 */
@ContributesIntoSet(AppScope::class)
class CounterUiFactory @Inject constructor() : Trapeze.UiFactory {
    override fun create(screen: TrapezeScreen): TrapezeUi<*>? {
        return if (screen is CounterScreen) ::CounterUi else null
    }
}
