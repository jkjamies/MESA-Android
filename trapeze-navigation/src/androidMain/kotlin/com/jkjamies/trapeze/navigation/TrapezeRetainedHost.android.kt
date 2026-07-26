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

package com.jkjamies.trapeze.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jkjamies.trapeze.TrapezeRetainedStore

/**
 * A [ViewModel] is the only thing on Android that reliably outlives Activity recreation, so it is
 * what the retained stores hang off. Trapeze does not ask feature authors to write ViewModels —
 * this one exists purely to own a lifetime, holds no logic, and is never seen by callers.
 */
internal class TrapezeRetainedViewModel : ViewModel(), TrapezeRetainedHost {
    private val delegate = MapRetainedHost()

    override fun storeFor(entryId: String): TrapezeRetainedStore = delegate.storeFor(entryId)

    override fun clear(entryId: String) {
        delegate.clear(entryId)
    }

    override fun onCleared() {
        // The host itself is going away (the Activity is finishing, not rotating), so every
        // retained scope still running is cancelled here.
        delegate.clearAll()
        super.onCleared()
    }
}

@Composable
public actual fun rememberTrapezeRetainedHost(): TrapezeRetainedHost =
    viewModel<TrapezeRetainedViewModel>()
