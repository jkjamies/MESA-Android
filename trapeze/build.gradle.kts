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

@file:OptIn(
    org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class,
    org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class,
)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

group = property("publishingGroup") as String
version = property("publishingVersion") as String

kotlin {
    jvmToolchain(17)

    // Every declaration this module publishes must say `public` (or `internal`) out loud and
    // name its return type. Without it, a `public` that was never meant to be public is one
    // omitted keyword away, and in a library that is a compatibility promise made by accident.
    explicitApi()

    // The reference dumps in `api/` are the record of that promise. `checkKotlinAbi` fails the
    // build when the compiled ABI drifts from them; `./gradlew updateKotlinAbi` re-records it,
    // which makes every breaking change show up as a reviewable diff rather than a surprise in
    // a consumer's build. Experimental only in the sense that the Gradle DSL may still move.
    abiValidation {
        enabled.set(true)
    }

    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64()
    macosX64()
    wasmJs {
        browser()
    }

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
    compileSdk = 36

    defaultConfig {
        minSdk = 27
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

apply(from = rootProject.file("gradle/publishing.gradle.kts"))
