---
name: bump-version
description: Bump library versions in gradle.properties and prepare release notes
disable-model-invocation: true
argument-hint: "<module> <major|minor|patch> OR all <major|minor|patch>"
---

# Bump Version

Bump the version of one or more MESA library modules and prepare for release.

**Input:** $ARGUMENTS

---

## Step 1: Parse Input

Determine what to bump:
- `<module> <level>` → Bump a single module (e.g., `trapeze patch`, `strata minor`)
- `all <level>` → Bump all library modules

Valid modules: `trapeze`, `trapeze-navigation`, `strata`, `trapeze-test`, `mesa-bom`
Valid levels: `major`, `minor`, `patch`

If `mesa-bom` is not explicitly included but other modules are bumped, prompt whether the BOM version should also be bumped (it typically should for releases).

---

## Step 2: Read Current Versions

Read the `gradle.properties` file for each affected module:
- `trapeze/gradle.properties`
- `trapeze-navigation/gradle.properties`
- `strata/gradle.properties`
- `trapeze-test/gradle.properties`
- `mesa-bom/gradle.properties`

Extract the current `publishingVersion` value from each.

---

## Step 3: Calculate New Versions

Apply semantic versioning:
- `major` → increment major, reset minor and patch to 0
- `minor` → increment minor, reset patch to 0
- `patch` → increment patch

Display the version changes for confirmation:
```
Module             Current → New
trapeze            0.2.0   → 0.2.1
trapeze-navigation 0.2.0   → 0.2.1
mesa-bom           0.2.0   → 0.2.1
```

Ask the user to confirm before applying.

---

## Step 4: Apply Version Changes

Update the `publishingVersion` property in each affected `gradle.properties` file.

If the BOM was bumped, also verify that the BOM's dependency declarations reference the correct versions of the other modules.

---

## Step 5: Generate Release Notes

Gather commits since the last release tag:
- Run `git log $(git describe --tags --abbrev=0)..HEAD --oneline` to get commits since the last tag

Generate release notes grouped by category:
- **Features** — new functionality
- **Bug Fixes** — corrections
- **Dependencies** — version bumps (group Dependabot/Renovate PRs)
- **Internal** — refactoring, CI, docs

Format as markdown suitable for a GitHub release.

---

## Step 6: Summary

Report:
- Which modules were bumped and their new versions
- The expected release tag format: `v{BOM_VERSION}`
- The generated release notes
- Remind the user that publishing happens automatically via GitHub Actions when a release is created on the repository
