---
name: fix
description: Diagnose and fix an error from a build failure, stack trace, or error message
disable-model-invocation: true
argument-hint: "<error message, stack trace, or description>"
---

# Fix

Diagnose and fix the provided error. This could be a build error, runtime crash, test failure, or a description of unexpected behavior.

**Input:** $ARGUMENTS

---

## Step 1: Parse the Error

Analyze the input to determine:
- **Error type:** Build error, runtime crash (stack trace), test failure, lint warning, or described behavior
- **Location:** File path(s) and line number(s) referenced in the error
- **Root cause clues:** Exception type, error message, compiler error code

If the input is a description rather than an error output, search the codebase for the relevant code.

---

## Step 2: Locate the Source

Read the file(s) referenced in the error. If no file is referenced:
- Search for relevant class/function names from the error message
- Check recent changes (`git diff main`) that may have introduced the issue

Read enough surrounding context to understand the code's intent — not just the failing line.

---

## Step 3: Diagnose

Identify the root cause. Common categories:

### Build Errors
- Missing imports or dependencies
- Type mismatches or incorrect generics
- Unresolved references (renamed/moved symbols)
- Incompatible dependency versions
- Missing `@AssistedInject` / `@AssistedFactory` wiring
- Missing `@ContributesBinding` or `@ContributesIntoSet`

### Runtime Crashes
- NullPointerException from unguarded access
- ClassCastException from incorrect type assumptions
- IllegalStateException from incorrect lifecycle usage
- Coroutine cancellation issues
- Missing Parcelable implementations

### Test Failures
- Assertion mismatches (expected vs actual)
- Missing or incorrect Fakes/Mocks
- Turbine timeout (state not emitted)
- Compose test rule issues (missing `waitForIdle`, incorrect matchers)
- Flaky timing issues

### Logic Bugs
- Incorrect conditional or operator
- State not updated correctly
- Event not handled in `when` block
- Navigation called with wrong screen/args

---

## Step 4: Fix

Apply the minimal fix that resolves the root cause. Do not refactor surrounding code or make unrelated improvements.

If the fix requires changes across multiple files (e.g., a renamed interface), update all affected files.

---

## Step 5: Verify

After applying the fix:
- If it was a **build error:** Run `./gradlew :<module>:compileDebugKotlin` to verify it compiles
- If it was a **test failure:** Run the specific test to verify it passes
- If it was a **runtime crash or logic bug:** Verify the fix compiles and explain what changed

Report what was wrong and what was changed.
