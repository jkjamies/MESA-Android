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
    `kotlin-dsl`
}

/**
 * The marker artifact Gradle publishes alongside every plugin. Resolving through it means the
 * version still comes from `libs.versions.toml` and nothing here has to name the implementation
 * artifact — which for AGP in particular has moved between releases.
 */
fun Provider<PluginDependency>.marker(): String = get().let {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    // On the classpath so the conventions below can apply these by id and configure their
    // extensions through the typed AGP and Kotlin Gradle plugin APIs.
    implementation(libs.plugins.kotlin.multiplatform.marker())
    implementation(libs.plugins.kotlin.android.marker())
    implementation(libs.plugins.kotlin.compose.marker())
    implementation(libs.plugins.android.library.marker())
    implementation(libs.plugins.android.application.marker())
    implementation(libs.plugins.jetbrains.compose.marker())
}

// Deliberately `Plugin<Project>` classes rather than precompiled `.gradle.kts` script plugins.
// Generating type-safe accessors for a precompiled script means applying its `plugins { }` block
// to a synthetic probe project, and that probe reads neither the root `gradle.properties` nor
// this build's own — so `android.builtInKotlin=false`, which AGP 9 needs here for
// `com.android.library` to coexist with the Kotlin Multiplatform plugin, is not in effect and
// AGP rejects `org.jetbrains.kotlin.android` during accessor generation. Classes are never
// probed. The cost is configuring extensions through typed APIs instead of `android { }`.
gradlePlugin {
    plugins {
        register("mesaKmpLibrary") {
            id = "mesa.kmp.library"
            implementationClass = "com.jkjamies.buildlogic.KmpLibraryConventionPlugin"
        }
        register("mesaKmpAndroidLibrary") {
            id = "mesa.kmp.android.library"
            implementationClass = "com.jkjamies.buildlogic.KmpAndroidLibraryConventionPlugin"
        }
        register("mesaAndroidLibrary") {
            id = "mesa.android.library"
            implementationClass = "com.jkjamies.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("mesaAndroidApplication") {
            id = "mesa.android.application"
            implementationClass = "com.jkjamies.buildlogic.AndroidApplicationConventionPlugin"
        }
    }
}
