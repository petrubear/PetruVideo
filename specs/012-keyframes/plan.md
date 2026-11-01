# Implementation Plan: Keyframes

**Branch**: `012-keyframes` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Overlay and text clips get an ordered list of `Keyframe` values (time relative to the clip, place, size, opacity, motion). One pure function, `Keyframes.poseAt`, gives the clip's shown place, size, and opacity at any time; the preview and export both call it, so they cannot differ. Pure commands edit keyframes with undo; split, trim, duplicate, move, and delete keep them consistent. Room v11 adds one `keyframe` table. The editor adds a Keyframes tool with a panel, diamonds on the selected clip in the lane, and automatic keyframing when a gesture changes a clip that already has keyframes.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Compose BOM 2026.09, Media3 1.11.1 (`OverlayLayout` compositor settings, `BitmapOverlay`), Room 2.8 (auto migration 10 to 11), Hilt
**Storage**: Room v11, table `keyframe(projectId, ownerId, timeMs, x, y, size, opacity, ease)`; ownerId is the overlay or text clip id
**Testing**: JUnit4 + kotest (interpolation, commands, split/trim math, invariants), Paparazzi goldens, androidTest (export frame comparison at several times, preview playback, migration, round trip), accessibility checks
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: Principle III: pose lookup is O(keyframes) per frame, no allocation beyond one small object; 30 fps preview with 3 animated clips
**Constraints**: offline; detekt limits (line 120, complexity 15, method 60); the editor view model is already large, so keyframe logic goes in its own file
**Scale/Scope**: at most 50 keyframes per clip; about 15 new files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Uses the existing compositor settings and overlay effects. |
| II Non-destructive | PASS. Keyframes are model data with undo; sources untouched. |
| III Real-time preview | PASS with measure. A pose lookup per frame per clip is trivial; the performance check is recorded in the quickstart. |
| IV Incremental slices | PASS. Builds on 006 and 010 only. |
| V Test-first on engine | PASS. Interpolation, commands, and split/trim math are pure and tested first. |
| VI Clean architecture | PASS. Math and commands in `core:model`, rendering in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no new permissions. |
| VIII Adaptive and foldable-first | PASS by design. The panel and diamonds reuse the existing sheet and lane code and the inspector section. Device and fold checks stay deferred until the user is back. |

Post-design re-check: PASS. No violations, so no Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/012-keyframes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Keyframes.kt                Keyframe, Pose, Keyframes.poseAt, edge helpers, limits
core/model/.../Overlay.kt, Text.kt         keyframes field, poseAt, effective values
core/model/.../engine/KeyframeCommands.kt  AddKeyframe, SetPoseAt, MoveKeyframe, CopyKeyframe, DeleteKeyframe, SetKeyframeMotion
core/model/.../engine/SplitDuplicateDelete.kt, TrimCommands.kt, TextCommands.kt, OverlayCommands.kt   keep keyframes consistent
core/data/.../db                           KeyframeEntity, DAO, mapping, session save, project duplicate; migration 10 to 11
core/media/.../composition/OverlayLayout.kt   shown values from poseAt
core/media/.../text/TextOverlays.kt, TextAnimator.kt   text position, scale, alpha from poseAt, on top of entrance and exit
feature/editor/.../KeyframeEditing.kt      view model helpers, selected keyframe state
feature/editor/.../ui/KeyframePanel.kt, KeyframeDiamonds.kt   panel and lane diamonds
feature/editor/.../ui/OverlayUi.kt, TextLaneView.kt, TextPreviewLayer.kt   diamonds, gestures auto-key
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.
