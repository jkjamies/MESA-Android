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

/**
 * A published multiplatform MESA library.
 *
 * Owns everything that must be identical across the published modules: the target set, the
 * toolchain, the API guarantees, and the coordinates. A module applying this declares only what
 * is genuinely its own — extra targets, dependencies, and its Maven metadata in `gradle.properties`.
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

group = property("publishingGroup") as String
version = property("publishingVersion") as String

kotlin {
    jvmToolchain(17)

    // Every declaration a MESA library publishes must say `public` (or `internal`) out loud and
    // name its return type. Without it, a `public` that was never meant to be public is one
    // omitted keyword away, and in a library that is a compatibility promise made by accident.
    explicitApi()

    // The reference dumps in `{module}/api/` are the record of that promise. `checkKotlinAbi`
    // fails the build when the compiled ABI drifts from them; `./gradlew updateKotlinAbi`
    // re-records it, which makes every breaking change arrive as a reviewable diff.
    abiValidation {
        enabled.set(true)
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
}

// Kotest's BehaviorSpec runs on the JUnit Platform. Only `jvmTest` is named here: the Android
// unit-test tasks are JUnit4 by contract, and switching them wholesale would silently stop
// collecting any JUnit4 test a module adds later.
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

apply(from = rootProject.file("gradle/publishing.gradle.kts"))
