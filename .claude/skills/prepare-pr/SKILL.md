---
name: prepare-pr
description: Thorough PR preparation - deep review of changed implementation, conventions, test coverage, and improvement suggestions
disable-model-invocation: true
argument-hint: ""
---

# Prepare PR

Perform a thorough review of the current changes. Start from the diff, then follow the dependency graph to understand how the changes fit into the broader system. Check correctness, code quality, MESA conventions, test coverage, and suggest improvements.

**Scope:** $ARGUMENTS

---

## Step 1: Gather Changes

Get all changes on the branch compared to main:
- Run `git diff main` to capture all committed and uncommitted changes on the branch

Also run `git status` to identify new untracked files.

Read the diff output AND the full content of every changed file.

---

## Step 2: Follow the Dependency Graph

Starting from the changed files, trace connections to understand the full impact:

- Read related files within the same module (State, Event, Screen, StateHolder, UI, factories, interactors)
- Read interfaces being implemented or extended
- Read consumers of changed APIs — if a public interface, use case, or domain type was modified, find the modules that depend on it and check they are compatible
- Read `build.gradle.kts` files for dependency declarations when module boundaries are relevant

The goal is to understand how the changes integrate with everything that touches them — not to review the entire codebase, but to verify nothing is broken or inconsistent at the boundaries.

---

## Step 3: Correctness & Logic Review

Present findings with checkboxes as you go:

### Logic & Correctness
- [ ] No broken logic (incorrect conditionals, wrong operator, inverted checks)
- [ ] No off-by-one errors or boundary issues
- [ ] No race conditions or concurrency issues
- [ ] No null safety issues (unguarded `!!`, missing null checks)
- [ ] No unreachable code or dead branches
- [ ] Error paths handled correctly (not swallowed silently)
- [ ] State mutations are correct and complete (no partial updates that leave inconsistent state)
- [ ] Event handling covers all sealed interface cases (no missing `when` branches)
- [ ] Changed APIs are compatible with all consumers found in Step 2

### Best Practices
- [ ] No hardcoded values that should be constants or parameters
- [ ] No unnecessary object allocations in hot paths (Compose recomposition, loops)
- [ ] `remember` and `rememberSaveable` used correctly in Composables
- [ ] Coroutine scopes and dispatchers used appropriately
- [ ] No blocking calls on the main thread
- [ ] Resources cleaned up (no leaked coroutines, listeners, or streams)

### General Quality
- [ ] No obvious security concerns (hardcoded secrets, injection, insecure data handling) — for a thorough audit, run `/security-check`
- [ ] No TODO/FIXME left unaddressed without a tracking issue
- [ ] Code compiles without warnings where possible
- [ ] No unused imports or dead code introduced
- [ ] Naming is clear and consistent

---

## Step 4: Convention Review

Present findings with checkboxes as you go:

### MESA / Trapeze Conventions
- [ ] **UDF flow respected:** UI -> Event -> eventSink -> StateHolder -> State -> UI
- [ ] **Stateless UI:** Composables hold no business logic or persistent state
- [ ] **No ViewModels:** Logic is in `TrapezeStateHolder`, not Android ViewModels
- [ ] **Interfaces injected:** No concrete implementations injected directly
- [ ] **Lazy for heavy deps:** Heavy dependencies use `Lazy<T>`
- [ ] **Event safety:** Event sink wrapped with `wrapEventSink()` in StateHolder
- [ ] **Screen is pure key:** Screen type parameter `T` preserved but screen instance never stored in StateHolder
- [ ] **Factory pattern:** StateHolders created via `@AssistedInject` with factory; screen args extracted in factory, not in StateHolder
- [ ] **Navigation:** `TrapezeNavigator` used correctly; navigation results follow the `popWithResult` / `rememberNavigationResult` pattern
- [ ] **Messages:** Transient UI messages use `TrapezeMessageManager`, not side channels

### Strata Conventions
- [ ] Interactors return `StrataResult`, not raw exceptions
- [ ] `StrataSubjectInteractor` triggered in UI/Logic layer (e.g., `LaunchedEffect`), not in `init`
- [ ] `strataLaunch` used for coroutine work (defaults to `Dispatchers.Default`)
- [ ] `StrataResult` extensions used idiomatically (`onSuccess`, `onFailure`, `fold`, `map`, etc.)

### Module Structure & Clean Architecture
- [ ] `api/` modules have no internal dependencies
- [ ] `presentation/` depends on `api/` and `domain/`, not `data/`
- [ ] `domain/` depends on `api/` and `data/`, not `presentation/`
- [ ] Feature isolation maintained — no cross-feature `presentation/` imports
- [ ] Use cases / interactors are in `domain/` or `api/`
- [ ] Repository interfaces in `domain/` or `api/`, implementations in `data/`
- [ ] No Android framework imports in `domain/` or `api/` (except Parcelable for Screen)

### DI (Metro)
- [ ] `@ContributesBinding` used for interface bindings
- [ ] `@ContributesIntoSet` used for factory multibindings
- [ ] `@AssistedInject` / `@AssistedFactory` used correctly for runtime params
- [ ] No manual graph wiring that should be automated

### License
- [ ] All new source files have the Apache 2.0 license header with correct year

---

## Step 5: Test Coverage Analysis

### Unit Tests (`src/test/`)
- [ ] `{Name}StateHolder.kt` has `{Name}StateHolderTest.kt` (BehaviorSpec + Turbine)
- [ ] `{Name}Interactor.kt` / `{Name}UseCase.kt` has `{Name}Test.kt` (BehaviorSpec)
- [ ] `{Name}Repository.kt` has `{Name}Test.kt` (BehaviorSpec)
- [ ] All events and state transitions in changed code are covered
- [ ] Error paths and edge cases are tested
- [ ] Navigation flows are tested
- [ ] Fakes exist for new dependencies in `test/` subpackage

### UI Tests (`src/androidTest/`)
- [ ] `{Name}Ui.kt` has `{Name}UiTest.kt` (JUnit4 + robot pattern)
- [ ] `robot/{Name}UiRobot.kt` exists
- [ ] `testdata/{Name}UiTestData.kt` exists
- [ ] All UI elements and interactions in changed code are covered
- [ ] UI state variations are covered (empty, error, loading, populated)

Flag any gaps.

---

## Step 6: Improvement Suggestions

Beyond issues, suggest improvements that are directly relevant to the changed code:
- Code simplification opportunities
- Better use of MESA/Strata patterns
- Performance considerations (unnecessary recompositions, missing `remember`, etc.)
- Accessibility (content descriptions, semantic properties)
- Better error handling or user feedback

---

## Step 7: Report

Present a structured report:

### Summary
Brief overview of the changes and overall assessment (ready to merge, needs work, etc.).

### Checklist Results
Show all completed checklists from Steps 3-5 with pass/fail/not-applicable indicators.

### Blocking Issues
Must fix before merge. Include file path and line references.

### Warnings
Should fix. Style issues, potential problems, missing patterns.

### Missing Tests
For each file missing tests or test cases:
- File path
- What specific tests are needed (unit, UI, or both)
- Suggest running `/add-tests @<filepath>` to generate them

### Suggestions
Improvement opportunities. These are optional but recommended.
