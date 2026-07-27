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
            api(project(":trapeze"))
            api(libs.turbine)
            api(libs.kotlinx.coroutines.test)
            implementation(libs.molecule.runtime)
            // `api` because `TrapezeStateHolder.test { }` drives a `@Composable` produceState.
            api(compose.runtime)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
        }
    }
}

android {
    namespace = "com.jkjamies.trapeze.test"
}
