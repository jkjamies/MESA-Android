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

import androidx.compose.runtime.Immutable
import com.jkjamies.trapeze.TrapezeNavigator

/**
 * Central circuit for Trapeze that adapts [StateHolderFactory] instances to their corresponding
 * [UiFactory] instances using [TrapezeScreen]. Create instances using [Builder].
 *
 * This instance should usually live on your application's DI graph.
 *
 * ## Usage
 *
 * ```kotlin
 * val trapeze = Trapeze.Builder()
 *     .addStateHolderFactory(myStateHolderFactory)
 *     .addUiFactory(myUiFactory)
 *     .build()
 *
 * TrapezeCompositionLocals(trapeze) {
 *     TrapezeContent(MyScreen)
 * }
 * ```
 */
@Immutable
public class Trapeze private constructor(builder: Builder) {
    private val stateHolderFactories: List<StateHolderFactory> = builder.stateHolderFactories.toList()
    private val uiFactories: List<UiFactory> = builder.uiFactories.toList()

    /**
     * Returns a [TrapezeStateHolder] for the given [screen], or null if none is found.
     * @param navigator Optional navigator for screens that need navigation capabilities.
     */
    internal fun stateHolder(
        screen: TrapezeScreen,
        navigator: TrapezeNavigator?
    ): TrapezeStateHolder<*, *, *>? {
        for (factory in stateHolderFactories) {
            factory.create(screen, navigator)?.let { return it }
        }
        return null
    }

    /**
     * Returns a [TrapezeUi] for the given [screen], or null if none is found.
     */
    internal fun ui(screen: TrapezeScreen): TrapezeUi<*>? {
        for (factory in uiFactories) {
            factory.create(screen)?.let { return it }
        }
        return null
    }

    /**
     * Factory for creating [TrapezeStateHolder] instances.
     * Implementations should check if the screen type matches and return null if not.
     */
    public fun interface StateHolderFactory {
        /**
         * Creates a [TrapezeStateHolder] for the given [screen], or null if this factory
         * does not handle the given screen type.
         * @param navigator Optional navigator for screens that need navigation capabilities.
         */
        public fun create(screen: TrapezeScreen, navigator: TrapezeNavigator?): TrapezeStateHolder<*, *, *>?
    }

    /**
     * Factory for creating [TrapezeUi] instances.
     * Implementations should check if the screen type matches and return null if not.
     */
    public fun interface UiFactory {
        /**
         * Creates a [TrapezeUi] for the given [screen], or null if this factory
         * does not handle the given screen type.
         */
        public fun create(screen: TrapezeScreen): TrapezeUi<*>?
    }

    /**
     * Builder for creating [Trapeze] instances.
     */
    public class Builder {
        internal val stateHolderFactories = mutableListOf<StateHolderFactory>()
        internal val uiFactories = mutableListOf<UiFactory>()

        /**
         * Adds a [StateHolderFactory] to this builder.
         */
        public fun addStateHolderFactory(factory: StateHolderFactory): Builder = apply {
            stateHolderFactories.add(factory)
        }

        /**
         * Adds a [UiFactory] to this builder.
         */
        public fun addUiFactory(factory: UiFactory): Builder = apply {
            uiFactories.add(factory)
        }

        /**
         * Builds the [Trapeze] instance.
         */
        public fun build(): Trapeze = Trapeze(this)
    }
}
