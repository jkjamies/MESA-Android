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

package com.jkjamies.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A published multiplatform MESA library that also ships an Android artifact and uses Compose.
 *
 * Adds the Android target and the Compose plugins on top of [KmpLibraryConventionPlugin]. The
 * applying module still supplies its own `android { namespace = … }`, which is the one value
 * that cannot be shared.
 */
class KmpAndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        // Same order as the plugins blocks these conventions replace: Kotlin Multiplatform
        // first, then AGP, then Compose.
        pluginManager.apply(KmpLibraryConventionPlugin::class.java)
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                publishLibraryVariants("release")
            }
        }

        extensions.configure<LibraryExtension> {
            compileSdk = COMPILE_SDK

            defaultConfig {
                // Deliberately lower than the sample app's. A library's floor is a constraint it
                // imposes on every consumer, so it moves only when something requires it to.
                minSdk = MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        // Note the `buildTypes { release { proguardFiles(…) } }` block these modules used to
        // carry is gone rather than moved. A library is not minified by AGP — `isMinifyEnabled`
        // was `false`, and R8 runs in the consuming application — so those files were never
        // read. `consumerProguardFiles` above is the one that actually ships.
    }

    private companion object {
        const val COMPILE_SDK = 36
        const val MIN_SDK = 27
    }
}
