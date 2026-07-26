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

kotlin {
    jvmToolchain(17)
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
    // On the classpath so the convention scripts below can apply them by id, and so Gradle can
    // generate the `kotlin { }` / `android { }` accessors those scripts configure.
    implementation(libs.plugins.kotlin.multiplatform.marker())
    implementation(libs.plugins.kotlin.android.marker())
    implementation(libs.plugins.kotlin.compose.marker())
    implementation(libs.plugins.android.library.marker())
    implementation(libs.plugins.android.application.marker())
    implementation(libs.plugins.jetbrains.compose.marker())
}
