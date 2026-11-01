# Tasks: Keyframes

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 animate, US2 timeline editing, US3 kept, saved, exported. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model `Keyframe`, `Pose`, `Keyframes.poseAt` (hold, Linear, Ease), edge-value helper, limits; `keyframes` on `OverlayClip` and `TextClip` with `poseAt`; tests first (`core/model/.../Keyframes.kt`, `Overlay.kt`, `Text.kt`, `KeyframesTest`)
- [X] T002 Room v11 `keyframe` table, DAO, mapping, session save, project duplicate remap, auto migration; `Migration10To11Test` and a round-trip test (`core/data/.../db`, `OriginalsUnchangedTest`)

## Phase 2: US1 Animate an overlay or text clip (P1)

- [X] T003 [US1] Commands `AddKeyframe`, `SetPoseAt` (50 per clip; the existing overlay and text placement, size, and opacity commands redirect to `SetPoseAt` when the clip has keyframes); plain values follow the first keyframe; deleting the last keyframe keeps the look; invariants (sorted, unique, inside clip, in range, at least one frame apart); tests first (`core/model/.../engine/KeyframeCommands.kt`, `KeyframeCommandsTest`, `TimelineInvariants.kt`)
- [X] T004 [US1] Overlay rendering from `poseAt` in `OverlayLayout`; export frame tests at several times (position, size, opacity) with the brown photo (`core/media/.../OverlayLayout.kt`, `ExportPipelineTest`)
- [X] T005 [US1] Text rendering from `poseAt`: position, scale from the largest keyframe size, alpha, composed with the entrance and exit animation; export frame test (`core/media/.../text`, `ExportPipelineTest`)
- [X] T006 [US1] Preview: live update with keyframes for both lanes, scrubbing and playing, plus a frame-by-frame smoothness test (SC-005); `KeyframePlaybackTest` (`core/media/.../preview`, `Media3PreviewEngine.kt`)
- [X] T007 [US1] Editor: Keyframes clip tool and panel (Add, Previous, Next), auto-keying of preview gestures and sliders on animated clips, view model events and tests including the tap count (SC-006) (`feature/editor/.../KeyframeEditing.kt`, `KeyframePanel.kt`, `EditorViewModel.kt`, `EditorContract.kt`)

## Phase 3: US2 Edit keyframes on the timeline (P2)

- [X] T008 [US2] Commands `MoveKeyframe`, `CopyKeyframe`, `DeleteKeyframe`, `SetKeyframeMotion`; neighbour and frame-gap rules, the plain values keep following the first keyframe; tests first (`KeyframeCommands.kt`)
- [X] T009 [US2] Diamonds on the selected overlay and text clips (48 dp, tap selects and seeks, drag moves with snapping), selected-keyframe state, panel buttons Copy here, Delete, Linear or Ease (`feature/editor/.../ui/KeyframeDiamonds.kt`, `OverlayUi.kt`, `TextLaneView.kt`, `KeyframePanel.kt`)
- [X] T010 [US2] Goldens, accessibility and Compose tests for the panel and diamonds in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported (P3)

- [X] T011 [US3] Split keeps both halves right (edge keyframes), trim removes outside keyframes and keeps edge values, duplicate and move keep relative times; tests first (`SplitDuplicateDelete.kt`, `TrimCommands.kt`, `TextCommands.kt`, `OverlayCommands.kt`)
- [X] T012 [US3] Relink, remove-missing, project duplicate and process-death recovery keep keyframes; tests
- [X] T013 [US3] Export test comparing a frame inside an animation with the pose function and the preview frame (`ExportPipelineTest`)

## Phase 5: Polish

- [X] T014 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest`; stop the emulator; fix findings; record results and the preview fps check in quickstart.md
- [ ] T015 Device checks (fold, mid-range fps) and designer review: deferred until the user is back

## Dependencies

T001 and T002 first. US1: T003 needs T001; T004, T005 need T001 (parallel with each other); T006 needs T004, T005; T007 needs T003, T006. US2 needs US1's commands: T008 after T003, T009 after T007 and T008, T010 after T009. US3: T011 needs T001 and T003; T012 needs T002; T013 needs T004. T014 after all.

## Implementation strategy

MVP is US1 with two keyframes on an overlay, then text, then timeline editing. Each story ends green and committed before the next.
