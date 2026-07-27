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
    id("mesa.android.application")
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jkjamies.mesa.counter"
    defaultConfig {
        applicationId = "com.jkjamies.mesa"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // On, so R8 actually runs. The sample is the only application in this repository,
            // which makes it the only place the libraries' consumer rules are ever exercised —
            // with this off, `trapeze/consumer-rules.pro` was a file nothing had ever read, and
            // a missing keep rule would have surfaced first in someone else's release build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":trapeze"))
    implementation(project(":trapeze-navigation"))
    implementation(project(":core:presentation"))
    implementation(project(":features:counter:presentation"))
    implementation(project(":features:summary:presentation"))
    implementation(project(":features:summary:api"))
    implementation(project(":features:summary:domain"))
    implementation(project(":features:summary:data"))
    implementation(libs.metrox.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}