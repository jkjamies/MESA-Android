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
 * Base class for all business logic units in Strata.
 *
 * Manages loading state, timeouts, and error handling via [StrataResult].
 */
abstract class StrataInteractor<in P, R> {
    private val loadingState = MutableStateFlow(State())

    @OptIn(FlowPreview::class)
    val inProgress: Flow<Boolean> by lazy {
        loadingState
            .debounce {
                if (it.ambientCount > 0) {
                    5.seconds
                } else {
                    0.seconds
                }
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
    suspend operator fun invoke(
        params: P,
        timeout: Duration = DefaultTimeout,
        userInitiated: Boolean = params.isUserInitiated,
    ): StrataResult<R> = strataRunCatching {
        addLoader(userInitiated)
        withTimeout(timeout) {
            doWork(params)
        }
    }.also {
        removeLoader(userInitiated)
    }

    private val P.isUserInitiated: Boolean
        get() = (this as? StrataUserInitiatedParams)?.isUserInitiated ?: true

    protected abstract suspend fun doWork(params: P): R

    companion object {
        internal val DefaultTimeout = 5.minutes
    }

    private data class State(val userCount: Int = 0, val ambientCount: Int = 0)
}

suspend operator fun <R> StrataInteractor<Unit, R>.invoke(
    timeout: Duration = StrataInteractor.DefaultTimeout,
) = invoke(Unit, timeout)
