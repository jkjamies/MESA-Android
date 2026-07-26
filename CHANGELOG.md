# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!-- Release entries will be automatically prepended below this line -->

## [Unreleased]

### Fixed
- **Published artifacts are now usable by external consumers.** Compose, coroutines and
  `:trapeze` were declared as `implementation` in every library, so they never reached a
  consumer's compile classpath even though their types are all over the public API. They
  are now `api` dependencies.
- **Navigation results are no longer consumed during composition.** `rememberNavigationResult`
  wrote to snapshot state it had just read, which invalidated the calling scope and made the
  result visible for a single, racy composition pass. Consumption now happens in an effect,
  and the delivered value is latched.
- `popWithResult` at the root no longer stores a result that no screen can consume.
- `NavigableTrapezeContent` releases saved state for popped screens even when a push and a
  pop land between two snapshot emissions (previously it only compared backstack size).
- `TrapezeContent` includes `trapeze` and `navigator` in its remember keys, and reports which
  factory is missing when a screen fails to resolve.

### Added
- `NavigationResultEffect(key) { }` for reacting to a navigation result exactly once.
- CI now runs the instrumented test suites on an emulator (API 28 and 34). They had never
  been executed by any workflow.
- The publish workflow verifies the build before pushing artifacts to the registry.

### Changed
- **Behavioral**: `rememberNavigationResult` now latches the delivered value instead of
  returning it once and then `null`. Code following the documented
  `LaunchedEffect(result) { result?.let { … } }` pattern is unaffected.

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
