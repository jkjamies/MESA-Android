---
name: add-feature
description: Scaffold a new feature module with the appropriate layers and MESA conventions
disable-model-invocation: true
argument-hint: "<feature-name> [--headless]"
---

# Add Feature

Scaffold a new feature module with the correct directory structure, build configuration, and DI wiring following MESA conventions.

**Input:** $ARGUMENTS

---

## Step 1: Determine Feature Name

Extract the feature name from the input (e.g., `my-feature`). This becomes:
- Directory: `features/<feature-name>/`
- Package segment: feature name converted to valid Kotlin package (e.g., `myfeature` or `my_feature`)
- Full package: `com.jkjamies.mesa.features.<package-segment>`

---

## Step 2: Determine Layers

**If `--headless` is provided:** Default to `api` + `domain` + `data` (no presentation). Ask the user to confirm, or if any of these should also be excluded.

**If no flags are provided:** Ask the user which layers this feature needs:

> Which layers does this feature need?
> 1. **Full feature** — `api` + `domain` + `data` + `presentation` (UI with StateHolder, Screen, Events)
> 2. **Headless library** — `api` + `domain` + `data` (no UI)
> 3. **Custom** — Let me pick individual layers
>
> For custom, which layers? (e.g., "api, domain")

If the user selects custom, also ask follow-up questions based on the selected layers:
- If `domain` is included: "Does this feature need Strata interactors?"
- If `data` is included: "Does this feature need a repository?"

---

## Step 3: Scaffold Directory Structure

Create the module directories and files based on the selected layers. **Do not scaffold presentation layer files directly** — that is handled by the `/add-screen` skill in Step 6.

### `api/` layer

```
features/<feature-name>/api/
  ├── build.gradle.kts
  └── src/main/java/<package>/api/
```

The `api` module defines the public contract: interfaces for use cases, the Screen type (if navigable), and any shared data types.

**build.gradle.kts** — Pure Kotlin/JVM or Android library with no internal dependencies.

### `domain/` layer

```
features/<feature-name>/domain/
  ├── build.gradle.kts
  └── src/main/java/<package>/domain/
```

The `domain` module contains business logic implementations. Depends on `api` and `data`.

**build.gradle.kts** — Depends on `:features:<feature-name>:api` and `:features:<feature-name>:data`.

### `data/` layer

```
features/<feature-name>/data/
  ├── build.gradle.kts
  └── src/main/java/<package>/data/
```

The `data` module contains repository implementations and data sources.

**build.gradle.kts** — Minimal dependencies. No dependency on `domain` or `presentation`.

### `presentation/` layer (directory and build config only)

If presentation is included, create only the module shell:

```
features/<feature-name>/presentation/
  ├── build.gradle.kts
  └── src/main/java/<package>/presentation/
```

**build.gradle.kts** — Depends on `:features:<feature-name>:api` and `:features:<feature-name>:domain`. Includes Compose dependencies.

The actual Screen, State, Event, StateHolder, UI, and Factories files are created by the `/add-screen` skill in Step 6.

---

## Step 4: Register the Module

Add the new module(s) to `settings.gradle.kts`:
```kotlin
include(":features:<feature-name>:api")
include(":features:<feature-name>:domain")    // if included
include(":features:<feature-name>:data")      // if included
include(":features:<feature-name>:presentation")  // if included
```

---

## Step 5: Build Configuration

Each `build.gradle.kts` should follow existing module patterns in the project. Read an existing feature module's build files (e.g., `features/counter/`) to match:
- Plugin declarations
- Android SDK versions
- Dependency declarations
- Publishing configuration (if applicable)

---

## Step 6: Delegate to Other Skills

After scaffolding the module structure (delegated skills handle their own license headers):

1. **If presentation is included:** Automatically run the `/add-screen` skill to scaffold the first screen. Offer the user two options for the screen name:
   - **Default:** `<FeatureName>Screen` (e.g., feature `settings` → `SettingsScreen`)
   - **Custom:** Let the user type their own name

2. **If domain is included and interactors are needed:** Repeat the following loop:
   - Ask: "Would you like to add an interactor? (yes/no)"
   - If yes:
     1. Offer two options for the name:
        - **Default:** `<FeatureName><Action>` based on common patterns (e.g., feature `settings` → `FetchSettings` or `ObserveSettings`)
        - **Custom:** Let the user type their own name
     2. Ask: "Is this a one-shot operation or an observable stream?"
        - **One-shot** → `StrataInteractor` (no flag)
        - **Observable** → `StrataSubjectInteractor` (`--observe`)
     3. Run `/add-interactor <feature-name> <InteractorName> [--observe]`
     4. Loop back and ask if they want to add another interactor
   - If no, proceed to the next step

---

## Step 7: License Headers

All source files generated directly by this skill (e.g., `build.gradle.kts`, placeholder source files) MUST include the Apache 2.0 license header. Files generated by delegated skills (`/add-screen`, `/add-interactor`) handle their own headers.

```kotlin
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
```

---

## Step 8: Verify

Run `./gradlew :<new-module>:compileDebugKotlin` for each created module to verify they compile.

Report:
- Which modules were created
- Which files were generated (including those from delegated skills)
