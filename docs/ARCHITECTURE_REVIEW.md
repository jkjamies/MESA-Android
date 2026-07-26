# MESA Architecture Review

Review date: 2026-07-26 · Reviewed at `ba4946d` (main)

Scope: all five published modules (`trapeze`, `trapeze-navigation`, `strata`,
`trapeze-test`, `mesa-bom`), the sample app, the build/publishing setup, and CI.

The question driving this review is the one the project sets for itself: **can somebody
add these artifacts to an empty project and build a real app with them?** Findings are
ordered by how directly they block that.

> **Verification caveat.** This review was produced in an environment where Google's Maven
> repository is unreachable, so AGP and Compose could not be resolved and *nothing here was
> compiled or executed*. Findings are from source reading. The fixes committed alongside
> this document need a green CI run before they are trusted.

---

## Summary

| | Count |
|---|---|
| Blocks out-of-the-box use | 4 (3 fixed) |
| Correctness bugs and leaks | 9 (4 fixed) |
| Library-hygiene gaps | 11 |
| Security / privacy | 4 (1 fixed) |
| Missing capabilities | 8 |

The core idea is sound and the code is clean, small, and readable. What is missing is
almost entirely at the edges: **the published artifacts were not consumable, the
navigation layer had a Compose-contract violation at its centre, the test suite that
covers navigation never ran, and there is no retained scope** — which the "no ViewModels"
stance makes load-bearing rather than optional.

---

## 1. Blocks out-of-the-box use

### 1.1 Published artifacts could not be compiled against — **FIXED**

Every library declared its public-API dependencies as `implementation`:

```kotlin
// trapeze/build.gradle.kts (before)
commonMain.dependencies {
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(libs.kotlinx.coroutines.core)
}
```

`implementation` dependencies land in `runtimeElements` only — never on a consumer's
compile classpath. But those types are the public API:

| Declaration | Leaked type | Module |
|---|---|---|
| `TrapezeStateHolder.produceState()` | `@Composable` | compose.runtime |
| `typealias TrapezeUi<S>` | `Modifier` | compose.ui |
| `wrapEventSink` | `CoroutineScope` | coroutines |
| `NavigableTrapezeContent(navigator, backStack)` | `TrapezeNavigator`, `TrapezeScreen` | `:trapeze` |
| `StrataSubjectInteractor.flow`, `StrataInteractor.inProgress` | `Flow` | coroutines |
| `strataLaunch`, `strataLaunchWithResult` | `Job`, `Deferred` | coroutines |

A consumer following the README would get unresolved references until they re-declared
every transitive dependency by hand. `trapeze-navigation` was the worst case: it exports
`:trapeze` types from nearly every public function while hiding `:trapeze` entirely.

Fixed in `db2ff58`, including the sample modules with the same defect
(`core:presentation`, both `features/*/presentation`, `features/summary/domain`).

**Follow-up:** nothing stops this regressing. See §3.2 (binary-compatibility validation).

### 1.2 System back button does nothing — **NOT FIXED**

There is no `BackHandler`, no `onBackPressed` wiring, and no predictive-back support
anywhere in the repository:

```
$ grep -rn "BackHandler\|onBackPressed\|PredictiveBack" --include=*.kt .
NO MATCHES
```

In the sample app, navigating Counter → Summary and pressing back **exits the app**
instead of popping to Counter. For a library whose headline feature is a navigation
layer, this is the first thing a user will try and the first thing that will fail.

`rememberTrapezeNavigator` already takes an `onRootPop` callback, so the shape of the fix
is clear: `NavigableTrapezeContent` should install a `BackHandler(enabled = backStack.size > 1)`
that calls `navigator.pop()`, and the sample's `onRootPop` should finish the Activity.
Predictive back (`PredictiveBackHandler`) is the follow-on.

This needs an `expect/actual` — `BackHandler` is Android-only in Compose Multiplatform
(`androidx.activity.compose.BackHandler`), with no-op actuals elsewhere.

### 1.3 The navigation test suite never ran — **FIXED**

`trapeze-navigation` has 517 lines of tests, `trapeze` has another 148, and each feature
has a UI suite — all in `androidInstrumentedTest` / `androidTest`. CI ran only:

```yaml
- run: ./gradlew build
- run: ./gradlew test
```

Neither task runs instrumented tests, and there was no emulator job. So the module with
the subtlest Compose behaviour in the project — backstack save/restore, result delivery,
saveable-state cleanup — had **zero** executed coverage. (§1.4's bug sat in code whose
test asserted the opposite of what the code does.)

