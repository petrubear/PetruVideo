# Implementation Plan: Speed Ramps

**Branch**: `018-speed-ramps` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`VideoClip` gets `speedCurve: SpeedCurve?`. A curve is a list of points `(positionMs, speedMilli)` in the clip's *played source time* (0 at the start of playback, the last point at the trimmed source length), with the speed changing linearly between points. A pure `SpeedCurve` class in `core:model` turns the curve into short constant-speed steps (about 100 ms of timeline each); from the same steps it gives the clip's timeline length, the played source time at a timeline moment, and the timeline moment of a source time, so the model, the player, and the export agree. `VideoClip.lengthMs` uses the curve when there is one, so lanes, transitions, durations, keyframes, and export follow. The composition builder hands the player a `SpeedProvider` built from the same steps (the player already supports a provider, as used for constant speed). Split, trim, relink, and freeze cut the curve with `SpeedCurve.windowed`. Room v15 adds a `speedCurve` text column. The editor adds a Speed curve tool with presets and a graph editor.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Media3 1.11.1 (`SpeedProvider` with `SpeedParameters`, pitch kept), Room 2.8 (auto migration 14 to 15), Compose BOM 2026.09 (Canvas graph), Hilt
**Storage**: Room v15, `video_clip.speedCurve` TEXT NULL, points as `position:speed;position:speed`
**Testing**: JUnit4 + kotest (steps, length, mappings, windowing, presets, commands, edit rules), androidTest (export length and frames for presets; sound pitch of a steady tone; migration; round trip), Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: `lengthMs` is cached per curve; the graph readout updates within 100 ms per move
**Constraints**: offline; detekt limits; `EditorViewModel` is large so Speed curve handlers go in a small file
**Scale/Scope**: about 18 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. The player's own speed provider. |
| II Non-destructive | PASS. The curve is model data with undo; sources untouched. |
| III Real-time preview | PASS with measure. Many short steps are cheap for the player; checked in the preview test. |
| IV Incremental slices | PASS. Builds on 002 (speed) and 012 to 017. |
| V Test-first on engine | PASS. Steps, length, mappings, and windowing are pure and tested first. |
| VI Clean architecture | PASS. Math and commands in `core:model`, provider in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions. |
| VIII Adaptive and foldable-first | PASS by design. Panel and graph use the same sheet and layout code. Device and fold checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/018-speed-ramps/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../SpeedCurve.kt                   SpeedPoint, SpeedCurve (steps, length, mappings, windowed), presets
core/model/.../Models.kt                       VideoClip.speedCurve, lengthMs, played-offset helpers
core/model/.../engine/SpeedCurveCommands.kt    SetSpeedCurve (points or preset), ResetSpeedCurve, point edits
core/model/.../engine/SplitDuplicateDelete.kt, TrimCommands.kt, MoveAndRelink.kt   cut the curve with the clip
core/model/.../engine/SpeedVolumeCommands.kt   constant speed refused while a curve is on
core/data/.../db                               column, mapping, migration 14 to 15
core/media/.../composition/CompositionBuilder.kt   SpeedProvider from the steps
feature/editor/.../SpeedCurveEditing.kt        view model helpers
feature/editor/.../ui/SpeedCurvePanel.kt, SpeedGraph.kt   presets and the point editor
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.

## Spike findings

- The player's `SpeedProvider` takes times relative to the start of the (clipped) media item, so the steps from `SpeedCurve.steps` are handed to it as they are; for a reversed clip the copy plays in the same direction, so the same steps apply.
- The exported length of a clip with the Bullet or Montage preset matches the length the editor shows within 150 ms on the emulator (the test allows 150 ms; the design target is 100 ms).
- Not covered yet: the sound pitch across a ramp (SC-004) was not measured; the player flag that keeps pitch is the one already used for constant speed. The project list duration and the ducking trigger mapping still assume constant speed for clips with a curve.
