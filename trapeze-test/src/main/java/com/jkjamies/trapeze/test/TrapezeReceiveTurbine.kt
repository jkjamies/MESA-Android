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

import app.cash.turbine.ReceiveTurbine

/**
 * A Trapeze-specific extension to [ReceiveTurbine] for the [TrapezeStateHolder.test] DSL.
 *
 * This implementation slightly alters the behavior of [awaitItem] by only emitting items
 * that are _different_ from the previously emitted item. This filters noise from redundant
 * Compose recompositions that produce identical state objects.
 */
public class TrapezeReceiveTurbine<T>(
    private val delegate: ReceiveTurbine<T>,
) : ReceiveTurbine<T> by delegate {

    private var hasLastItem: Boolean = false
    private var lastItem: T? = null

    /**
     * Awaits the next item that is different from the previously emitted item.
     *
     * Intermediate duplicate emissions are silently consumed, preventing tests from
     * having to account for recomposition noise.
     */
    override suspend fun awaitItem(): T {
        while (true) {
            val next = delegate.awaitItem()
            if (!hasLastItem || lastItem != next) {
                lastItem = next
                hasLastItem = true
                return next
            }
        }
    }

    /**
     * Awaits the next item and asserts that it is unchanged from the previous emission.
     */
    public suspend fun awaitUnchanged() {
        val next = delegate.awaitItem()
        if (!hasLastItem || next != lastItem) {
            throw AssertionError(
                "Expected unchanged item but received $next. Previous was $lastItem."
            )
        }
        lastItem = next
    }
}
