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

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with a [StrataException].
 *
 * This type is used to enforce strict exception handling boundaries within the Strata architecture.
 */
sealed interface StrataResult<out T> {
    /**
     * Represents a successful operation containing [data].
     */
    data class Success<T>(val data: T) : StrataResult<T>

    /**
     * Represents a failed operation containing an [error] of type [StrataException].
     */
    data class Failure(val error: StrataException) : StrataResult<Nothing>

    /**
     * Performs the given [action] on the encapsulated [StrataException] exception if this instance represents [Failure].
     * Returns the original `StrataResult` unchanged.
     */
    fun onFailure(action: (StrataException) -> Unit): StrataResult<T> {
        if (this is Failure) action(error)
        return this
    }

    /**
     * Performs the given [action] on the encapsulated value if this instance represents [Success].
     * Returns the original `StrataResult` unchanged.
     */
    fun onSuccess(action: (T) -> Unit): StrataResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Returns the encapsulated value if this instance represents [Success] or `null` if it is [Failure].
     */
    fun getOrNull(): T? = (this as? Success)?.data
}