Fixed in `f4c53f7`: an emulator matrix on API 28 (the libraries' `minSdk`) and API 34,
with reports uploaded on failure. Expect the first run to surface pre-existing failures.

Note the second step was also redundant: for the KMP modules there is no `test` task
(it is `jvmTest`), and `build` → `check` already runs every unit-test task plus Lint.

### 1.4 GitHub Packages requires auth for public reads — **NOT FIXED**

`https://maven.pkg.github.com` requires a personal access token even for public packages,
and the README says so. That is a hard stop for "add this to your project": every
consumer must create a PAT and put credentials in `~/.gradle/gradle.properties` before a
single line compiles.

For a library intended to be used out of the box, **publishing to Maven Central** is the
single highest-leverage change available. It requires namespace verification, GPG
signing, and sources/javadoc jars — none of which are configured today.

---

## 2. Correctness bugs and leaks

### 2.1 Navigation results were consumed *during composition* — **FIXED**

```kotlin
// before
@Composable
public fun rememberNavigationResult(key: String, backStack: TrapezeBackStack = ...): TrapezeNavigationResult? {
    val result by remember { mutableStateOf<TrapezeNavigationResult?>(null) }  // never written — dead
    val consumed = backStack.consumeResult(key)                                 // snapshot WRITE in composition
    if (consumed != null) return consumed
    return result                                                               // always null
}
```

`consumeResult` reads `_results` and then writes it. Both happen inside composition, so
Compose invalidates the reading scope and immediately recomposes — on the second pass the
map is empty and the function returns `null`. The result was therefore visible for
exactly one composition pass, and only to whichever caller read it first.

The documented usage (`LaunchedEffect(editResult) { … }`) happened to work because the
effect launches on the pass where the value is non-null, but it is a race, not a design.
The `remember { mutableStateOf(null) }` was dead code that could never hold a value.

The instrumented test `givenAResult_whenRememberNavigationResultIsCalledTwice_thenItConsumesOnFirstRead`
asserts the value is still non-null after `runOnIdle` — i.e. it asserts the recomposition
does not happen. It has never run (§1.3).

Fixed in `d52b175`: consumption moved into a `LaunchedEffect` driven by `snapshotFlow`,
the delivered value latched in local state, and `NavigationResultEffect(key) { }` added
for the fire-once case. Tests updated to match.

### 2.2 Results stranded at the root — **FIXED**

```kotlin
override fun <R : TrapezeNavigationResult> popWithResult(key: String, result: R) {
    backStack.setResult(key, result)          // stored unconditionally
    if (backStack.size > 1) backStack.pop() else currentOnRootPop?.invoke()
}
```

Calling `popWithResult` at the root stored a result with no screen left to consume it. It
then lived in `_results` for the lifetime of the backstack **and was serialized into the
saved-state `Bundle` on every save**. Fixed: the result is stored only when the pop
succeeds.

### 2.3 The results map is unbounded and globally keyed — **NOT FIXED**

```kotlin
private var _results by mutableStateOf<Map<String, TrapezeNavigationResult>>(emptyMap())
```

Two independent problems:

- **Key collisions.** Keys are bare app-supplied strings in one flat namespace. Two
  features both using `"result"` will steal each other's data, and there is no way to
  detect it. Screen A can also consume a result that Screen C produced for Screen B.
- **No garbage collection.** A result is only removed when somebody consumes it with the
  matching key. A typo in the key, a screen that navigates onward instead of reading, or
  a `popToRoot()` that skips the intended consumer all leave the entry in the map forever
  — and inside the process-death `Bundle`.

The structural fix is to scope results to a backstack *entry* rather than a global map,
which depends on §2.4. A cheap interim mitigation is to drop results whose owning screen
is no longer on the stack when the stack shrinks.

### 2.4 Backstack entries have no identity — **NOT FIXED**

`TrapezeBackStack` is a `List<TrapezeScreen>`, and screens are value types. Consequences:

- `NavigableTrapezeContent` calls `saveableStateHolder.SaveableStateProvider(key = currentScreen)`.
  A stack of `Home → Detail → Home` gives both `Home` entries the **same saved-state key**,
  so the second one resumes the first one's scroll position, text-field contents, and any
  `rememberSaveable` in the StateHolder.
- `popTo(screen)` uses `lastIndexOf`, so with duplicates it is ambiguous by construction.
  The KDoc admits this ("pops to the most recent occurrence") rather than fixing it.
- Result scoping (§2.3) and per-entry retained scope (§2.6) both need entry identity.

Circuit solves this with a per-record `BackStack.Record` carrying a generated key. That is
the change to make here: wrap each pushed screen in a record with a stable unique id, key
saveable state and results off the record id, and keep the public `TrapezeScreen` API
unchanged.

### 2.5 Saveable state leaked on same-size stack changes — **FIXED**

The cleanup effect compared *sizes*:

```kotlin
if (currentScreens.size < previousScreens.size) { /* remove missing screens */ }
```

`snapshotFlow` conflates, so a push and a pop landing between two emissions leaves the
size unchanged and the popped screen's saved state is never released. The effect was also
keyed on `Unit`, so swapping backstacks would not restart tracking. Fixed: keyed on
`backStack`, diffed by set membership.

### 2.6 There is no retained scope — **NOT FIXED (largest architectural gap)**

MESA's stance is "No ViewModels: logic belongs in `TrapezeStateHolder`." But:

- `TrapezeContent` creates the StateHolder with `remember(screen)` — plain `remember`, so
  it is **destroyed and recreated on every configuration change**.
- `wrapEventSink` uses `rememberCoroutineScope()`, whose scope is cancelled when the
  composable leaves the composition.

So every in-flight operation started from an event sink is cancelled on rotation, on theme
change, on entering multi-window, and on navigating away. A "save" that takes 400ms and a
user who rotates mid-save silently loses the write. `rememberSaveable` covers *state* but
nothing covers *work*.

This is precisely the problem `ViewModel` exists to solve, and rejecting `ViewModel`
without replacing the capability leaves a hole users will hit on day one. Options, roughly
in order of preference:

1. A `TrapezeRetainedScope` keyed on the backstack entry id (§2.4), surviving config
   changes and cleared when the entry is popped — Circuit's `rememberRetained` model.
2. An opt-in `androidx.lifecycle.ViewModelStoreOwner`-backed scope per entry.
3. At minimum: document loudly that event-sink work does not survive, and give
   `strataLaunch` a documented escape hatch to an application-scoped coroutine scope.

Related sharp edge: `strataLaunch` does `check(!it.isCancelled)` and **throws
`IllegalStateException`** when the scope is already cancelled. `wrapEventSink` guards with
`coroutineScope.isActive`, but that check and the `launch` are not atomic — an event
dispatched exactly at disposal can crash on the main thread instead of being dropped.

### 2.7 Backstack restore silently discards entries — **NOT FIXED**

```kotlin
val stack = bundle.getParcelableArrayList<Parcelable>("stack")
    ?.filterIsInstance<TrapezeScreen>()
    ?.takeIf { it.isNotEmpty() }
    ?: return@Saver null
```

`filterIsInstance` drops anything that failed to restore — a renamed screen class, a
`Parcelable` whose `CREATOR` was stripped by R8 (§3.4) — **silently reordering and
shrinking the user's history** with no log and no signal. Restoring `A → B → C` as `A → C`
is worse than restoring nothing. Recommend logging each dropped entry and falling back to
the root, and using the non-deprecated `getParcelableArrayList(key, clazz)` on API 33+.

### 2.8 Factory resolution is unguarded — **NOT FIXED**

`Trapeze.stateHolder`/`ui` linearly scan every registered factory on every screen
resolution (O(features) per navigation) and the **first match wins silently** — a
behaviour the existing test enshrines. Two features accidentally claiming the same screen
type produces a wrong-screen bug with no diagnostic. Consider a debug-build assertion for
ambiguous matches, and caching resolution by screen class.

### 2.9 Smaller behavioural issues — **NOT FIXED**

- `TrapezeNavigator.popToRoot()` and `popTo()` have **default no-op / `false`
  implementations on the interface**. Any custom navigator silently does nothing for
  those. They should be abstract; the defaults exist only for source compatibility and
  hide bugs.
- `StrataSubjectInteractor.flow` applies `distinctUntilChanged()` to emitted values,
  silently swallowing legitimate repeat emissions (a refresh tick carrying an identical
  payload). Also there is no way to stop or reset a subscription once started.
- `StrataInteractor.inProgress` debounces on the *incoming* state's `ambientCount`, so a
  user-initiated load starting while an ambient load is running is debounced 5s — the
  opposite of the documented intent ("user-initiated calls update the indicator
  immediately").
- `AppGraph.trapeze` is an unscoped `@Provides get()`, so a new `Trapeze` registry is
  built per injection point. Harmless with one consumer, wrong in principle.
- `SummaryState.saveInProgress` is computed and never rendered — the sample advertises a
  loading state it does not show.
- **Verify:** `MainActivity` uses constructor injection via Metro's `AppComponentFactory`,
  but `AndroidManifest.xml` declares no `android:appComponentFactory` (only
  `tools:ignore="Instantiatable"`). If `metrox-android`'s manifest does not merge one in,
  the app crashes on launch. Could not be checked here — the dependency was unresolvable.

---

## 3. Library hygiene

### 3.1 No explicit API mode

Visibility modifiers are applied inconsistently — `TrapezeState`, `TrapezeEvent`,
`TrapezeInterop`, `TrapezeInteropEvent` and `TrapezeUi` have no `public` keyword while
everything around them does. For a published library, `explicitApi()` should enforce this
rather than review catching it. Enabling it requires a small audit (a handful of missing
modifiers, and an explicit return type on `StrataInteractor<Unit, R>.invoke`).

### 3.2 No binary-compatibility validation

Nothing prevents an accidental breaking change between 0.3.0 and 0.4.0. Add
`binary-compatibility-validator` with checked-in `.api` dumps and wire `apiCheck` into
`check`. This would also have caught §1.1 the moment it was introduced.

### 3.3 No lint, format, or license-header gate

`CLAUDE.md` mandates Apache headers on every source file; nothing enforces it. There is no
ktlint/detekt/spotless configuration. CI runs Android Lint only as a side effect of
`build`, with no baseline and no `lintOptions` config.

### 3.4 ProGuard/R8 consumer rules are empty

`trapeze/consumer-rules.pro` is a zero-byte file; `trapeze-navigation`'s has one comment.
Every module ships `isMinifyEnabled = false`, so **no minified build has ever been
exercised**. `TrapezeScreen` and `TrapezeNavigationResult` are `Parcelable` and rely on
reflective `CREATOR` access, which R8 strips without:

```proguard
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
```

A consumer shipping a minified release build — the normal case — will hit
`BadParcelableException` on process-death restore. This should be a consumer rule, and the
sample app should enable minification so CI exercises it.

### 3.5 Build configuration is copy-pasted and has drifted

Every module hand-repeats `compileSdk = 36`, `minSdk`, and the Java 17 block. They have
already diverged: **`minSdk` is 27 in the libraries and `features/counter`, but 28 in the
app and every `features/summary` module.** Extract a `build-logic` convention plugin
(`mesa.android.library`, `mesa.kmp.library`, `mesa.published`) and set these once.

### 3.6 Publishing setup

- `gradle/publishing.gradle.kts` is applied with `apply(from:)` + `afterEvaluate`, which is
  legacy-script territory and not configuration-cache friendly. Move into `build-logic`.
- `mesa-bom` reads sibling `gradle.properties` files with `java.util.Properties` at
  configuration time — this is not registered as a build input, so edits to a sibling
  version may not invalidate the task.
- No sources or javadoc jars for the non-KMP publications; no Dokka anywhere.
- No GPG signing (required for Maven Central, §1.4).
- `trapeze-test` is at 0.2.0 while everything else is 0.3.0, but the **BOM version drives
  the release tag** — so a `trapeze-test`-only fix cannot be released without bumping the
  BOM. Either align all versions or decouple the tag from the BOM.

### 3.7 Alpha/milestone toolchain for a published library

`agp = "9.2.0-alpha05"` and Gradle `9.5.0-milestone-5`. Alphas change metadata generation
and behaviour between builds; consumers on stable AGP may not be able to consume the
resulting module metadata. Pin published artifacts to stable releases, and keep the alpha
on a separate CI lane if you want early signal.

### 3.8 Three copies of the same guidance document

`CLAUDE.md`, `GEMINI.md`, and `.junie/guidelines.md` are ~500-line near-duplicates and
have already drifted — `.junie/guidelines.md` documented `./gradlew :strata:test`, a task
that does not exist (it is `jvmTest`). Keep one canonical document and make the others
one-line pointers. (Corrected in `f27e3b0`; consolidation still recommended.)

### 3.9 Duplicated test suites

`CounterStateHolderTest`, `SummaryStateHolderTest`, and all their fakes exist **twice** —
once in `src/test` (Kotest + Molecule) and once in `src/androidTest` (JUnit4 + compose
rule) — testing the same logic through different harnesses. Now that §1.3 makes the
instrumented suites actually run, this is duplicated maintenance and duplicated CI time.
Delete the instrumented StateHolder copies; keep the UI tests, which genuinely need a
device.

### 3.10 Coverage gaps

No tests exist for: `TrapezeContent`'s missing-factory path, `NavigableTrapezeContent`'s
saveable-state cleanup, `popTo` with duplicate equal screens, backstack restore from a
corrupt or partially-unparcelable bundle, `StrataInteractor.inProgress` debounce
semantics, or `strataLaunchWithResult` cancellation. There is no screenshot testing and no
Compose stability/strong-skipping verification.

### 3.11 Documentation errors (fixed)

The documented dependency rule was `domain → api, data`, but the code has `data → domain`
and `presentation → api` only. README also pinned `mesa-bom:0.2.0` in its install snippet
while shipping 0.3.0. Both corrected in `f27e3b0`.

---

## 4. Security and privacy

### 4.1 Exception detail is piped to the UI by default

```kotlin
public fun TrapezeMessage(t: Throwable, id: Uuid = Uuid.random()): TrapezeMessage =
    TrapezeMessage(message = t.message ?: "Error occurred: $t", id = id)
```

This is the framework's **documented** way to surface errors, and its default is the raw
exception message — with a fallback of `"$t"`, which includes the fully-qualified
exception class. Exception messages routinely carry request URLs, SQL fragments, file
paths, and occasionally credentials embedded in a URL. Making that the path of least
resistance means real apps will ship it.

Recommend: require an explicit user-facing string, keep the `Throwable` on the message for
logging, and make the throwable-only overload opt-in and clearly marked as
debug/diagnostic.

### 4.2 Sample defaults to backing up app data

`android:allowBackup="true"` with a stub `data_extraction_rules.xml` means the DataStore
file is included in cloud backup and device transfer. Harmless for a counter, but the
sample is what people copy. Either set `allowBackup="false"` or add worked `<exclude>`
examples.

### 4.3 Publish workflow permissions — **FIXED**

The publish job held `contents: write` while writing nothing to the repository; dropped to
`read`. The job also published without running a single test — a release can be cut from
any commit, and Maven artifacts are immutable once pushed. A `./gradlew build` verification
step now runs first (`f4c53f7`).

### 4.4 No artifact signing or dependency verification

Artifacts are published unsigned. There is no Gradle dependency verification
(checksums/signatures) for the build's own dependencies, on a toolchain that pulls alpha
plugins.

---

## 5. Missing capabilities for a complete architecture

Ranked by how likely a real app is to need them:

1. **Retained scope / config-change survival** (§2.6) — the "no ViewModels" claim is not
   yet backed by a replacement.
2. **System back and predictive back** (§1.2).
3. **Screen transition animations.** `NavigableTrapezeContent` swaps content with no
   `AnimatedContent` and no hook to supply one. Every navigation is a hard cut.
4. **Nested navigation / multiple backstacks** — bottom-nav tabs with independent history
   are not expressible.
5. **Overlays as first-class destinations** — dialogs, bottom sheets, and full-screen
   overlays currently have to be modelled as state inside a screen.
6. **Deep links / URL routing** — no mapping from a URI to a `TrapezeScreen`.
7. **A multiplatform sample.** Six targets are published; only an Android app exists. iOS,
   desktop, and wasm support is effectively untested end-to-end.
8. **A StateHolder lifecycle hook** — there is no `onDispose`/`onCleared` equivalent for
   releasing non-Compose resources.

---

## 6. Suggested PR sequence

Committed on this branch:

| # | Commit | Contents |
|---|---|---|
| 1 | `db2ff58` | `api` vs `implementation` across all published modules |
| 2 | `d52b175` | Navigation result consumption, root-pop leak, saveable cleanup, `TrapezeContent` keys |
| 3 | `f4c53f7` | Instrumented-test CI job, publish verification, workflow permissions |
| 4 | `f27e3b0` | Documentation corrections + changelog |
| 5 | `—` | Sample aligned with the documented `wrapEventSink` contract |

Recommended follow-ups, each a self-contained PR:

| # | Scope | Why this order |
|---|---|---|
| 6 | `BackHandler` + `onRootPop` wiring (§1.2) | Small, self-contained, most visible bug |
| 7 | Backstack entry identity (§2.4) | Unblocks 8 and 9 |
| 8 | Entry-scoped navigation results (§2.3) | Depends on 7 |
| 9 | Retained scope (§2.6) | Depends on 7; the largest design change |
| 10 | `explicitApi()` + binary-compatibility validator (§3.1, §3.2) | Locks in §1.1 |
| 11 | `build-logic` convention plugins (§3.5, §3.6) | Removes the `minSdk` drift |
| 12 | Consumer R8 rules + minified sample (§3.4) | Needs 11 |
| 13 | Screen transition animations (§5.3) | Independent |
| 14 | Maven Central publishing (§1.4) | Needs 10, 11; unblocks real adoption |

Items 6–9 are the ones standing between this and an architecture somebody can ship on.
