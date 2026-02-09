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
 * Base exception for all domain-level errors in the Strata architecture.
 *
 * Subclass this for specific business-rule failures.
 * Thrown [StrataException]s are captured as [StrataResult.Failure] without wrapping.
 */
public abstract class StrataException(message: String, cause: Throwable? = null) : Exception(message, cause)
