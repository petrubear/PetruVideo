# Tasks: Clip Transforms

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 rotate and flip, US2 crop, US3 kept, saved, exported, combined. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model: `ClipTransform`, `CropBox` (validation, minimum size, `isFull`), turn and flip of a box, `turnedSize`, preset shape boxes (1:1, 4:5, 9:16, 16:9, 4:3), `VideoClip.transform`; tests first (`core/model/.../Transform.kt`, `Models.kt`, `TransformTest`)
- [X] T002 Persistence: Room v13 columns, mapping, auto migration 12 to 13; `Migration12To13Test` and a round-trip plus project duplicate test (`core/data/.../db`, `OriginalsUnchangedTest`)
- [X] T003 Rendering spike and effects: `ClipTransformEffects` (rotate and flip matrix, `Crop`) placed before the fit in the clip chain, only for transformed clips; export frame tests for rotation, flip, crop, combined, and no stretch (`core/media/.../effects/ClipTransformEffects.kt`, `CompositionBuilder.kt`, `ExportPipelineTest`); record findings in plan.md

## Phase 2: US1 Rotate and flip (P1)

- [X] T004 [US1] Commands `RotateClip`, `FlipClip`, `ResetTransform` (the box turns and mirrors with the picture; main-track clips only; one undo step); invariants; tests first (`core/model/.../engine/TransformCommands.kt`, `TimelineInvariants.kt`, `TransformCommandsTest`)
- [X] T005 [US1] Editor: Transform clip tool and panel (Rotate, Flip horizontal, Flip vertical, Reset), view model events and tests including the tap count (SC-007) (`feature/editor/.../TransformEditing.kt`, `ui/TransformPanel.kt`, `EditorViewModel.kt`, `EditorContract.kt`, `EditorStateBuilders.kt`)

## Phase 3: US2 Crop (P2)

- [X] T006 [US2] Command `SetCrop` (validated box, minimum size, inside the picture); tests first (`TransformCommands.kt`, `TransformCommandsTest`)
- [X] T007 [US2] Crop screen: still, box with eight 48 dp handles, nudge buttons, preset chips with shape kept in pixels, Full, Cancel, Done; view model crop session (`ui/CropScreen.kt`, `TransformEditing.kt`)
- [X] T008 [US2] Goldens, accessibility and Compose tests for the panel and the crop screen in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported, combined (P3)

- [X] T009 [US3] Split, duplicate, trim, move, speed, delete, project duplicate keep the transform; relink to a different shape resets only the crop with a message; tests (`SplitDuplicateDelete.kt`, `MoveAndRelink.kt`)
- [X] T010 [US3] Combined with keyframes, a filter, an effect, and a transition: export frame tests (`ExportPipelineTest`)

## Phase 5: Polish

- [X] T011 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results in quickstart.md
- [ ] T012 Device checks (fold, feel on the phone) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 and T003 need T001. US1: T004 needs T001; T005 needs T004 and T003. US2: T006 needs T004; T007 needs T006 and T005; T008 after T007. US3: T009 needs T004; T010 needs T003. T011 after all.

## Implementation strategy

MVP is US1: rotate and flip in preview and export. Then crop, then edit consistency and combinations. Each story ends green and committed before the next.
