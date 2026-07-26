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

package com.jkjamies.trapeze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that survives Android configuration changes and is cancelled when the
 * calling content is permanently removed from the composition.
 *
 * `rememberCoroutineScope()` is cancelled whenever the composition goes away, which includes a
 * rotation — so a save started from an event sink was lost if the user turned the device. This
 * scope is held by Compose's `retain`, whose retention boundary is "permanently removed", not
 * "temporarily recomposed elsewhere".
 *
 * The scope inherits the composition's [CoroutineContext] — its dispatcher, and under a headless
 * test runtime its `TestCoroutineScheduler` — but drops the composition's [Job], which is what
 * decouples its lifetime without changing which thread work runs on.
 *
 * This is the default scope behind [TrapezeStateHolder.wrapEventSink].
 */
@Composable
public fun rememberRetainedCoroutineScope(): CoroutineScope {
    val compositionContext = rememberCoroutineScope().coroutineContext
    return retain { RetainedCoroutineScopeHolder(compositionContext) }.scope
}

/**
 * Owns the scope so retirement has something to hook onto.
 *
 * This class is the project's only contact with `androidx.compose.runtime.retain`, which is
 * experimental and incubating in Compose 1.10. Keeping it to one small type means an API change
 * upstream is a single-file fix, and — because nothing here appears in Trapeze's public API —
 * consumers never inherit an opt-in requirement.
 *
 * `RetainObserver` has five members: [onRetained], [onEnteredComposition], [onExitedComposition],
 * [onRetired] and [onUnused]. Only the last two release anything.
 *
 * The artifact reaches the compile classpath transitively through `compose.ui`, which `:trapeze`
 * exports. There is no JetBrains-published `runtime-retain`, so declaring it explicitly would mean
 * pinning an androidx coordinate against every KMP target by hand.
 */
private class RetainedCoroutineScopeHolder(compositionContext: CoroutineContext) : RetainObserver {

    // SupervisorJob so one failed event handler does not take down unrelated work from the
    // same screen. minusKey(Job) detaches from the composition's lifetime while keeping its
    // dispatcher.
    val scope: CoroutineScope =
        CoroutineScope(compositionContext.minusKey(Job) + SupervisorJob())

    override fun onRetained() {
        // Nothing to start: the scope is created eagerly and stays idle until something launches.
    }

    override fun onEnteredComposition() {
    }

    override fun onExitedComposition() {
        // Deliberately *not* cancelling here. Leaving the composition is exactly the transient
        // case this scope exists to survive; only retirement is permanent.
    }

    override fun onRetired() {
        scope.cancel()
    }

    override fun onUnused() {
        // Created but never actually retained — the composition that requested it was abandoned.
        // Nothing has launched yet, but the Job is real, so release it rather than leave it
        // dangling.
        scope.cancel()
    }
}
