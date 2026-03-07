---
name: add-interactor
description: Scaffold a new Strata interactor with interface, implementation, fake, and test
disable-model-invocation: true
argument-hint: "<feature-name> <InteractorName> [--observe]"
---

# Add Interactor

Scaffold a new Strata interactor for an existing feature module, including the public interface, implementation, fake, and test.

**Input:** $ARGUMENTS

---

## Step 1: Parse Input

Extract:
- **Feature name:** The feature module to add the interactor to (e.g., `summary`)
- **Interactor name:** The name of the interactor (e.g., `FetchUser`, `ObserveLastSavedValue`)
- **Type:**
  - No flag → `StrataInteractor` (one-shot async, returns `StrataResult<R>`)
  - `--observe` → `StrataSubjectInteractor` (stream, exposes `Flow<T>` via `.flow`)

Verify the feature module exists at `features/<feature-name>/`. If it doesn't exist, **stop and ask the user** whether they intended a different feature name or whether they need to create the feature first with `/add-feature`. Do not proceed until the feature exists.

---

## Step 2: Determine Parameters and Return Types

Ask the user:
1. What are the input parameters type? (e.g., `Int`, `String`, `Unit`)
2. What is the return/output type? (e.g., `User`, `List<Item>`, `Int?`)

---

## Step 3: Read Existing Module Structure

Read the feature module's existing files to understand:
- Package naming conventions
- Existing `build.gradle.kts` dependencies
- Any existing interactors to match style

---

## Step 4: Create Files

### Interface in `api/`

Place at: `features/<feature-name>/api/src/main/java/<package>/api/<InteractorName>.kt`

**StrataInteractor (one-shot):**
```kotlin
abstract class <InteractorName> : StrataInteractor<P, R>()
```

**StrataSubjectInteractor (observe):**
```kotlin
abstract class <InteractorName> : StrataSubjectInteractor<P, T>()
```

### Implementation in `domain/`

Place at: `features/<feature-name>/domain/src/main/java/<package>/domain/<InteractorName>Impl.kt`

**StrataInteractor (one-shot):**
```kotlin
@ContributesBinding(AppScope::class)
class <InteractorName>Impl @Inject constructor(
    // dependencies
) : <InteractorName>() {
    override suspend fun doWork(params: P): R {
        TODO("Implement")
    }
}
```

**StrataSubjectInteractor (observe):**
```kotlin
@ContributesBinding(AppScope::class)
class <InteractorName>Impl @Inject constructor(
    // dependencies
) : <InteractorName>() {
    override fun createObservable(params: P): Flow<T> {
        TODO("Implement")
    }
}
```

### Fake in test directories

Create the Fake in both `src/test/` and `src/androidTest/` test subpackages where test files exist for the feature (they do not share code).

Place at: `<test-source>/java/<package>/presentation/test/Fake<InteractorName>.kt` (or the appropriate module's test directory if consumed elsewhere)

**StrataInteractor Fake:**
```kotlin
class Fake<InteractorName>(
    private val shouldFail: Boolean = false
) : <InteractorName>() {
    val invocations = mutableListOf<P>()

    override suspend fun doWork(params: P): R {
        if (shouldFail) throw IllegalStateException("<InteractorName> failed for testing")
        invocations.add(params)
        TODO("Return test value")
    }
}
```

**StrataSubjectInteractor Fake:**
```kotlin
class Fake<InteractorName> : <InteractorName>() {
    val valueFlow = MutableStateFlow<T>(/* initial value */)

    override fun createObservable(params: P): Flow<T> = valueFlow
}
```

### Unit Test in `domain/`

Place at: `features/<feature-name>/domain/src/test/java/<package>/domain/<InteractorName>ImplTest.kt`

```kotlin
class <InteractorName>ImplTest : BehaviorSpec({

    Given("a <InteractorName> that succeeds") {
        When("invoked with params") {
            Then("it returns the expected result") {
                val subject = <InteractorName>Impl(/* faked dependencies */)
                val result = subject(params)
                result.shouldBeInstanceOf<StrataResult.Success<*>>()
                // assert result.data
            }
        }
    }

    Given("a <InteractorName> that fails") {
        When("invoked") {
            Then("it returns Failure") {
                // setup dependency to fail
                val subject = <InteractorName>Impl(/* faked dependencies */)
                val result = subject(params)
                result.shouldBeInstanceOf<StrataResult.Failure>()
            }
        }
    }
})
```

For `StrataSubjectInteractor`, test the flow output using Turbine:
```kotlin
class <InteractorName>ImplTest : BehaviorSpec({

    Given("a <InteractorName>") {
        When("observed") {
            Then("it emits the expected values") {
                val subject = <InteractorName>Impl(/* faked dependencies */)
                subject(params)
                subject.flow.test {
                    awaitItem() shouldBe expectedValue
                }
            }
        }
    }
})
```

---

## Step 5: License Headers

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

## Step 6: Verify

Run `./gradlew :<feature-module>:compileDebugKotlin` for the affected modules (`api`, `domain`) to verify compilation.

Report:
- Which files were created
- The interactor type (one-shot or observe)
- Parameter and return types
- Where the fake(s) were placed