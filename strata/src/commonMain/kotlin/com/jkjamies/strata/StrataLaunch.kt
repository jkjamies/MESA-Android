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
 * Launches a coroutine on [Dispatchers.Default].
 *
 * Use this instead of a bare [launch] inside event sinks: it pins background work off the
 * main thread by default rather than inheriting whatever dispatcher the calling scope
 * happens to carry.
 *
 * Override the dispatcher via [context] (e.g. `strataLaunch(Dispatchers.Main) { … }`).
 *
 * If the receiving [CoroutineScope] has already been cancelled — a UI event arriving as the
 * composition is torn down, for instance — the returned [Job] is already cancelled and
 * [block] never runs. That is a normal outcome, not an error: dropping a late event is
 * correct, and crashing the caller for it would not be. Check [Job.isCancelled] if the
 * distinction matters.
 *
 * @throws IllegalArgumentException if [context] carries a [Job], which would detach the
 *   coroutine from the scope's structured concurrency.
 */
public fun CoroutineScope.strataLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    require(context[Job] == null) {
        "strataLaunch does not accept a Job in `context` — it would detach the coroutine " +
            "from this scope. Launch in a different scope instead."
    }
    return launch(Dispatchers.Default + context, start, block)
}

/**
 * Launches a coroutine on [Dispatchers.Default] that wraps [block] in [strataRunCatching],
 * returning a [Deferred] of [StrataResult].
 *
 * Combines the threading behaviour of [strataLaunch] with automatic error wrapping, so
 * callers get a structured result without manual try/catch.
 *
 * As with [strataLaunch], an already-cancelled scope yields an already-cancelled [Deferred]
 * and [block] never runs.
 *
 * @throws IllegalArgumentException if [context] carries a [Job].
 */
public fun <T> CoroutineScope.strataLaunchWithResult(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<StrataResult<T>> {
    require(context[Job] == null) {
        "strataLaunchWithResult does not accept a Job in `context` — it would detach the " +
            "coroutine from this scope. Launch in a different scope instead."
    }
    return async(Dispatchers.Default + context, start) {
        strataRunCatching { block() }
    }
}
