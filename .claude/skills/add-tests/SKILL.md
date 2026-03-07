---
name: add-tests
description: Add missing test cases, targeting a specific file or all changed files on the branch
disable-model-invocation: true
argument-hint: "[@<filepath>] [--unit] [--ui] [--both]"
---

# Add Tests

Add test coverage for a specific file or for all testable files changed on the branch. If test files already exist, analyze them for missing test cases and add them. If no test files exist, create them from scratch.

**Input:** $ARGUMENTS

---

## Step 1: Determine Targets

**If a file path is provided:** Use that file as the single target.

**If no file path is provided:** Run `git diff main --name-only` to get all changed files on the branch. Filter to testable source files (StateHolders, UI Composables, Interactors, UseCases, Repositories). Exclude test files, build files, and configuration. Process each testable file.

---

## Step 2: Analyze Each Target

For each target file, read it and understand:
- The component type (StateHolder, UI Composable, Interactor, UseCase, Repository, etc.)
- The State, Event, and Screen types involved
- Dependencies that need faking or mocking
- The module path (e.g., `features/counter/presentation`)
- The package name
- Every public method, event, state property, and code path

Determine which test types apply:
- `--unit` → JVM unit tests only
- `--ui` → Android instrumented UI tests only
- `--both` → Both
- No flag → Auto-detect based on component type:

| Component | Test Type | Location |
|-----------|-----------|----------|
| **StateHolder** | Unit (JVM) | `src/test/` |
| **UI Composable** | UI (androidTest) | `src/androidTest/` |
| **Interactor / UseCase** | Unit (JVM) | `src/test/` |
| **Repository** | Unit (JVM) | `src/test/` |

---

## Step 3: Check for Existing Tests

Search for existing test files:
- For `{Name}StateHolder.kt` → look for `{Name}StateHolderTest.kt` in `src/test/`
- For `{Name}Ui.kt` → look for `{Name}UiTest.kt` in `src/androidTest/`, plus `robot/{Name}UiRobot.kt` and `testdata/{Name}UiTestData.kt`
- For `{Name}Interactor.kt` / `{Name}UseCase.kt` → look for `{Name}Test.kt` in `src/test/`
- For `{Name}Repository.kt` → look for `{Name}Test.kt` in `src/test/`

Also check for existing Fake files in the `fakes/` subpackage.

**If test files exist:** Read them and compare against the implementation to identify:
- Events or state transitions not covered
- Code branches or error paths not tested
- New methods or behaviors added to the implementation but missing from tests
- Missing Fake files for new dependencies

**If test files do not exist:** Create them from scratch following the patterns below.

---

## Step 4: Directory Structure

### Unit Tests (`src/test/`)

Mirror the implementation package structure. Place fakes in a `fakes/` subpackage:

```
src/test/java/<package>/
  ├── {Name}Test.kt
  └── fakes/
      └── Fake{Dependency}.kt
```

### UI Tests (`src/androidTest/`)

Mirror the implementation package structure. Use `robot/` and `testdata/` subdirectories:

```
src/androidTest/java/<package>/
  ├── {Name}Test.kt
  ├── robot/
  │   └── {Name}Robot.kt
  └── testdata/
      └── {Name}TestData.kt
```

### Naming

- Test files: `{SubjectName}Test.kt` (e.g., `CounterStateHolderTest.kt`, `CounterUiTest.kt`)
- Robot files: `{SubjectName}Robot.kt` (e.g., `CounterUiRobot.kt`)
- Test data files: `{SubjectName}TestData.kt` (e.g., `CounterUiTestData.kt`)
- Fake files: `Fake{DependencyName}.kt` (e.g., `FakeAppInterop.kt`)

---

## Step 5: Generate / Update Unit Tests (JVM)

### Conventions

- **Framework:** Kotest `BehaviorSpec` with `coroutineTestScope = true`
- **Assertions:** Kotest matchers (`shouldBe`, `shouldBeInstanceOf`, `shouldBeNull`, `shouldNotBeNull`, `shouldContain`, etc.)
- **Async:** Turbine for Flow testing; `TrapezeStateHolder.test {}` extension for StateHolder tests
- **Navigation:** `FakeTrapezeNavigator` from `trapeze-test` library
- **Events:** `TestEventSink` from `trapeze-test` when testing event recording
- **Mocking preference:** Prefer hand-written Fakes over MockK mocks. Use MockK (`io.mockk:mockk`) only when faking would be impractical (e.g., complex interfaces with many methods, Android framework classes, or third-party library types)

### StateHolder Test Pattern

StateHolder tests use the `trapeze-test` library's `TrapezeStateHolder.test {}` extension which runs `produceState` in a headless Compose runtime via Molecule + Turbine. StateHolder tests are always JVM unit tests, never instrumented tests.

