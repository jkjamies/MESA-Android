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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
     * Work the user asked for is reported immediately. Work that is entirely ambient is held
     * back for [ambientLoadingDelay] so short background refreshes never flash a spinner. If a
     * user-initiated call starts while ambient work is pending, the indicator turns on at once.
     *
     * The delay is measured from the moment loading *became* ambient, not from the last change
     * to the number of in-flight calls. Overlapping background work therefore cannot keep
     * pushing the indicator further out: three staggered refreshes still surface after one
     * [ambientLoadingDelay], and a steady trickle of them surfaces rather than never appearing.
     */
    // `by lazy` so an `ambientLoadingDelay` override is visible: reading an open member from a
    // constructor initializer would see the base-class default.
    @OptIn(ExperimentalCoroutinesApi::class)
    public val inProgress: Flow<Boolean> by lazy {
        val ambientDelay = ambientLoadingDelay
        loadingState
            .map { state -> state.activity }
            // Collapsing to the three states that matter *before* switching is what anchors the
            // delay: going from one ambient call to two is not a change here, so the pending
            // timer below is left alone instead of being restarted.
            .distinctUntilChanged()
            .flatMapLatest { activity ->
                when (activity) {
                    Activity.Idle -> flowOf(false)
                    Activity.User -> flowOf(true)
                    Activity.Ambient -> flow {
                        delay(ambientDelay)
                        emit(true)
                    }
                }
            }
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
            // Clamped: an unbalanced release would otherwise drive a count negative and wedge
            // the interactor into never reporting idle again.
            if (fromUser) {
                it.copy(userCount = (it.userCount - 1).coerceAtLeast(0))
            } else {
                it.copy(ambientCount = (it.ambientCount - 1).coerceAtLeast(0))
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

    /** What the interactor is busy with, as far as a loading indicator is concerned. */
    private enum class Activity { Idle, User, Ambient }

    private data class State(val userCount: Int = 0, val ambientCount: Int = 0) {
        val activity: Activity
            get() = when {
                // User-initiated work wins: if the user is waiting on something, say so, even
                // when a background refresh happens to be running alongside it.
                userCount > 0 -> Activity.User
                ambientCount > 0 -> Activity.Ambient
                else -> Activity.Idle
            }
    }
}

/**
 * Convenience overload for interactors that take no parameters.
 *
 * Deliberately takes no `timeout` default. Supplying one here would hard-code
 * [StrataInteractor.DefaultTimeout] and silently override a subclass's own [defaultTimeout];
 * omitting it lets the member operator pick the instance's value. Kotlin cannot express
 * "fall back to the member default" from an extension, hence the two overloads.
 */
public suspend operator fun <R> StrataInteractor<Unit, R>.invoke(): StrataResult<R> = invoke(Unit)

/**
 * Convenience overload for parameterless interactors run with an explicit [timeout].
 */
public suspend fun <R> StrataInteractor<Unit, R>.invoke(
    timeout: Duration,
): StrataResult<R> = invoke(Unit, timeout)
