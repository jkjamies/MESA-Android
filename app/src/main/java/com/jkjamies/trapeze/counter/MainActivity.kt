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

package com.jkjamies.trapeze.counter

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.jkjamies.trapeze.Trapeze
import com.jkjamies.trapeze.TrapezeCompositionLocals
import com.jkjamies.trapeze.counter.theme.TrapezeTheme
import com.jkjamies.trapeze.features.counter.presentation.CounterScreen
import com.jkjamies.trapeze.navigation.NavigableTrapezeContent
import com.jkjamies.trapeze.navigation.rememberSaveableBackStack
import com.jkjamies.trapeze.navigation.rememberTrapezeNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey

@ContributesIntoMap(AppScope::class, binding<Activity>())
@ActivityKey(MainActivity::class)
class MainActivity : ComponentActivity() {

    @Inject lateinit var trapeze: Trapeze

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrapezeTheme {
                TrapezeCompositionLocals(trapeze) {
                    val backStack = rememberSaveableBackStack(root = CounterScreen(initialCount = 0))
                    val navigator = rememberTrapezeNavigator(backStack)

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavigableTrapezeContent(
                            navigator = navigator,
                            backStack = backStack,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}