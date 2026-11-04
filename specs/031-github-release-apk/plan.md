# Implementation Plan: Publish the APK to GitHub Releases

**Branch**: `031-github-release-apk` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/031-github-release-apk/spec.md`

## Summary

Add a second GitHub Actions workflow, `release.yml`, that runs when a tag `vX.Y.Z` is pushed or when the maintainer starts it by hand for an existing tag. It checks the tag against `appVersion`, takes the version's section from `CHANGELOG.md`, runs the unit tests, builds the minimized release APK signed with a release key kept in repository secrets, and creates or updates the GitHub release with the APK, a SHA-256 file and the notes. `app/build.gradle.kts` gets a `release` signing config that is used only when the key settings are present (so local builds and CI are unchanged). The README and CHANGELOG are updated. Because the release build has never shipped, the plan also builds it, signs it with a throwaway key, and checks it starts and exports.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Gradle 9.8, AGP 9.5.0, JDK 21 on the runner

**Primary Dependencies**: GitHub Actions (`actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`), `gh` CLI on the runner, Android build-tools (`apksigner`), R8 with `proguard-rules.pro`

**Storage**: repository secrets for the key (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`); no key in the repository

**Testing**: a shell test for the changelog extraction script and the version check; a local release build signed with a throwaway key, checked with `apksigner verify`; install and run the release build on a phone or emulator; the workflow itself can only be exercised on GitHub after a push

**Target Platform**: GitHub Actions `ubuntu-latest`; the APK targets Android 8.0+ (minSdk 26)

**Project Type**: mobile-app with CI

**Performance Goals**: release visible within 30 minutes of the tag push (SC-001)

**Constraints**: the key must not appear in logs or the repository; pull requests must not get the key; the existing `ci.yml` stays as it is

**Scale/Scope**: one workflow file, one small script, a few lines in `app/build.gradle.kts`, README and CHANGELOG edits

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check |
|---|---|
| I. Native Android, Kotlin-first | Pass: no app code change. |
| II. Non-destructive editing | Pass: no project or data change. |
| III. Real-time preview performance | Pass with a check: the minimized release build is the one users get, so it is started, played and exported before the first release (FR-011). |
| IV. Incremental delivery | Pass: signing config, then the workflow, then docs; each is usable alone. |
| V. Test-first on core logic | Pass: the script that reads the CHANGELOG is tested before use; there is no engine logic here. |
| VI. Clean architecture | Pass: build logic stays in Gradle and the workflow; no app module changes. |
| VII. Privacy, safety, resources | Pass: the key is a secret, never logged, not available to pull requests; no user data involved. |
| VIII. Adaptive and foldable-first | Not applicable: no UI change. |

No violations; Complexity Tracking is empty. Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/031-github-release-apk/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md             # created by /speckit-tasks
```

### Source Code (repository root)

```text
.github/workflows/release.yml        # new: tag or manual start, checks, build, publish
scripts/changelog-notes.sh           # new: prints the CHANGELOG section of a version, fails if missing
scripts/test-changelog-notes.sh      # new: tests for the script
app/build.gradle.kts                 # release signing config used only when the key settings exist
app/proguard-rules.pro               # rules added only if the release check shows something removed that is needed
README.md                            # install from a release, how to release, one-time key setup
CHANGELOG.md                         # entry for this change, release steps updated
```

**Structure Decision**: one new workflow beside `ci.yml`, with the logic that is easy to get wrong (reading the CHANGELOG) in a small script that can be tested on a laptop.

## Complexity Tracking

No violations.
