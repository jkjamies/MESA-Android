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
    alias(libs.plugins.kotlin.parcelize)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api` because these types appear in this module's public API:
            //   - :trapeze supplies `TrapezeNavigator`, `TrapezeScreen`, `TrapezeNavigationResult`.
            //   - compose.runtime/ui back the `@Composable` entry points and `Modifier` params.
            api(project(":trapeze"))
            api(compose.runtime)
            api(compose.ui)
        }
        androidMain.dependencies {
            // Supplies the BackHandler actual. Internal to this module, so `implementation`.
            implementation(libs.androidx.activity.compose)
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.kotest.assertions.core)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

android {
    namespace = "com.jkjamies.trapeze.navigation"
}
