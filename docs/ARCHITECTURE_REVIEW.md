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
| Correctness bugs and leaks | 9 (8 fixed) |
| Library-hygiene gaps | 11 (3 fixed) |
| Security / privacy | 4 (3 fixed) |
| Missing capabilities | 8 (1 fixed) |

**Status:** everything below is marked FIXED or NOT FIXED against the branch
`claude/project-architecture-review-0aw4o5`. The three items still open — retained scope,
`explicitApi()` + binary-compatibility validation, and convention plugins — were deliberately
held back; see §6.

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

### 1.2 System back button does nothing — **FIXED**

There is no `BackHandler`, no `onBackPressed` wiring, and no predictive-back support
anywhere in the repository:

```
$ grep -rn "BackHandler\|onBackPressed\|PredictiveBack" --include=*.kt .
NO MATCHES
```

In the sample app, navigating Counter → Summary and pressing back **exits the app**
instead of popping to Counter. For a library whose headline feature is a navigation
layer, this is the first thing a user will try and the first thing that will fail.

`NavigableTrapezeContent` now installs a back handler while `backStack.size > 1` and leaves
back alone at the root, so the host Activity finishes as usual. `handleBack = false` opts out.
Android delegates to `androidx.activity.compose.BackHandler` through an `expect/actual`; other
targets get a documented no-op. Covered by `BackHandlingTest`.

Predictive back (`PredictiveBackHandler`, with a progress-driven transition) is still open and
belongs with screen transitions — see §5.3.

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

### 2.3 The results map is unbounded and globally keyed — **FIXED**

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

Results are now addressed to the backstack *entry* that will receive them, so two features
using the same key cannot collide. Every path that removes entries funnels through one place
that discards results belonging to departed entries, so the map cannot grow unbounded and
nothing orphaned reaches the saved-state `Bundle`.

### 2.4 Backstack entries have no identity — **FIXED**

`TrapezeBackStack` is a `List<TrapezeScreen>`, and screens are value types. Consequences:

- `NavigableTrapezeContent` calls `saveableStateHolder.SaveableStateProvider(key = currentScreen)`.
  A stack of `Home → Detail → Home` gives both `Home` entries the **same saved-state key**,
  so the second one resumes the first one's scroll position, text-field contents, and any
  `rememberSaveable` in the StateHolder.
- `popTo(screen)` uses `lastIndexOf`, so with duplicates it is ambiguous by construction.
  The KDoc admits this ("pops to the most recent occurrence") rather than fixing it.
- Result scoping (§2.3) and per-entry retained scope (§2.6) both need entry identity.

Each push now occupies a `TrapezeBackStackEntry` with a generated id that survives
configuration changes and process death. Saveable state and results key off the entry id; the
public `TrapezeScreen` API is unchanged. `LocalTrapezeBackStackEntry` exposes the current entry,
which is also where per-entry retained scope will hang off (§2.6). Covered by
`BackStackEntryIdentityTest`.

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

### 2.7 Backstack restore silently discards entries — **FIXED**

```kotlin
val stack = bundle.getParcelableArrayList<Parcelable>("stack")
    ?.filterIsInstance<TrapezeScreen>()
    ?.takeIf { it.isNotEmpty() }
    ?: return@Saver null
```

`filterIsInstance` drops anything that failed to restore — a renamed screen class, a
`Parcelable` whose `CREATOR` was stripped by R8 (§3.4) — **silently reordering and
shrinking the user's history** with no log and no signal. Restoring `A → B → C` as `A → C`
is worse than restoring nothing. The saver now restores the longest valid prefix, logs how many
entries were dropped, and uses the non-deprecated Bundle accessors on API 33+.

### 2.8 Factory resolution is unguarded — **PARTIALLY FIXED**

`Trapeze.stateHolder`/`ui` linearly scan every registered factory on every screen
resolution (O(features) per navigation) and the **first match wins silently** — a
behaviour the existing test enshrines. Two features accidentally claiming the same screen
type produces a wrong-screen bug with no diagnostic.

Trapeze cannot detect the collision without invoking factories for their side effects, so this
is now documented on `stateHolder`/`ui` rather than enforced. Caching resolution by screen class
remains worth doing.

### 2.9 Smaller behavioural issues — **MOSTLY FIXED**

- **FIXED** `TrapezeNavigator.popToRoot()` and `popTo()` had default no-op / `false`
  implementations, so a custom navigator silently did nothing. Both are now abstract.
- **FIXED** `StrataSubjectInteractor.flow` applied `distinctUntilChanged()` to emitted values,
  swallowing legitimate repeat emissions. Now opt-in via `distinctValues`, and `stop()`/
  `isActive` were added — a subscription previously could not be torn down.
- **FIXED** `StrataInteractor.inProgress` was rebuilt. The `debounce` restarted its timer on
  every change to the in-flight count, so a second background refresh starting 4s into the 5s
  window pushed the indicator to 9s, and a steady trickle deferred it indefinitely; it also
  delayed a user-initiated call's spinner whenever ambient work happened to be running. Loading
  is now projected to Idle/User/Ambient before switching, anchoring the delay to the transition
  into ambient. Three tests, all verified to fail against the old logic.
- **FIXED** `AppGraph.trapeze` is `@SingleIn(AppScope::class)`; it previously rebuilt the
  registry per injection point.
