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

apply(plugin = "maven-publish")

val publishingGroup: String by project
val publishingArtifactId: String by project
val publishingVersion: String by project
val publishingName: String by project
val publishingDescription: String by project

val isAndroidLibrary = plugins.hasPlugin("com.android.library")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                groupId = publishingGroup
                artifactId = publishingArtifactId
                version = publishingVersion

                if (isAndroidLibrary) {
                    from(components["release"])
                } else {
                    from(components["java"])
                }

                pom {
                    name.set(publishingName)
                    description.set(publishingDescription)
                    url.set("https://github.com/jkjamies/MESA-Android")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("jkjamies")
                            name.set("Jason Jamieson")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/jkjamies/MESA-Android.git")
                        developerConnection.set("scm:git:ssh://github.com/jkjamies/MESA-Android.git")
                        url.set("https://github.com/jkjamies/MESA-Android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/jkjamies/MESA-Android")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                        ?: findProperty("gpr.user") as String? ?: ""
                    password = System.getenv("GITHUB_TOKEN")
                        ?: findProperty("gpr.token") as String? ?: ""
                }
            }
        }
    }
}
