package com.jkjamies.trapeze

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Glue: Typealias for the UI signature.
 * This ensures the UI is a standard Composable function that
 * accepts a specific State type.
 */
typealias TrapezeUi<S> = @Composable (modifier: Modifier, state: S) -> Unit