```kotlin
class {Name}StateHolderTest : BehaviorSpec({

    Given("a {Name}StateHolder with {initial condition}") {
        When("{action or event}") {
            Then("{expected outcome}") {
                val navigator = FakeTrapezeNavigator()
                val holder = {Name}StateHolder(/* assisted params, fakes, navigator */)
                holder.test {
                    val state = awaitItem()
                    // assert initial state

                    state.eventSink({Name}Event.SomeEvent)
                    awaitItem().someProperty shouldBe expectedValue
                }
            }
        }
    }
})
```

### Interactor / UseCase Test Pattern

```kotlin
class {Name}Test : BehaviorSpec({

    Given("a {Name} that succeeds") {
        When("invoked with params") {
            Then("it returns Success with the result") {
                val subject = {Name}Impl(/* faked dependencies */)
                val result = subject(params)
                result.shouldBeInstanceOf<StrataResult.Success<*>>()
                result.data shouldBe expectedValue
            }
        }
    }

    Given("a {Name} that fails") {
        When("invoked") {
            Then("it returns Failure") {
                // setup fake to fail, invoke, assert Failure
            }
        }
    }
})
```

### Fake Creation

When dependencies need faking, create Fake classes in a `fakes/` subpackage:

- Extend the abstract class or implement the interface directly
- Provide controllable state (e.g., `shouldFail: Boolean` constructor param, `MutableStateFlow` for observables, `mutableListOf` to record calls)
- If the same Fake is needed in both `src/test/` and `src/androidTest/`, create it in both source sets under their respective `fakes/` subpackages (they do not share code)

---

## Step 6: Generate / Update UI Tests (Android Instrumented)

### Conventions

- **Framework:** JUnit4 with `createComposeRule()`
- **Assertions:** Kotest matchers for value assertions, Compose testing APIs for UI assertions
- **Pattern:** Robot pattern — always split into three files (Test, Robot, TestData)
- **Mocking preference:** Same as unit tests — prefer Fakes, use MockK (`io.mockk:mockk-android` variant) only when faking is impractical

### TestData file (`testdata/{Name}TestData.kt`)

Provides pre-built State objects for tests. Each factory method accepts an `eventSink` parameter defaulting to `{}`:

```kotlin
object {Name}UiTestData {

    fun defaultState(
        eventSink: ({Name}Event) -> Unit = {}
    ) = {Name}State(
        // sensible default property values
        eventSink = eventSink
    )

    fun stateWith{Variation}(
        // override specific properties
        eventSink: ({Name}Event) -> Unit = {}
    ) = {Name}State(
        // variation-specific values
        eventSink = eventSink
    )
}
```

### Robot file (`robot/{Name}Robot.kt`)

Encapsulates UI interactions and assertions. Methods return `this` for chaining:

```kotlin
class {Name}UiRobot(private val composeTestRule: ComposeContentTestRule) {

    fun setContent(state: {Name}State) = apply {
        composeTestRule.setContent {
            {Name}Ui(state = state)
        }
    }

    // --- Assertions ---

    fun assertTextDisplayed(text: String) = apply {
        composeTestRule.onNodeWithText(text).assertExists()
    }

    fun assertTextNotDisplayed(text: String) = apply {
        composeTestRule.onNodeWithText(text).assertDoesNotExist()
    }

    // --- Actions ---

    fun clickButton(text: String) = apply {
        composeTestRule.onNodeWithText(text).performClick()
    }
}
```

### Test file (`{Name}UiTest.kt`)

Uses the robot and test data. Test method names follow the pattern `givenX_whenY_thenZ`:

```kotlin
class {Name}UiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val robot get() = {Name}UiRobot(composeTestRule)

    @Test
    fun givenA{Name}State_whenDisplayed_thenItShows{ExpectedContent}() {
        robot
            .setContent({Name}UiTestData.defaultState())
            .assertTextDisplayed("expected text")
    }

    @Test
    fun givenA{Name}State_when{Button}IsClicked_then{Event}IsEmitted() {
        var event: {Name}Event? = null
        robot
            .setContent({Name}UiTestData.defaultState(eventSink = { event = it }))
            .clickButton("Button Text")

        event shouldBe {Name}Event.SomeEvent
    }
}
```

### Updating Existing UI Tests

When a test file exists but is missing the robot pattern structure:
- Create the missing `robot/` and `testdata/` files
- Refactor existing inline test logic into the robot and test data classes
- Add any missing test cases for uncovered UI elements or events

---

## Step 7: License Header

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

## Step 8: Verify

After generating or updating tests, attempt to compile:
- Unit tests: `./gradlew :<module>:testDebugUnitTest`
- UI tests: Report the run command to the user: `./gradlew :<module>:connectedAndroidTest`

Fix any compilation issues before finishing.
