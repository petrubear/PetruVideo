# Research: Publish the APK to GitHub Releases

## 1. Trigger

- **Decision**: `on: push: tags: ['v[0-9]+.[0-9]+.[0-9]+']` and `workflow_dispatch` with a `tag` input.
- **Rationale**: the project already marks versions with `vX.Y.Z` tags. The pattern excludes release candidates (`v0.6.0-rc1`). Manual start covers tags made before the workflow (v0.5.0 and older). Tag and manual runs are not available to pull requests from forks, and only people with write access can start a manual run.
- **Alternatives**: trigger on a GitHub release being created by hand: then the maintainer would still make the release by hand, which is the thing to remove.

## 2. Signing

- **Decision**: a `release` signing config in `app/build.gradle.kts` that is created only when `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` are set (environment variables); the workflow decodes the base64 secret to a file in `RUNNER_TEMP` and sets them. Without them the release build type stays unsigned as today.
- **Rationale**: sideloaded APKs must be signed to install, and the same key must be used for every version so updates install over each other. Environment variables keep the key out of files and out of the repository, and local builds are unchanged.
- **Alternatives**: sign with the debug key (a new key on each runner, so updates would fail and the key is not owned by the maintainer); use a service that holds the key (more setup, nothing gained here).

## 3. Checks before publishing

- **Decision**: the workflow fails early, in this order: tag form, tag exists (manual start), tag equals `appVersion`, CHANGELOG section exists, the four secrets exist, unit tests pass.
- **Rationale**: the cheap checks come first so a mistake costs seconds, and each failure message names its cause (FR-005, SC-003).
- **Alternatives**: run the full CI (screenshot goldens, lint) again: the tag's commit already went through `ci.yml` on push; repeating the heavy checks only slows the release.

## 4. Release notes

- **Decision**: `scripts/changelog-notes.sh X.Y.Z` prints the lines under `## [X.Y.Z]` up to the next `## [` heading and fails when the section is missing or empty.
- **Rationale**: a small script is testable without GitHub and keeps the words of the CHANGELOG unchanged (FR-002).
- **Alternatives**: a marketplace action to parse the CHANGELOG: an extra dependency for ten lines of shell.

## 5. Creating or updating the release

- **Decision**: `gh release view TAG` and then `gh release edit` plus `gh release upload --clobber` when it exists, or `gh release create TAG --verify-tag --title TAG --notes-file` when not; `permissions: contents: write`.
- **Rationale**: one release per tag however many times it runs (FR-007, SC-005); `gh` is on the runner and uses the built-in token.
- **Alternatives**: a third-party release action: more surface for a one-command job.

## 6. APK name and checksum

- **Decision**: copy `app/build/outputs/apk/release/app-release.apk` to `PetruVideo-X.Y.Z.apk`, write `PetruVideo-X.Y.Z.apk.sha256`, attach both, and run `apksigner verify` before attaching.
- **Rationale**: a versioned file name is what a person sees after downloading; the checksum allows checking the download (FR-001).

## 7. The minimized release build

- **Decision**: build `assembleRelease` (R8 and resource shrinking are already on) locally with a throwaway key, install and run it, play a project and export; add keep rules only for what the check shows is removed.
- **Rationale**: the release build has never been shipped; Hilt, Room, Media3 and ONNX Runtime use reflection or native code that R8 can break (FR-011).
- **Alternatives**: turn minifying off for releases: a larger file and no protection, and it hides the problem instead of fixing it; kept as a fallback if a rule cannot be found.
