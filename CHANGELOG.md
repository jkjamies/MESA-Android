# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!-- Release entries will be automatically prepended below this line -->

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