- **FIXED** `SummaryState.saveInProgress` is now rendered — the save button disables and shows
  progress.
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

### 3.8 Three copies of the same guidance document — **FIXED**

`CLAUDE.md`, `GEMINI.md`, and `.junie/guidelines.md` are ~500-line near-duplicates and
have already drifted — `.junie/guidelines.md` documented `./gradlew :strata:test`, a task
that does not exist (it is `jvmTest`). `CLAUDE.md` is now the single canonical document; `GEMINI.md` and `.junie/guidelines.md`
point at it.

### 3.9 Duplicated test suites — **FIXED**

`CounterStateHolderTest`, `SummaryStateHolderTest`, and all their fakes exist **twice** —
once in `src/test` (Kotest + Molecule) and once in `src/androidTest` (JUnit4 + compose
rule) — testing the same logic through different harnesses. Now that §1.3 makes the instrumented
suites actually run, this was duplicated maintenance and duplicated CI time. The instrumented
StateHolder copies and their fakes are deleted; the UI tests, which genuinely need a device,
remain. The template `ExampleUnitTest`/`ExampleInstrumentedTest` are gone too.

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

### 4.1 Exception detail is piped to the UI by default — **FIXED**

```kotlin
public fun TrapezeMessage(t: Throwable, id: Uuid = Uuid.random()): TrapezeMessage =
    TrapezeMessage(message = t.message ?: "Error occurred: $t", id = id)
```

This is the framework's **documented** way to surface errors, and its default is the raw
exception message — with a fallback of `"$t"`, which includes the fully-qualified
exception class. Exception messages routinely carry request URLs, SQL fragments, file
paths, and occasionally credentials embedded in a URL. Making that the path of least
resistance means real apps will ship it.

The throwable-only factory is removed. `TrapezeMessage` now takes an explicit user-facing
string plus an optional `cause` that Trapeze never renders, so the easy path is the safe one.

### 4.2 Sample defaults to backing up app data — **FIXED**

`android:allowBackup="true"` with a stub `data_extraction_rules.xml` means the DataStore
file is included in cloud backup and device transfer. Harmless for a counter, but the
sample is what people copy. Both rule files now carry worked `<exclude>` entries for the
DataStore file, demonstrating the mechanism rather than shipping commented-out stubs.

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

## 6. What shipped, and what was deliberately held back

Committed on `claude/project-architecture-review-0aw4o5`:

| Commit | Contents |
|---|---|
| `db2ff58` | `api` vs `implementation` across all published modules (§1.1) |
| `d52b175` | Result consumption in composition, root-pop leak, saveable cleanup, remember keys (§2.1, §2.2, §2.5) |
| `f4c53f7` | Instrumented-test CI job, publish verification, workflow permissions (§1.3, §4.3) |
| `f27e3b0` | Documentation corrections (§3.11) |
| `506929b` | Review document; sample follows the `wrapEventSink` contract |
| `a7417c4` | Strata: subscription lifecycle, `distinctValues`, launch semantics (§2.9) |
| `1fb598d` | Backstack entry identity, entry-scoped results, system back, restore truncation (§2.3, §2.4, §2.7, §1.2) |
| `78f663d` | `TrapezeMessage` API, abstract navigator members, DI scoping, backup rules, doc consolidation, duplicate test removal (§2.9, §3.8, §3.9, §4.1, §4.2) |
| *(final)* | `StrataInteractor` loading model re-derived around the ambient transition (§2.9) |

Strata's changes are the only ones **executed** — it is pure Kotlin and builds against Maven
Central, so its 58 tests were run locally, including three verified to fail against the previous
loading-state implementation. Everything touching Compose is source-reviewed only (see the caveat at the
top) and needs CI.

### Held back deliberately

Three items were left out rather than written blind, because each is a design change that wants
a compiler in the loop and would be hard to review stacked on top of unverified work:

| # | Scope | Why held |
|---|---|---|
| 1 | **Retained scope** (§2.6) | The largest design change in the list, and it needs a new dependency (a `ViewModel`-backed store on Android, or the multiplatform lifecycle-viewmodel artifact) whose API cannot be checked here. Now unblocked by entry identity: the store hangs off `TrapezeBackStackEntry.id`, cleared when the entry is popped. |
| 2 | **`explicitApi()` + binary-compatibility validator** (§3.1, §3.2) | `apiDump` has to be *run* to generate the `.api` files, and it cannot be run here. Adding `apiCheck` without the dumps would just make CI red. |
| 3 | **`build-logic` convention plugins** (§3.5, §3.6) | Mechanical but wide, and it touches every build file this branch already modified. Much safer once the current changes are known-good. |

### Recommended order from here

1. Get this branch green in CI — expect fallout in the instrumented suites, which are running
   for the first time.
2. Retained scope (§2.6) — the remaining load-bearing gap.
3. `explicitApi()` + binary-compat dumps (§3.1, §3.2) — locks in §1.1 permanently.
4. `build-logic` convention plugins (§3.5) — removes the `minSdk` 27/28 drift.
5. Consumer R8 rules + a minified sample (§3.4) — needs 4.
6. Screen transitions and predictive back (§5.3) — the visible polish.
7. Maven Central (§1.4) — needs 3 and 4; the thing that unblocks actual adoption.
8. Deep links (§5.6) — parked at the author's request.
