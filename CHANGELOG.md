# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!-- Release entries will be automatically prepended below this line -->

## [Unreleased]

### Added
- **System back handling.** `NavigableTrapezeContent` pops the backstack on the platform back
  affordance while more than one screen is on the stack, and stays out of the way at the root.
  Opt out with `handleBack = false`. Previously nothing was wired to back at all — on Android
  the back button left the app regardless of how deep the stack was.
- **`TrapezeBackStackEntry`** — each push occupies an entry with a stable id that survives
  configuration changes and process death, exposed via `LocalTrapezeBackStackEntry`.
- `NavigationResultEffect(key) { }` for reacting to a navigation result exactly once.
- `StrataSubjectInteractor.stop()` and `isActive`; a subscription could previously be started
  but never torn down.
- `StrataInteractor.ambientLoadingDelay` and `defaultTimeout` are overridable per interactor.
- `TrapezeMessage.cause` carries the originating `Throwable` for logging.
- **`rememberRetainedCoroutineScope()`** — a scope held by Compose's `retain` and cancelled on
  retirement. It is the default behind `wrapEventSink`, so work started from an event sink survives
  an Android configuration change and is cancelled when the screen is permanently gone.
  `rememberSaveable` covered state; nothing covered in-flight work. Use Compose's `retain { }`
  directly for retained *values* — MESA does not wrap it.
- CI runs the instrumented test suites on an emulator (API 28 and 34). They had never been
  executed by any workflow.
- The publish workflow verifies the build before pushing artifacts to the registry.
- A `NOTICE` file carrying the project's copyright, the conventional home for any third-party
  notices a future dependency requires.

### Fixed
- **Published artifacts are now usable by external consumers.** Compose, coroutines and
  `:trapeze` were declared as `implementation` in every library, so they never reached a
  consumer's compile classpath even though their types are all over the public API. They are
  now `api` dependencies.
- **Navigation results are no longer consumed during composition.** `rememberNavigationResult`
  wrote to snapshot state it had just read, which invalidated the calling scope and made the
  result visible for a single, racy composition pass. Consumption now happens in an effect and
  the delivered value is latched.
- **Equal screens no longer share saveable UI state.** `Home → Detail → Home` keyed both `Home`
  entries identically, so the second visit resumed the first's scroll position, text fields, and
  any `rememberSaveable` a StateHolder owned. State is keyed by entry id.
- **Navigation results are scoped to a backstack entry.** Keys lived in one flat map, so two
  features both using `"result"` stole each other's data, and an unconsumed result was never
  collected — it persisted in the map and in the saved-state `Bundle` for the lifetime of the
  backstack. Results are addressed to an entry and discarded when that entry is popped.
- **Backstack restore no longer silently drops entries.** A screen that failed to unparcel was
  skipped, rewriting the user's history into something they never navigated. It now restores the
  longest valid prefix and logs what was dropped.
- **`StrataInteractor.inProgress` is rebuilt around when loading *became* ambient.** The old
  debounce restarted its timer on every change to the in-flight count, so a second background
  refresh starting 4s into the 5s window pushed the indicator out to 9s — and a steady trickle of
  overlapping refreshes deferred it indefinitely. It also debounced whenever any ambient work was
  running, delaying a user-initiated call's spinner by the full 5s. Loading is now projected to
  Idle/User/Ambient before switching, so the delay is anchored to the transition into ambient and
  user-initiated work reports immediately.
- `popWithResult` at the root no longer stores a result that no screen can consume.
- `NavigableTrapezeContent` releases saved state for popped entries even when a push and a pop
  land between two snapshot emissions (previously it only compared backstack size).
- `TrapezeContent` includes `trapeze` and `navigator` in its remember keys, and reports which
  factory is missing when a screen fails to resolve.
- `strataLaunch`/`strataLaunchWithResult` no longer throw `IllegalStateException` on an
  already-cancelled scope. An event arriving as the composition is torn down is a normal
  outcome; the returned `Job` is simply already cancelled.

### Changed
- **Breaking:** `TrapezeMessage(Throwable)` is removed. The user-facing string must be supplied
  explicitly and the throwable passed as `cause`. Deriving displayed text from
  `throwable.message` made leaking request URLs, query fragments, and file paths into the UI the
  path of least resistance.
- **Breaking:** `TrapezeNavigator.popToRoot()` and `popTo()` are abstract. Their no-op/`false`
  defaults meant a custom navigator silently did nothing.
- **Breaking:** `StrataSubjectInteractor` no longer applies `distinctUntilChanged` to emitted
  values by default; override `distinctValues` to restore it. Filtering unconditionally
  swallowed legitimate repeat emissions.
- **Behavioural:** `rememberNavigationResult` latches the delivered value instead of returning
  it once and then `null`. Code following the documented
  `LaunchedEffect(result) { result?.let { … } }` pattern is unaffected.
- `strataLaunch`/`strataLaunchWithResult` reject a `Job` passed in `context`, which would have
  detached the coroutine from the scope.
- `GEMINI.md` and `.junie/guidelines.md` now point at `CLAUDE.md` instead of duplicating it.

## [0.2.0] - 2026-03-01

### Added
- **Navigation Result Passing**: Screen B can now return data to Screen A when popping.
  - `TrapezeNavigationResult` marker interface (Parcelable) in `trapeze` module.
  - `TrapezeNavigator.popWithResult(key, result)` method for returning results on pop.
  - `rememberNavigationResult(key)` composable for consuming results in the receiving screen.
  - `LocalTrapezeBackStack` CompositionLocal for internal result access.
  - Results survive configuration changes and process death via `TrapezeBackStack` saver.
  - Results are single-consumption: consumed on first read, returning `null` thereafter.

### Fixed
- Suppressed false-positive `Instantiatable` lint error on `MainActivity` (uses Metro `AppComponentFactory`).
