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

package com.jkjamies.trapeze.features.summary.presentation

import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeStateHolder
import com.jkjamies.trapeze.TrapezeUi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Factory that creates [SummaryStateHolder] for [SummaryScreen].
 */
@ContributesIntoSet(AppScope::class)
class SummaryStateHolderFactory @Inject constructor(
    private val factory: SummaryStateHolder.Factory
) : Trapeze.StateHolderFactory {
    override fun create(
        screen: TrapezeScreen,
        navigator: TrapezeNavigator?
    ): TrapezeStateHolder<*, *, *>? {
        return if (screen is SummaryScreen && navigator != null) {
            factory.create(navigator)
        } else null
    }
}

/**
 * Factory that provides [SummaryUi] for [SummaryScreen].
 */
@ContributesIntoSet(AppScope::class)
class SummaryUiFactory @Inject constructor() : Trapeze.UiFactory {
    override fun create(screen: TrapezeScreen): TrapezeUi<*>? {
        return if (screen is SummaryScreen) ::SummaryUi else null
    }
}
