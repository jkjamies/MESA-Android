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

package com.jkjamies.strata

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches a coroutine on [Dispatchers.Default] and immediately verifies that its [Job] is not
 * already cancelled.
 *
 * Use this instead of bare [launch] inside event sinks to fail fast when the
 * [CoroutineScope] has been cancelled (e.g., after composition disposal).
 *
 * The coroutine runs on [Dispatchers.Default] unless overridden via [context]
 * (e.g., `strataLaunch(Dispatchers.Main) { … }`).
 *
 * @throws IllegalStateException if the returned [Job] is already cancelled at launch time.
 */
public fun CoroutineScope.strataLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(Dispatchers.Default + context, start, block).also {
    check(!it.isCancelled) {
        "launch failed. Job is already cancelled"
    }
}

/**
 * Launches a coroutine on [Dispatchers.Default] that wraps [block] in [strataRunCatching],
 * returning a [Deferred] of [StrataResult].
 *
 * This combines the threading and cancellation guarantees of [strataLaunch] with automatic
 * error handling via [strataRunCatching], so callers get structured results without manual
 * try/catch or wrapping.
 *
 * The coroutine runs on [Dispatchers.Default] unless overridden via [context]
 * (e.g., `strataLaunchWithResult(Dispatchers.Main) { … }`).
 *
 * @throws IllegalStateException if the returned [Deferred] is already cancelled at launch time.
 */
public fun <T> CoroutineScope.strataLaunchWithResult(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<StrataResult<T>> = async(Dispatchers.Default + context, start) {
    strataRunCatching { block() }
}.also {
    check(!it.isCancelled) {
        "launch failed. Job is already cancelled"
    }
}
