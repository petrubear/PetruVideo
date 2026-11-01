# Implementation Plan: Keyframes for Video Clips

**Branch**: `014-video-keyframes` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`VideoClip` gets the same `keyframes: List<Keyframe>` list that overlay and text clips have (feature 012). The `Pose` means: x and y = the pan (the point of the clip shown at the frame center, permille), size = zoom (1000 = fills the frame as today, up to 4000), opacity. One pure function, `VideoClip.poseAt(relMs)` (built on `Keyframes.poseAt`), plus a pure `Framing.clamp` that keeps the zoomed clip covering the frame, serve preview and export. Rendering is one new per-clip GL effect (`ClipFramingEffect`) added to the clip's existing effect chain after filter and effect; it reads the keyframes live from a handle, so edits show without rebuilding. Existing 012 commands (`AddKeyframe`, `SetPoseAt`, `MoveKeyframe`, `CopyKeyframe`, `DeleteKeyframe`, `SetKeyframeMotion`) are generalised to also address video clips; split, trim, speed, duplicate keep keyframes consistent. The existing `keyframe` table already keys by clip id (unique across lanes), so video clips use it with no schema change and no migration.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Compose BOM 2026.09, Media3 1.11.1 (`BaseGlShaderProgram` per-clip effect, as `ClipEffectHandle`), Room 2.8, Hilt
**Storage**: existing `keyframe` table (v11), owner = video clip id. No schema change, no migration (spike to confirm in research).
**Testing**: JUnit4 + kotest (pose and clamp math, commands, split/trim/speed math, invariants), Paparazzi goldens, androidTest (export frame comparison at several times, preview, round trip, transition with animated clips), accessibility checks
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: pose lookup O(keyframes) per frame; one extra full-frame shader pass only on clips that have keyframes (clips without keyframes add no effect, so export and preview are unchanged, FR-005)
**Constraints**: offline; detekt (line 120, complexity 15, method 60); `EditorViewModel` and `KeyframeEditing.kt` are large, new logic goes in new files
**Scale/Scope**: at most 50 keyframes per clip; about 12 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Uses a Media3 GL effect like the existing clip effects. |
| II Non-destructive | PASS. Keyframes are model data with undo; sources untouched. |
| III Real-time preview | PASS with measure. One extra shader pass on animated clips only; performance check recorded in the quickstart. |
| IV Incremental slices | PASS. Builds on 012 and 011 only. |
| V Test-first on engine | PASS. Pose, clamp, commands, and edit math are pure and tested first. |
| VI Clean architecture | PASS. Math and commands in `core:model`, the effect in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions. |
| VIII Adaptive and foldable-first | PASS by design. Reuses the Keyframes panel, inspector section, and diamonds. Fold and device checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/014-video-keyframes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Models.kt                       VideoClip.keyframes, poseAt
core/model/.../Framing.kt                      FRAMING_* constants, Framing.clamp, Framing.uv (pure)
core/model/.../engine/KeyframeCommands.kt      generalised to the video lane (find clip in any of three lanes)
core/model/.../engine/SplitDuplicateDelete.kt, TrimCommands.kt, SpeedVolumeCommands.kt   keep video keyframes consistent
core/model/.../TimelineInvariants.kt           video keyframes checks
core/data/.../repo/RoomProjectRepository.kt, Mappers.kt   load and save video clip keyframes; project duplicate owner remap (already handled by clip ids)
core/media/.../effects/ClipFramingEffect.kt    handle + GL shader: zoom, pan, opacity from keyframes
core/media/.../composition/CompositionBuilder.kt   adds the framing effect to clips with keyframes; handles map for live update
feature/editor/.../KeyframeEditing.kt          selection and add/gestures for a video clip
feature/editor/.../ui/KeyframePanel.kt, KeyframeDiamonds.kt, video lane view   panel tool for video clips, diamonds on selected clip
feature/editor/.../ui/*Preview*                preview pinch and drag on a selected video clip auto-key
```

**Structure Decision**: extend existing modules; no new module.

## Complexity Tracking

None.

## Spike findings

- The `presentationTimeUs` a per-clip GL effect receives is the clip's own time on the timeline after the speed change: the export frame tests match `VideoClip.poseAt` at 0.2, 1, 1.8 and 2.6 s for a clip with two keyframes.
- Video keyframes use the existing `keyframe` table; no schema change and no migration were needed.
- A clip draws through the framing effect only when it has keyframes or the Keyframes panel was opened on it (`PreviewEngine.armClipFraming`), so clips without keyframes are untouched (FR-005).
