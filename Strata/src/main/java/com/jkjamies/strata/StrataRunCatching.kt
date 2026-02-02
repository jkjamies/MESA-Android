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

import kotlin.coroutines.cancellation.CancellationException

/**
 * Calls the specified function [block] and returns its encapsulated result as a [StrataResult].
 *
 * - If execution completes successfully, returns [StrataResult.Success].
 * - If execution throws [CancellationException], it is re-thrown (respecting structured concurrency).
 * - If execution throws a [StrataException], it is captured as [StrataResult.Failure].
 * - If execution throws any other [Throwable], it is wrapped in [StrataExecutionException] and captured as [StrataResult.Failure].
 */
inline fun <R> strataRunCatching(block: () -> R): StrataResult<R> {
    return try {
        StrataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: StrataException) {
        StrataResult.Failure(e)
    } catch (e: Throwable) {
        StrataResult.Failure(StrataExecutionException(e))
    }
}
