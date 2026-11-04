# Feature Specification: Publish the APK to GitHub Releases

**Feature Branch**: `031-github-release-apk`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "The project is already compiled on GitHub now but the app is not showing in the release section. Let's add the wiring to add the released APK there. Update the README and CHANGELOG files accordingly."

## Scope note

Today the repository builds and tests on every push, and versions are marked with a git tag (`v0.5.0` and earlier), but nothing builds an installable app file from a tag, so the repository's Releases page is empty. This feature makes each version tag produce a GitHub release with the installable app file and its release notes, and lets the maintainer produce one for a tag that already exists. It also updates the README (how to install from a release, how to cut one) and the CHANGELOG (the entry for this change and the release steps). Not included: publishing to Google Play or any other store, automatic version numbering, and changing what the app does.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A version tag publishes a release with the app file (Priority: P1)

The maintainer finishes a version, updates the version and the CHANGELOG, tags the commit `vX.Y.Z`, and pushes the tag. A short time later the repository's Releases page shows a release named after the version, with the release notes from the CHANGELOG and an installable app file attached.

**Why this priority**: This is the whole request.

**Independent Test**: Push a new version tag and check the Releases page shows the release with its notes and one app file that installs on a phone.

**Acceptance Scenarios**:

1. **Given** a commit whose version is `0.5.1` and the tag `v0.5.1` is pushed, **When** the publishing finishes, **Then** a release `v0.5.1` exists with the notes of the `0.5.1` section of the CHANGELOG and the app file `PetruVideo-0.5.1.apk` attached.
2. **Given** the release exists, **When** a person downloads the file on an Android phone (8.0 or newer) and installs it, **Then** the app installs and opens.
3. **Given** the file is attached, **When** a person wants to check it, **Then** a checksum for the file is shown with the release.
4. **Given** the tag does not match the version in the app (the tag says `v0.6.0` and the app says `0.5.1`), **When** publishing runs, **Then** it stops with a message that names both versions and no release is made.
5. **Given** the CHANGELOG has no section for the tagged version, **When** publishing runs, **Then** it stops with a message saying so and no release is made.

---

### User Story 2 - Publish a release for a version that was already tagged (Priority: P1)

The maintainer has tags that were made before this feature (the latest is `v0.5.0`). They start the publishing by hand for one of those tags and get the same release a new tag would have.

**Why this priority**: The repository already has versions; without this the Releases page stays empty until the next version.

**Independent Test**: Start the publishing by hand for `v0.5.0` and check the release appears with the file and the notes of that version.

**Acceptance Scenarios**:

1. **Given** the tag `v0.5.0` exists and has no release, **When** the maintainer starts the publishing and names that tag, **Then** the release `v0.5.0` appears with its notes and file.
2. **Given** a release for that tag already exists, **When** the maintainer starts the publishing again, **Then** the existing release is updated (notes and file replaced) rather than a second one being made, or the run says it did nothing; it never ends with two releases for one tag.
3. **Given** the maintainer names a tag that does not exist, **When** the publishing runs, **Then** it stops with a message that the tag was not found.

---

### User Story 3 - People can find out how to install it (Priority: P2)

A person who finds the repository can see from the README where the latest release is, how to install the file on a phone, and which Android version is needed. The maintainer can see from the README and the CHANGELOG how to cut a release.

**Why this priority**: The file is only useful if people know where it is and how to use it.

**Independent Test**: Read the README and the CHANGELOG as a new person and follow the steps to install and to cut a release.

**Acceptance Scenarios**:

1. **Given** the README, **When** a person looks for how to get the app, **Then** it links to the Releases page, says which Android version is needed, and explains in a few steps how to install an APK that does not come from a store.
2. **Given** the README and the CHANGELOG, **When** the maintainer looks for how to release, **Then** the steps are listed: update the version and the CHANGELOG, tag, push the tag, check the release, and the one-time setup of the signing key.
3. **Given** the CHANGELOG, **When** it is read, **Then** it has an entry for this change under the version that ships it.

