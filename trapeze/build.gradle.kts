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

plugins {
    id("mesa.kmp.android.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // These are `api` because they leak into Trapeze's public API surface:
            //   - compose.runtime: `TrapezeStateHolder.produceState()` is `@Composable`.
            //   - compose.ui: the `TrapezeUi` typealias takes a `Modifier`.
            //   - coroutines: `wrapEventSink` exposes a `CoroutineScope` receiver.
            // Declaring them as `implementation` would keep them off the consumer's
            // compile classpath and make the published artifact unusable.
            api(compose.runtime)
            api(compose.ui)
            api(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.kotest.assertions.core)
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }
    }
}

android {
    namespace = "com.jkjamies.trapeze"
}
