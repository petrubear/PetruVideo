# Implementation Plan: Clip Transforms

**Branch**: `016-clip-transforms` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`VideoClip` gets a `ClipTransform(quarterTurns, flipH, flipV, crop)` value. `crop` is a `CropBox` in permille of the turned picture (left, top, right, bottom). Pure helpers in `core:model` compute the shape of the transformed picture (so the editor can draw the box and keep preset shapes) and validate the box. In the composition each clip with a transform gets Media3 effects placed before the existing fit-to-frame effect: a rotate-and-flip matrix effect, then `Crop`. The fit, keyframe framing, filter, and picture effect follow as today, so the order is rotate and flip, crop, fit, keyframes, filter and effect. Room v13 adds seven columns to `video_clip` by auto migration. The editor gets a Transform clip tool with Rotate, Flip horizontal, Flip vertical, Crop, Reset, and a crop screen over a still of the clip.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Media3 1.11.1 (`ScaleAndRotateTransformation`, `Crop`, `Presentation`), Room 2.8 (auto migration 12 to 13), Compose BOM 2026.09, Hilt
**Storage**: Room v13, columns on `video_clip`: `xfTurns`, `xfFlipH`, `xfFlipV`, `xfCropLeft`, `xfCropTop`, `xfCropRight`, `xfCropBottom` (crop columns default to the whole picture 0, 0, 1000, 1000)
**Testing**: JUnit4 + kotest (transform math, crop rules, preset shapes, commands, split/duplicate/trim/speed/relink), androidTest (export frame tests for rotation, flip, crop, all combined and no-stretch; round trip; migration), Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: clips without a transform add no effect (FR-008); a transformed clip adds one or two cheap matrix passes
**Constraints**: offline; detekt limits (line 120, complexity 15, method 60); `EditorViewModel` is large so Transform handlers go in a small file
**Scale/Scope**: about 14 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Built-in Media3 matrix and crop effects. |
| II Non-destructive | PASS. The transform is model data with undo; sources untouched. |
| III Real-time preview | PASS. Clips with a transform add trivial GPU work; the preview reloads on commit. |
| IV Incremental slices | PASS. Builds on 001 and 014. |
| V Test-first on engine | PASS. Transform math, crop rules, and commands are pure and tested first. |
| VI Clean architecture | PASS. Math and commands in `core:model`, effects in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions. |
| VIII Adaptive and foldable-first | PASS by design. The Transform tool and crop screen use the same sheet and layout code as other tools. Fold and device checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/016-clip-transforms/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Transform.kt                    ClipTransform, CropBox, CropShape presets, turned size, fit helpers
core/model/.../Models.kt                       VideoClip.transform
core/model/.../engine/TransformCommands.kt     RotateClip, FlipClip, SetCrop, ResetTransform
core/model/.../engine/MoveAndRelink.kt         relink keeps rotation and flips, resets the crop on a different shape
core/data/.../db                               columns, mappers, migration 12 to 13
core/media/.../effects/ClipTransformEffects.kt rotate and flip matrix effect + Crop effect list
core/media/.../composition/CompositionBuilder.kt   puts them before the fit
feature/editor/.../TransformEditing.kt         view model helpers (crop session)
feature/editor/.../ui/TransformPanel.kt        Rotate, Flip, Crop, Reset
feature/editor/.../ui/CropScreen.kt            still, box with 48 dp handles, preset chips, Full, Done
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.

## Spike findings

- `ScaleAndRotateTransformation` (counterclockwise degrees, so a clockwise turn counts as 360 minus the turn) then a second one for the mirrors, then `Crop` with NDC edges (top at +1), then the fit: the export frame tests confirm the order and the orientation (turn, mirror, crop, turn then crop, and keyframes acting on the turned picture).
- Turning a mirrored picture swaps which axis is mirrored (stored as turns plus one and the two flips swapped); the unit tests check four turns return to the start.
