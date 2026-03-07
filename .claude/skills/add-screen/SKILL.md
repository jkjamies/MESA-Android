---
name: add-screen
description: Add a new screen to an existing feature module with StateHolder, UI, factories, and tests
disable-model-invocation: true
argument-hint: "<feature-name> <ScreenName>"
---

# Add Screen

Add a new screen to an existing feature module. This scaffolds the Screen, State, Event, StateHolder, UI, and factory wiring within the feature's existing `presentation/` layer, plus the Screen definition in `api/`.

**Input:** $ARGUMENTS

---

## Step 1: Validate the Feature

Extract the feature name and screen name from the input.

Verify the feature module exists at `features/<feature-name>/` and has a `presentation/` layer. If the feature doesn't exist, suggest running `/add-feature` first. If the feature exists but has no `presentation/` layer, inform the user that this feature is headless and a presentation layer would need to be added first.

---

## Step 2: Read Existing Patterns

Read the existing screen files in the feature module to match:
- Package naming conventions
- Import patterns
- StateHolder constructor patterns (what dependencies are injected)
- Factory registration patterns
- How the existing `build.gradle.kts` is configured

Also read existing screens from other features if the current feature has no prior screens.

---

## Step 3: Determine Screen Details

Ask the user:
1. Does this screen receive navigation arguments? If so, what are they? (These become properties on the Screen data class and `@Assisted` params on the StateHolder)
2. Does this screen need to return a result when popped? If so, what result type? (Creates a `TrapezeNavigationResult` implementation)

---

## Step 4: Create Files

### Screen in `api/`

Place at: `features/<feature-name>/api/src/main/java/<package>/api/<ScreenName>Screen.kt`

```kotlin
@Parcelize
data class <ScreenName>Screen(
    // navigation arguments as properties
) : TrapezeScreen
```

If the screen returns a result, also create the result type:
```kotlin
@Parcelize
data class <ScreenName>Result(
    // result properties
) : TrapezeNavigationResult
```

### State in `presentation/`

Place at: `features/<feature-name>/presentation/src/main/java/<package>/presentation/<ScreenName>State.kt`

```kotlin
data class <ScreenName>State(
    // display properties
    val eventSink: (<ScreenName>Event) -> Unit
) : TrapezeState
```

### Event in `presentation/`

Place at: `features/<feature-name>/presentation/src/main/java/<package>/presentation/<ScreenName>Event.kt`

```kotlin
sealed interface <ScreenName>Event : TrapezeEvent {
    // user interactions
}
```

### StateHolder in `presentation/`

Place at: `features/<feature-name>/presentation/src/main/java/<package>/presentation/<ScreenName>StateHolder.kt`

```kotlin
class <ScreenName>StateHolder @AssistedInject constructor(
    // @Assisted params — navigation args extracted from screen by factory
    @Assisted private val navigator: TrapezeNavigator,
    // injected dependencies
) : TrapezeStateHolder<<ScreenName>Screen, <ScreenName>State, <ScreenName>Event>() {

    @Composable
    override fun produceState(): <ScreenName>State {
        val wrappedSink = wrapEventSink { event ->
            when (event) {
                // handle events
            }
        }

        return <ScreenName>State(
            eventSink = wrappedSink
        )
    }

    @AssistedFactory
    fun interface Factory {
        fun create(/* nav args, */ navigator: TrapezeNavigator): <ScreenName>StateHolder
    }
}
```

### UI in `presentation/`

Place at: `features/<feature-name>/presentation/src/main/java/<package>/presentation/<ScreenName>Ui.kt`

```kotlin
@Composable
fun <ScreenName>Ui(
    state: <ScreenName>State,
    modifier: Modifier = Modifier
) {
    // stateless composable
}
```

### Factories in `presentation/`

Place at: `features/<feature-name>/presentation/src/main/java/<package>/presentation/<ScreenName>Factories.kt`

```kotlin
@ContributesIntoSet(AppScope::class)
class <ScreenName>StateHolderFactory @Inject constructor(
    private val factory: <ScreenName>StateHolder.Factory
) : Trapeze.StateHolderFactory {
    override fun create(screen: TrapezeScreen, navigator: TrapezeNavigator?): TrapezeStateHolder<*, *, *>? {
        return if (screen is <ScreenName>Screen && navigator != null) {
            factory.create(/* extract screen args, */ navigator)
        } else null
    }
}

@ContributesIntoSet(AppScope::class)
class <ScreenName>UiFactory @Inject constructor() : Trapeze.UiFactory {
    override fun create(screen: TrapezeScreen): TrapezeUi<*>? {
        return if (screen is <ScreenName>Screen) ::<ScreenName>Ui else null
    }
}
```

---

## Step 5: Generate Tests

Automatically generate test files following the conventions from the `/add-tests` skill:

### Unit Test (`src/test/`)

`<ScreenName>StateHolderTest.kt` — BehaviorSpec + Turbine via `TrapezeStateHolder.test {}`:
- Test initial state
- Test each event
- Test navigation (if applicable)
- Test error handling (if applicable)

### UI Test (`src/androidTest/`)

Three files following the robot pattern:
- `<ScreenName>UiTest.kt` — JUnit4 test class using robot
- `robot/<ScreenName>UiRobot.kt` — UI interactions and assertions
- `testdata/<ScreenName>UiTestData.kt` — State factory methods

---

## Step 6: License Headers

All generated files MUST include the Apache 2.0 license header:

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

## Step 7: Verify

Run `./gradlew :features:<feature-name>:presentation:compileDebugKotlin` to verify compilation.

Report:
- Which files were created
- Screen navigation arguments (if any)
- Result type (if any)

Then ask:
- "Would you like to add interactors for this screen?" → suggest running `/add-interactor <feature-name> <InteractorName>`
