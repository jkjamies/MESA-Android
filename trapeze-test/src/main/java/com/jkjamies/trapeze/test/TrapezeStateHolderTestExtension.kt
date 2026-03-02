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

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.jkjamies.trapeze.TrapezeEvent
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeState
import com.jkjamies.trapeze.TrapezeStateHolder

/**
 * Runs the [TrapezeStateHolder.produceState] composable in a headless Molecule runtime
 * and pipes the emitted states through a [TrapezeReceiveTurbine] for assertion.
 *
 * Duplicate state emissions (common in Compose recompositions) are automatically filtered
 * by [TrapezeReceiveTurbine.awaitItem], so tests only see meaningful state changes.
 *
 * Usage:
 * ```kotlin
 * val holder = MyStateHolder(initialCount = 0, navigator = navigator)
 * holder.test {
 *     val initial = awaitItem()
 *     initial.count shouldBe 0
 *
 *     initial.eventSink(MyEvent.Increment)
 *     awaitItem().count shouldBe 1
 * }
 * ```
 */
public suspend fun <S : TrapezeScreen, T : TrapezeState, E : TrapezeEvent>
    TrapezeStateHolder<S, T, E>.test(
        validate: suspend TrapezeReceiveTurbine<T>.() -> Unit,
    ) {
    moleculeFlow(RecompositionMode.Immediate) {
        produceState()
    }
        .test {
            TrapezeReceiveTurbine(this).validate()
        }
}
