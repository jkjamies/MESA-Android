package com.jkjamies.trapeze

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Entry Point: Bridges the Logic and the UI.
 * Generic constraints ensure S is identical in both.
 */
@Composable
fun <T : TrapezeScreen, S : TrapezeState, E : TrapezeEvent> TrapezeContent(
    modifier: Modifier = Modifier,
    screen: T,
    stateHolder: TrapezeStateHolder<T, S, E>,
    ui: TrapezeUi<S>
) {
    val state = stateHolder.produceState(screen)
    ui(modifier, state)
}