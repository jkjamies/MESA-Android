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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Base class for streaming business logic in Strata.
 *
 * Subclass and implement [createObservable] to return a [Flow] for a given set of parameters.
 * [invoke] switches the active subscription and [stop] tears it down; [flow] always reflects
 * the most recent call.
 *
 * A subject interactor starts *idle* — [flow] emits nothing until [invoke] is called. Trigger
 * it from the UI or logic layer (typically a `LaunchedEffect`), never from an `init` block:
 *
 * ```kotlin
 * LaunchedEffect(userId) { observeUser(userId) }
 * val user by observeUser.flow.collectAsState(initial = null)
 * ```
 *
 * Repeated calls to [invoke] with equal parameters are conflated, so re-triggering from a
 * recomposition will not resubscribe to the underlying source.
 *
 * @param P The parameter type. Must be non-null so parameters can be compared for equality.
 * @param T The emitted value type.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public abstract class StrataSubjectInteractor<P : Any, T> {

    // A StateFlow is deliberate: `invoke` is not a suspending function, and assigning
    // `.value` neither suspends nor drops. Equal assignments conflate, which is exactly the
    // "don't resubscribe for the same parameters" behaviour we want, with no buffer to size.
    private val subscription = MutableStateFlow<Subscription<P>>(Subscription.Idle)

    /**
     * Whether consecutive equal *values* are filtered out of [flow].
     *
     * Defaults to `false`: a repeat emission usually means something happened — a refresh
     * completed, a write was confirmed — and silently dropping it hides real events. Override
     * to `true` when the source is chatty and the consumer only cares about changes.
     *
     * This concerns emitted values only. Equal *parameters* are always conflated, which can
     * only avoid a redundant resubscribe and can never lose an emission.
     */
    protected open val distinctValues: Boolean = false

    /**
     * The values produced by the currently active subscription.
     *
     * Emits nothing while idle. Switching parameters cancels the previous source before
     * subscribing to the new one.
     */
    // `by lazy` rather than a direct initializer: `distinctValues` is open, and reading an
    // open member from a constructor initializer would observe the base-class default rather
    // than a subclass override.
    public val flow: Flow<T> by lazy {
        val values = subscription.flatMapLatest { current ->
            when (current) {
                is Subscription.Idle -> emptyFlow()
                is Subscription.Active -> createObservable(current.params)
            }
        }
        if (distinctValues) values.distinctUntilChanged() else values
    }

    /** Whether a subscription is currently active. */
    public val isActive: Boolean get() = subscription.value is Subscription.Active

    /**
     * Subscribes to [createObservable] with the given [params], replacing any active
     * subscription. Calling this with parameters equal to the current ones is a no-op.
     */
    public operator fun invoke(params: P) {
        subscription.value = Subscription.Active(params)
    }

    /**
     * Cancels the active subscription and returns to idle. [flow] stops emitting until
     * [invoke] is called again.
     */
    public fun stop() {
        subscription.value = Subscription.Idle
    }

    protected abstract fun createObservable(params: P): Flow<T>

    private sealed interface Subscription<out P : Any> {
        data object Idle : Subscription<Nothing>
        data class Active<P : Any>(val params: P) : Subscription<P>
    }
}