---

### Edge Cases

- The signing key is not set up yet: the run stops with a message that says which key setting is missing and how to add it; it never publishes a file that cannot be installed, and it never prints the key.
- The tests or checks fail on the tagged commit: no release is published for it.
- A pre-release or release-candidate tag (for example `v0.6.0-rc1`): not published as a normal release; it is out of scope and ignored.
- The tag is pushed twice or the run is repeated: only one release exists for the tag afterwards.
- Someone who is not the maintainer opens a pull request or pushes a branch: no release and no signing key is available to the run.
- The app file is minimized for release; a release build that does not start must not be published.
- The release notes contain text from the CHANGELOG as written, with no change to its words.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Pushing a tag of the form `vMAJOR.MINOR.PATCH` MUST produce a GitHub release named after the tag, with one installable app file named `PetruVideo-MAJOR.MINOR.PATCH.apk` and its SHA-256 checksum attached.
- **FR-002**: The release notes MUST be the section of `CHANGELOG.md` for that version, unchanged.
- **FR-003**: The app file MUST be the minimized release build of the tagged commit, signed with a release key, so it installs on Android 8.0 and newer and can be updated over the previous release.
- **FR-004**: The release signing key MUST be kept as repository secrets set by the maintainer once; it MUST NOT be stored in the repository or appear in any log, and runs started from pull requests or other people's branches MUST NOT have access to it.
- **FR-005**: Publishing MUST stop without creating a release, with a message that says why, when the tag does not match the app's version, when the CHANGELOG has no section for the version, when the signing key settings are missing, or when the build or the tests fail.
- **FR-006**: The maintainer MUST be able to start the publishing by hand for an existing tag, and the result MUST be the same as for a new tag.
- **FR-007**: Running the publishing again for a tag MUST leave exactly one release for it, with the newest file and notes.
- **FR-008**: The existing build and test run on every push MUST keep working as it does now.
- **FR-009**: The README MUST say where releases are, which Android version is needed, how to install an APK from outside a store, and how to cut a release including the one-time key setup.
- **FR-010**: The CHANGELOG MUST get an entry for this change and its release steps MUST be updated to the new flow.
- **FR-011**: The release build that is published MUST be checked to start and to export a video before the first release is published; where the minimizing removes something the app needs, it MUST be fixed or the rule added so the published app behaves like the debug build.

### Key Entities *(include if feature involves data)*

- **Release**: a GitHub release for one version tag: name, notes (from the CHANGELOG), the app file, and the checksum.
- **App file**: the signed, minimized APK of the tagged commit, named by version.
- **Signing key**: the key and passwords used to sign; held as repository secrets, never in the repository.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a version tag is pushed, the release with its file and notes is on the Releases page within 30 minutes in 100% of runs where the checks pass.
- **SC-002**: The file from a release installs and opens on a phone with Android 8.0 or newer, and installs over the previous release without removing the app first.
- **SC-003**: For every refusal case in FR-005, no release is created and the run's message names the cause in one line.
- **SC-004**: No log or file of any run contains the key or its passwords.
- **SC-005**: Running the publishing twice for the same tag leaves exactly one release.
- **SC-006**: A new person can go from the README to an installed app in under 5 minutes, and the maintainer can cut a release by following the README alone.

## Assumptions

- The repository is on GitHub (`petrubear/PetruVideo`) and the maintainer can add repository secrets; the one-time setup is creating a release key and adding it as secrets, and the steps go in the README.
- "Released APK" means an APK file that can be installed directly (sideloaded); an app bundle for a store is not needed now.
- Only tags in the plain `vMAJOR.MINOR.PATCH` form are published; release candidates are not.
- The release uses the version already in the app's build settings; the publishing never changes the version or the CHANGELOG.
- The first release to publish is `v0.5.0` through the manual start, since it was tagged before this feature; older tags may be published the same way if wanted.
- The release build has been compiled before but never shipped, so it needs the check in FR-011.
