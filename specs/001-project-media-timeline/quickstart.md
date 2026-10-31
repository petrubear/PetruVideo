# Quickstart: Validating Project Creation, Media Import, and Basic Timeline with Trim

A validation guide for this feature. Gradle commands apply once the scaffolding task has created the project; until then the scenarios define what to verify. Details live in [data-model.md](data-model.md) and [contracts/](contracts/); this file does not repeat them.

## Prerequisites

- Android Studio (current stable) with JDK 17 and the Android SDK.
- Emulator profiles: phone (compact), a resizable device, and a foldable (inner display with fold posture controls). One physical mid-range phone for performance checks; a physical foldable if available.
- A set of test media on the device or emulator: three videos with sound (one portrait, one 4K, one with rotation metadata), one video without sound, two photos, two audio files, and one corrupted file.
- The designer's UI Reference canvas open for visual comparison: https://claude.ai/artifact/D8dMeHZE4XUAvNj4wERyNo

## Automated checks

```bash
./gradlew :core:model:test
```
Engine unit and property tests: all commands, history, invariants, format derivation (SC-006).

```bash
./gradlew :core:data:connectedDebugAndroidTest
```
Room and migration tests, autosave and recovery (SC-005, SC-007).

```bash
./gradlew testDebugUnitTest
```
View model and arrangement-function tests, including tabletop and hinge avoidance (SC-009, SC-010).

```bash
./gradlew connectedDebugAndroidTest
```
Compose UI and end-to-end flow tests on the phone and foldable emulators.

```bash
./gradlew verifyScreenshots
```
Design conformance: each designed screen and layout compared with the reference images (SC-008). The task name depends on the screenshot tool chosen at scaffolding.

## Manual scenarios

1. **First project (Story 1, SC-001)**: Open the app, tap New project, add a video from Add media, and see it on the timeline. Target: under 60 seconds.
2. **Multi-import in order (Story 2)**: Select five mixed items in a chosen order; confirm numbered badges, tray count and total; tap "Add to timeline in order"; confirm videos and photos appear in that order and audio-only files land on the audio lane.
3. **Bad file and permissions (Story 2)**: Include the corrupted file; confirm a per-file message and that the rest imports. Deny media access; confirm the explanation and the system picker path.
4. **Timeline and playback (Story 3, SC-003)**: Scrub, play across clip boundaries, drag a clip to a new position, change a photo duration. Confirm 30 fps feel on a mid-range device with 1080p footage.
5. **Trim and split (Story 4, SC-002)**: Trim start and end, restore by dragging back, split at the playhead, duplicate, delete; undo and redo each; confirm the minimum length stops at 100 ms. Confirm the original file is unchanged (checksum).
6. **Detach and replace audio (Story 4)**: Select a video with sound, tap Detach; confirm the video is silent and an audio clip appears under it; trim, move, mute, and replace it; confirm the video clip is unchanged.
7. **Format from first clip (clarification 1)**: Start a project with a portrait video; confirm the top bar format, then add a landscape clip and confirm it is fitted without stretching.
8. **Missing media (clarification 2)**: Delete a source file, reopen the project; confirm the clip shows as missing; relink and confirm the trim is kept; remove another.
9. **Autosave and recovery (SC-005)**: Edit, then force-stop the app within a few seconds; reopen and confirm at most 5 seconds of work is lost.
10. **Foldable (Story 5, SC-009, SC-010)**: Edit on the cover screen, unfold mid-playback, then half-fold into tabletop; confirm project, playhead, selection, and playback are unchanged, the layout changes within 1 second, and nothing interactive sits under the hinge. Rotate and enter split-screen; confirm no state loss.
11. **Large-screen input (FR-027)**: With a keyboard attached, test Space, arrow keys, Delete, undo/redo; drag a clip from the library onto the timeline.
12. **Accessibility (FR-030)**: Enable TalkBack and large font size; confirm labels on icon-only controls and no clipped text.

## Done when

All automated checks pass, scenarios 1 to 12 are verified on at least one phone profile and one foldable profile, and screenshot tests show no unapproved deviation from the designer's screens.
