---
name: review
description: Quick review of the current diff for bugs, logic errors, convention violations, and test gaps
disable-model-invocation: true
argument-hint: "[--staged | --uncommitted]"
---

# Review Changes

Quick feedback on the current diff. Focus on catching bugs, logic errors, and convention violations. Keep output concise — this is a development pulse check, not a pre-merge audit.

**Scope:** $ARGUMENTS

---

## Step 1: Gather the Diff

Determine which diff to review:
- `--staged` → `git diff --cached` (only staged changes)
- `--uncommitted` → `git diff HEAD` (only uncommitted changes)
- No flag → Default to `git diff main` (all changes on the branch vs main, committed and uncommitted)

Also run `git status` to identify new untracked files included in the changes.

Read the diff output AND the full content of each changed file so you can assess whether the changed code is correct in context.

---

## Step 2: Scan for Issues

Read each changed file and look for actual problems. Do NOT present an exhaustive checklist — only report issues you find. Look for:

- Broken logic (incorrect conditionals, wrong operator, inverted checks)
- Off-by-one errors or boundary issues
- Race conditions or concurrency issues
- Null safety issues (unguarded `!!`, missing null checks)
- Unreachable code or dead branches
- Swallowed errors
- Incomplete state mutations
- Missing `when` branches for sealed interfaces
- Unnecessary allocations in Compose recomposition paths
- Incorrect `remember` / `rememberSaveable` usage
- Blocking calls on the main thread
- Leaked coroutines, listeners, or streams

If no issues are found, say so briefly.

---

## Step 3: Convention Check

Verify MESA conventions are followed. Present as a concise pass/fail list — only include items relevant to the changed code:

- [ ] UDF flow: UI → Event → eventSink → StateHolder → State → UI
- [ ] Stateless UI: Composables hold no business logic
- [ ] No ViewModels: Logic in `TrapezeStateHolder`
- [ ] Interfaces injected, not concrete implementations
- [ ] Heavy dependencies use `Lazy<T>`
- [ ] Event sink wrapped with `wrapEventSink()`
- [ ] Screen instance never stored in StateHolder
- [ ] `@AssistedInject` with factory pattern for StateHolders
- [ ] Interactors return `StrataResult`
- [ ] `StrataSubjectInteractor` triggered in UI/Logic layer, not `init`
- [ ] `strataLaunch` used for coroutine work
- [ ] Module boundaries respected (`api/` clean, `presentation/` doesn't touch `data/`)
- [ ] New source files have Apache 2.0 license header

Skip items that don't apply to the changed files.

---

## Step 4: Test Gaps

For each changed or new source file, briefly note missing or incomplete test coverage. Keep it to one or two lines per gap — just identify what's missing:

- Missing test files (e.g., "No unit test for `FooStateHolder`")
- Uncovered new behavior (e.g., "New `Delete` event not tested in `BarStateHolderTest`")
- Missing fakes for new dependencies

Do not suggest running other skills or provide detailed instructions on how to write the tests.

---

## Step 5: Report

Keep the report compact:

### Summary
One or two sentences on what changed and overall quality.

### Issues
List problems found in Step 2, grouped by severity:
- **Blocking:** Must fix (bugs, broken logic, broken conventions)
- **Warning:** Should fix (potential problems, best practice violations)

If none, say "No issues found."

### Conventions
Show the pass/fail list from Step 3. Omit items marked N/A.

### Test Gaps
List gaps from Step 4. If coverage looks complete, say so.
