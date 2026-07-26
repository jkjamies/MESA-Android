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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for one-shot business logic operations in Strata.
 *
 * Subclass and implement [doWork] to define the operation. Invoke via the `operator fun invoke`,
 * which applies the timeout, wraps failures into [StrataResult], and tracks loading state.
 *
 * @param P The parameter type.
 * @param R The result type on success.
 */
public abstract class StrataInteractor<in P, R> {
    private val loadingState = MutableStateFlow(State())

    /**
     * How long to wait before reporting *ambient* work as in progress.
     *
     * Background refreshes that finish quickly should not flash a spinner. Work the user
     * explicitly asked for is never delayed — see [inProgress]. Override to tune, or set to
     * [Duration.ZERO] to report every load immediately.
     */
    protected open val ambientLoadingDelay: Duration = 5.seconds

    /** The timeout applied by [invoke] when the caller does not pass one explicitly. */
    protected open val defaultTimeout: Duration = DefaultTimeout

    /**
     * Emits `true` while work is running.
     *
     * Work flagged as user-initiated is reported immediately. Purely ambient work is delayed
     * by [ambientLoadingDelay] so short background refreshes never flash a spinner. Once any
     * user-initiated call is in flight the indicator turns on right away, even if ambient work
     * started first.
     */
    // `by lazy` so an `ambientLoadingDelay` override is visible: reading an open member from a
    // constructor initializer would see the base-class default.
    @OptIn(FlowPreview::class)
    public val inProgress: Flow<Boolean> by lazy {
        val delay = ambientLoadingDelay
        loadingState
            .debounce { state ->
                // Only defer when the work is *entirely* ambient. Debouncing whenever any
                // ambient work happened to be running would delay the user's own spinner.
                if (state.userCount == 0 && state.ambientCount > 0) delay else Duration.ZERO
            }
            .map { (it.userCount + it.ambientCount) > 0 }
            .distinctUntilChanged()
    }

    private fun addLoader(fromUser: Boolean) {
        loadingState.update {
            if (fromUser) {
                it.copy(userCount = it.userCount + 1)
            } else {
                it.copy(ambientCount = it.ambientCount + 1)
            }
        }
    }

    private fun removeLoader(fromUser: Boolean) {
        loadingState.update {
            if (fromUser) {
                it.copy(userCount = it.userCount - 1)
            } else {
                it.copy(ambientCount = it.ambientCount - 1)
            }
        }
    }

    /**
     * Executes the interactor with the given [params].
     *
     * @param params The parameters for the execution.
     * @param timeout The timeout duration.
     * @param userInitiated Whether this execution was initiated by a user action (affects loading state).
     * @return A [StrataResult] containing the result or failure.
     */
    public suspend operator fun invoke(
        params: P,
        timeout: Duration = defaultTimeout,
        userInitiated: Boolean = params.isUserInitiated,
    ): StrataResult<R> = withLoader(userInitiated) {
        try {
            strataRunCatching {
                withTimeout(timeout) {
                    doWork(params)
                }
            }
        } catch (e: TimeoutCancellationException) {
            StrataResult.Failure(StrataTimeoutException(timeout, e))
        }
    }

    private inline fun <T> withLoader(fromUser: Boolean, block: () -> T): T {
        addLoader(fromUser)
        try {
            return block()
        } finally {
            removeLoader(fromUser)
        }
    }

    private val P.isUserInitiated: Boolean
        get() = (this as? StrataUserInitiatedParams)?.isUserInitiated ?: true

    protected abstract suspend fun doWork(params: P): R

    public companion object {
        /** The timeout applied when neither the caller nor the subclass specifies one. */
        public val DefaultTimeout: Duration = 5.minutes
    }

    private data class State(val userCount: Int = 0, val ambientCount: Int = 0)
}

/**
 * Convenience overload for interactors that take no parameters.
 */
public suspend operator fun <R> StrataInteractor<Unit, R>.invoke(
    timeout: Duration = StrataInteractor.DefaultTimeout,
): StrataResult<R> = invoke(Unit, timeout)
