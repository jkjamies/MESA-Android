---
name: review
description: Review the current git diff for code quality, correctness, MESA conventions, and missing tests
disable-model-invocation: true
argument-hint: "[--staged | --uncommitted]"
---

# Review Changes

Review the current diff for correctness, code quality, convention violations, and missing test coverage. While this is diff-focused, read the full changed files to understand context and catch broken logic.

**Scope:** $ARGUMENTS

---

## Step 1: Gather the Diff

Determine which diff to review:
- `--staged` → `git diff --cached` (only staged changes)
- `--uncommitted` → `git diff HEAD` (only uncommitted changes)
- No flag → Default to `git diff main` (all changes on the branch vs main, committed and uncommitted)

Also run `git status` to identify new untracked files included in the changes.

Read the diff output AND the full content of each changed file so you can assess whether the changed code is correct in context (e.g., a new event handler that references state incorrectly, logic that doesn't match its intent, off-by-one errors, etc.).

---

## Step 2: Correctness & Logic Review

For each changed file, verify the implementation is sound. Present findings with checkboxes as you go:

### Logic & Correctness
- [ ] No broken logic (incorrect conditionals, wrong operator, inverted checks)
- [ ] No off-by-one errors or boundary issues
- [ ] No race conditions or concurrency issues
- [ ] No null safety issues (unguarded `!!`, missing null checks)
- [ ] No unreachable code or dead branches
- [ ] Error paths handled correctly (not swallowed silently)
- [ ] State mutations are correct and complete (no partial updates that leave inconsistent state)
- [ ] Event handling covers all sealed interface cases (no missing `when` branches)

### Best Practices
- [ ] No hardcoded values that should be constants or parameters
- [ ] No unnecessary object allocations in hot paths (Compose recomposition, loops)
- [ ] `remember` and `rememberSaveable` used correctly in Composables
- [ ] Coroutine scopes and dispatchers used appropriately
- [ ] No blocking calls on the main thread
- [ ] Resources cleaned up (no leaked coroutines, listeners, or streams)

### General Quality
- [ ] No obvious security concerns (hardcoded secrets, injection, insecure data handling) — for a thorough audit, run `/security-check`
- [ ] No TODO/FIXME left unaddressed
- [ ] No unused imports or dead code introduced
- [ ] Code is clear and readable

---

## Step 3: Convention Review

Present findings with checkboxes as you go:

### MESA / Trapeze Conventions
- [ ] UDF flow respected: UI -> Event -> eventSink -> StateHolder -> State -> UI
- [ ] Stateless UI: Composables hold no business logic or persistent state
- [ ] No ViewModels: Logic is in `TrapezeStateHolder`
- [ ] Interfaces injected, not concrete implementations
- [ ] Heavy dependencies use `Lazy<T>`
- [ ] Event sink wrapped with `wrapEventSink()` in StateHolder
- [ ] Screen instance never stored in StateHolder
- [ ] StateHolders use `@AssistedInject` with factory pattern

### Strata Conventions
- [ ] Interactors return `StrataResult`, not raw exceptions
- [ ] `StrataSubjectInteractor` triggered in UI/Logic layer, not in `init`
- [ ] `strataLaunch` used for coroutine work

### Module Boundaries
- [ ] `api/` modules have no internal dependencies
- [ ] `presentation/` does not depend on `data/`
- [ ] `domain/` does not depend on `presentation/`

### License
- [ ] New source files have the Apache 2.0 license header

---

## Step 4: Test Coverage Check

For each changed or new source file in the diff, check for both unit tests and UI tests:

### Unit Tests (`src/test/`)
- [ ] `{Name}StateHolder.kt` has `{Name}StateHolderTest.kt` (BehaviorSpec + Turbine)
- [ ] `{Name}Interactor.kt` / `{Name}UseCase.kt` has `{Name}Test.kt` (BehaviorSpec)
- [ ] `{Name}Repository.kt` has `{Name}Test.kt` (BehaviorSpec)
- [ ] Existing unit tests cover new behaviors introduced in the diff (new events, state transitions, error paths)
- [ ] Fakes exist for new dependencies in `test/` subpackage

### UI Tests (`src/androidTest/`)
- [ ] `{Name}Ui.kt` has `{Name}UiTest.kt` (JUnit4 + robot pattern)
- [ ] `robot/{Name}UiRobot.kt` exists
- [ ] `testdata/{Name}UiTestData.kt` exists
- [ ] Existing UI tests cover new UI elements or interactions introduced in the diff

Flag any gaps.

---

## Step 5: Report

### Summary
One or two sentences on what changed and overall quality.

### Checklist Results
Show the completed checklists from Steps 2-4 with pass/fail/not-applicable indicators.

### Issues
List actual issues found, by severity:
- **Blocking:** Must fix (bugs, broken logic, security, broken conventions)
- **Warning:** Should fix (style issues, potential problems, best practice violations)
- **Suggestion:** Nice to have

### Missing Tests
For each file missing tests or with uncovered new behavior:
- File path and what is missing (unit test, UI test, or specific test cases)
- Suggest running `/add-tests @<filepath>` to generate them

If no issues are found, say so.
