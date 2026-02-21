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

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for all Trapeze logic holders.
 *
 * A StateHolder is the "brain" of a screen — it owns the business logic and produces an
 * immutable [TrapezeState] that the UI renders. There is no initial state; instead,
 * [produceState] is called inside the composition to build and emit state reactively.
 *
 * Use [wrapEventSink] inside [produceState] to create a coroutine-safe event callback
 * that silently drops events when the composition's [CoroutineScope] is no longer active.
 *
 * @param T The [TrapezeScreen] type this holder is associated with.
 * @param S The [TrapezeState] type this holder produces.
 * @param E The [TrapezeEvent] type this holder handles.
 */
public abstract class TrapezeStateHolder<T : TrapezeScreen, S : TrapezeState, E : TrapezeEvent> {
    @Composable
    public abstract fun produceState(screen: T): S

    @Composable
    protected inline fun <reified EV : E> wrapEventSink(
        crossinline block: CoroutineScope.(EV) -> Unit
    ): (EV) -> Unit = com.jkjamies.trapeze.wrapEventSink(block)
}