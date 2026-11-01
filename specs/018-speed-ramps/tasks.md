# Tasks: Speed Ramps

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 presets, US2 custom curve, US3 kept, saved, exported. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model: `SpeedPoint`, `SpeedCurve` (validation, steps, timeline length, `sourceAt`, `timelineAt`, `windowed`, presets), `VideoClip.speedCurve`, `lengthMs`, played-offset helpers; tests first (`core/model/.../SpeedCurve.kt`, `Models.kt`, `SpeedCurveTest`)
- [X] T002 Persistence: Room v15 column, mapping, auto migration 14 to 15; `Migration14To15Test` and a round-trip plus project duplicate test (`core/data/.../db`, `OriginalsUnchangedTest`)
- [X] T003 Rendering spike: builder `SpeedProvider` from the steps for forward and reversed clips; export tests for length and frames of a preset and for the pitch of a steady tone (`CompositionBuilder.kt`, `ExportPipelineTest`); record findings in plan.md

## Phase 2: US1 Presets (P1)

- [X] T004 [US1] Commands `SetSpeedCurve` (preset or points, length limits), `ResetSpeedCurve`, constant speed refused while a curve is on; invariants; tests first (`core/model/.../engine/SpeedCurveCommands.kt`, `SpeedVolumeCommands.kt`, `TimelineInvariants.kt`, `SpeedCurveCommandsTest`)
- [X] T005 [US1] Editor: Speed curve tool, panel with presets, readout and Reset, dimmed constant Speed, view model events and tests including the tap count (SC-007) (`feature/editor/.../SpeedCurveEditing.kt`, `ui/SpeedCurvePanel.kt`, `EditorViewModel.kt`, `EditorContract.kt`, `EditorStateBuilders.kt`)

## Phase 3: US2 Custom curve (P2)

- [X] T006 [US2] Point commands (add, move, nudge, delete with limits) as pure functions on `SpeedCurve` and one command `EditSpeedCurve`; tests first (`SpeedCurve.kt`, `SpeedCurveCommands.kt`)
- [X] T007 [US2] Graph editor: log speed axis, draggable points (48 dp), tap to add, point buttons, nudge actions, live readout; view model drag session (`ui/SpeedGraph.kt`, `SpeedCurveEditing.kt`)
- [X] T008 [US2] Goldens, accessibility and Compose tests for the panel and graph in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported (P3)

- [X] T009 [US3] Split, trim, relink clamp, duplicate, reverse, freeze keep the speed at every moment; keyframes follow the new length; tests (`SplitDuplicateDelete.kt`, `TrimCommands.kt`, `MoveAndRelink.kt`)
- [X] T010 [US3] Combined with keyframes, a transition, and reverse: export frame and length tests (`ExportPipelineTest`)

## Phase 5: Polish

- [X] T011 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results in quickstart.md
- [ ] T012 Device checks (fold, feel of the graph on the phone, preview smoothness) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 and T003 need T001. US1: T004 needs T001; T005 needs T004. US2: T006 needs T004; T007 needs T006 and T005; T008 after T007. US3: T009 needs T004; T010 needs T003. T011 after all.

## Implementation strategy

MVP is US1: presets in preview and export. Then the graph editor, then edit consistency and combinations. Each story ends green and committed before the next.
