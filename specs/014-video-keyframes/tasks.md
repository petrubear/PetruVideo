# Tasks: Keyframes for Video Clips

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 zoom and pan a clip, US2 timeline editing, US3 kept through edits, saved, exported. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Spike: confirm the `presentationTimeUs` a per-clip GL effect receives is the clip's own time after speed change (record in plan.md "Spike findings"); confirm saving video keyframes needs no schema change (`core/media/.../effects`, `core/data`)
- [X] T002 Model: `VideoClip.keyframes` and `poseAt` (neutral pose `500,500,1000,100` when empty); `Framing.clamp` (zoom 1000..4000, cover rule for x/y) and its tests first (`core/model/.../Models.kt`, `Framing.kt`, `FramingTest`, `KeyframesTest`)
- [X] T003 Persistence: load and save video lane keyframes with the existing `keyframe` table; project duplicate keeps them; round-trip test (`core/data/.../RoomProjectRepository.kt`, `Mappers.kt`, `OriginalsUnchangedTest`)

## Phase 2: US1 Zoom and pan a video clip (P1)

- [X] T004 [US1] Commands: `AddKeyframe`, `SetPoseAt` and the others accept video clips (clip found in any lane); first change on a clip without keyframes adds a start keyframe holding the plain look when the playhead is not at the start; poses clamped; 50 limit; invariants for video clips; tests first (`core/model/.../engine/KeyframeCommands.kt`, `TimelineInvariants.kt`, `VideoKeyframeCommandsTest`)
- [X] T005 [US1] Rendering: `ClipFramingEffect` with a live handle (zoom, pan, opacity from `poseAt` + `Framing.clamp`), added last in the clip chain only for clips with keyframes; export frame tests at several times (zoom crop, pan, fade) (`core/media/.../effects/ClipFramingEffect.kt`, `CompositionBuilder.kt`, `ExportPipelineTest`)
- [X] T006 [US1] Preview: live update of the framing handle when keyframes change, playing and scrubbing, smoothness test (SC-005), transition between two animated clips (FR-012) (`Media3PreviewEngine.kt`, `KeyframePlaybackTest`)
- [X] T007 [US1] Editor: Keyframes tool and panel for a selected video clip (Add, Previous, Next, Zoom and Opacity sliders), preview pinch and drag auto-key, view model events, tap count test (SC-007) (`feature/editor/.../KeyframeEditing.kt`, `KeyframePanel.kt`, `EditorViewModel.kt`, preview gesture layer)

## Phase 3: US2 Edit keyframes on the timeline (P2)

- [X] T008 [US2] Move, Copy, Delete, Motion commands for video clips; deleting the last keyframe returns the clip to full frame and full opacity; tests first (`KeyframeCommands.kt`, `VideoKeyframeCommandsTest`)
- [X] T009 [US2] Diamonds on the selected video clip in a strip clear of the trim handles (48 dp, tap selects and seeks, drag moves with snapping), panel buttons Copy here, Delete, Linear or Ease (`feature/editor/.../ui/KeyframeDiamonds.kt`, video lane clip view, `KeyframePanel.kt`)
- [X] T010 [US2] Goldens, accessibility and Compose tests for the panel and diamonds in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported (P3)

- [X] T011 [US3] Split (edge keyframes), trim (window), speed change (rescale times), duplicate keep video keyframes right; tests first (`SplitDuplicateDelete.kt`, `TrimCommands.kt`, `SpeedVolumeCommands.kt`)
- [X] T012 [US3] Relink, remove-missing, project duplicate and process-death recovery keep video keyframes; tests
- [X] T013 [US3] Export test: a frame inside an animation matches the pose function and the preview frame, including across a transition (`ExportPipelineTest`)

## Phase 5: Polish

- [ ] T014 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results and the preview fps check in quickstart.md
- [ ] T015 Device checks (fold, mid-range fps, feel on the phone) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 then T003. US1: T004 needs T002; T005 needs T001 and T002; T006 needs T005; T007 needs T004 and T006. US2: T008 after T004, T009 after T007 and T008, T010 after T009. US3: T011 needs T002 and T004; T012 needs T003; T013 needs T005. T014 after all.

## Implementation strategy

MVP is US1: a two-keyframe push-in on a video clip in preview and export. Then timeline editing, then edit consistency. Each story ends green and committed before the next.
