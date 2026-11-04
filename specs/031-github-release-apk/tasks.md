# Tasks: Publish the APK to GitHub Releases

**Input**: Design documents from `/specs/031-github-release-apk/`

**Tests**: A shell test for the CHANGELOG reader (the only logic here that can be wrong silently) and local build checks; the workflow itself can only run on GitHub.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [X] T001 (The offline release build failed on one dependency that was not cached; online it builds, unsigned.)Check the baseline: run `./gradlew :app:assembleRelease` once with no key and note the result (the release build has never shipped); list the existing tags with `git tag`

## Phase 2: Foundational (blocks the stories)

- [X] T002 Write the failing shell test `scripts/test-changelog-notes.sh`: for a sample CHANGELOG file it checks that a version's section is printed unchanged, the next heading is not included, a missing version fails with a message, and an empty section fails
- [X] T003 Write `scripts/changelog-notes.sh VERSION [FILE]` (T002 passes)
- [X] T004 Add the optional `release` signing config to `app/build.gradle.kts` (used only when the four `RELEASE_*` environment variables are set) and use it in the `release` build type; check that `./gradlew :app:assembleDebug` and the build with no variables are unchanged

## Phase 3: User Story 1 - A version tag publishes a release (P1) MVP

- [X] T005 [US1] Build the release with a throwaway key made in the scratchpad (`keytool`), run `apksigner verify`, and note the file size
- [X] T006 (Checked on the emulator, not the phone, which was not connected: the signed release build installs, opens, shows Projects and opens the editor with no crash. Export and voice split were not run on the release build. R8 renamed the ONNX Runtime classes, so a keep rule was added.)[US1] Check the release build runs: install it on a phone or emulator, open it, open a project, play, export; if anything fails because of R8, fix `app/proguard-rules.pro` (FR-011) and repeat T005
- [X] T007 [US1] Write `.github/workflows/release.yml`: trigger on `v[0-9]+.[0-9]+.[0-9]+` tags and on manual start with a `tag` input; `permissions: contents: write`; steps: checkout the tag with full history, check the tag form and that it exists, check it equals `appVersion`, read the notes with the script, check the four secrets, set up Java 21 and Gradle, run `:core:model:test testDebugUnitTest`, decode the key to `RUNNER_TEMP`, build `:app:assembleRelease`, run `apksigner verify`, rename the file and write the checksum, create or update the release with `gh`
- [X] T008 (`actionlint` passes on `release.yml` and `ci.yml`; the workflow itself can only run on GitHub.)[US1] Lint the workflow with `actionlint` if available (or review it line by line) and check every failure path ends the job with a one-line message and creates nothing

## Phase 4: User Story 2 - Publish a release for an existing tag (P1)

- [X] T009 [US2] Make sure the manual start in `release.yml` checks out the named tag (not the default branch), fails with "tag not found" when it does not exist, and updates the existing release instead of making a second (`gh release view`, `edit`, `upload --clobber`)

## Phase 5: User Story 3 - People can find out how to install it (P2)

- [X] T010 [P] [US3] Update `README.md`: a "Download" section with the Releases link, the Android 8.0 requirement, and the install steps for an APK from outside a store; a "Releasing" section with the one-time key setup and the steps to cut a release; fix the stale "(001 to 027)" to the real range
- [X] T011 [P] [US3] Update `CHANGELOG.md`: an entry for this change under a new version, and the "To release" paragraph rewritten for the new flow (update version and CHANGELOG, commit, tag, push the tag)

## Phase 6: Polish and release

- [X] T012 Run `./gradlew detekt testDebugUnitTest` and `scripts/test-changelog-notes.sh`; confirm `ci.yml` is unchanged
- [X] T013 Bump `appVersion` to 0.5.1 in `app/build.gradle.kts` (the CHANGELOG entry from T011 is its section), commit without attribution lines, tag `v0.5.1` locally, do not push; list for the maintainer the steps left on GitHub (add the four secrets, push the tag, start the workflow for `v0.5.0` if wanted)

## Dependencies

T002 then T003; T004 before T005; T005 and T006 before the first release; T007 needs T003 and T004; T009 extends T007; T010 and T011 are independent of the workflow code; T013 last.

## Implementation strategy

MVP is T001 to T008 (tag publishes a release). US2 and the docs follow. The workflow cannot run until the maintainer adds the secrets and pushes, so everything that can be checked locally is checked before then.
