package com.jkjamies.trapeze

import androidx.compose.runtime.Composable

/**
 * The Brain: StateHolder manages the logic lifecycle.
 * No initial state is required in the constructor.
 */
abstract class TrapezeStateHolder<T : TrapezeScreen, S : TrapezeState, E : TrapezeEvent> {
    @Composable
    abstract fun produceState(screen: T): S
}