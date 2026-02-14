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
    alias(libs.plugins.android.library)

    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.jkjamies.trapeze.features.summary.presentation"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":trapeze"))
    implementation(project(":trapeze-navigation"))
    implementation(project(":core:presentation"))
    api(project(":features:summary:api"))
    implementation(project(":strata"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotest.assertions.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
