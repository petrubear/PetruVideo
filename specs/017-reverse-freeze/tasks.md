# Tasks: Reverse and Freeze Frame

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 freeze a frame, US2 reverse a clip, US3 kept, saved, exported. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model: `VideoClip.reversed`, `FREEZE_MIN_MS`, `FREEZE_MAX_MS`, `FREEZE_DEFAULT_MS`, `REVERSE_MAX_MS`, source-time mapping helper for a clip offset; tests first (`core/model/.../Models.kt`, `Reverse.kt`, `ReverseTest`)
- [X] T002 Persistence: Room v14 column `reversed`, mapping, auto migration 13 to 14; `Migration13To14Test` and a round-trip plus project duplicate test (`core/data/.../db`, `OriginalsUnchangedTest`)

## Phase 2: US1 Freeze a frame (P1)

- [X] T003 [US1] Command `FreezeFrame` (split at the playhead, insert the still between the parts, edges without an empty part, 0.5 to 10 s, copies transform, filter, effect, one keyframe for the animated pose, no sound, transition out stays on the last part, one undo step); invariants; tests first (`core/model/.../engine/FreezeCommands.kt`, `FreezeCommandsTest`)
- [X] T004 [US1] `FreezeStills`: extract the frame at the clip's source time to a JPEG under `freezes/` and probe it as a photo media item; androidTest that the still equals the source frame at that time (`core/media/.../freeze/FreezeStills.kt`, `FreezeStillsTest`)
- [X] T005 [US1] Editor: Freeze frame tool and panel (length slider, Insert), view model events and tests including the tap count (SC-007) (`feature/editor/.../ReverseFreezeEditing.kt`, `ui/FreezePanel.kt`, `EditorViewModel.kt`, `EditorContract.kt`, `EditorStateBuilders.kt`)
- [X] T006 [US1] Export test: the frame inside a freeze equals the frame at the playhead before freezing (`ExportPipelineTest`)

## Phase 3: US2 Reverse a clip (P2)

- [ ] T007 [US2] Spike and `ReverseRenderer`: all-intra Transformer pass, reverse remux of the video samples, reversed audio encoded to AAC, progress and cancel that leave nothing behind; androidTest on a synthetic clip with a marked first and last frame and a rising tone (`core/media/.../reverse/ReverseRenderer.kt`, `ReverseRendererTest`); record findings in plan.md
- [ ] T008 [US2] `ReverseStore` (file name by fingerprint and range, covering lookup, cleanup of unused files, free-space check); tests (`core/media/.../reverse/ReverseStore.kt`, `ReverseStoreTest`)
- [ ] T009 [US2] Command `SetReversed` (video-kind clips up to 30 s as trimmed); reversed split, trim (mirrored), duplicate; tests first (`core/model/.../engine/ReverseCommands.kt`, `SplitDuplicateDelete.kt`, `TrimCommands.kt`, `ReverseCommandsTest`)
- [ ] T010 [US2] Builder reads the reversed file with the mapped range; preview and export make sure files exist first (render missing ones); export test: frames and sound of a reversed clip are in reverse order (`CompositionBuilder.kt`, `Media3PreviewEngine.kt`, `TransformerRunner.kt`, `ExportPipelineTest`)
- [ ] T011 [US2] Editor: Reverse tool, progress with Cancel, unavailable reasons, "preparing" note, view model events and tests (`ReverseFreezeEditing.kt`, `ui/ReversePanel.kt`, `ui/ReverseProgress.kt`, `EditorViewModel.kt`)
- [ ] T012 [US2] Goldens, accessibility and Compose tests for the freeze panel and reverse progress in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported (P3)

- [ ] T013 [US3] Relink, remove-missing, project duplicate and process-death recovery keep reversed state and freezes; reversed files rebuilt when missing and swept when unused; tests (`MoveAndRelink.kt`, `ReverseStore.kt`)

## Phase 5: Polish

- [ ] T014 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results in quickstart.md
- [ ] T015 Device checks (fold, reverse timing on the phone) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 needs T001. US1: T003 needs T001; T004 is independent; T005 needs T003 and T004; T006 needs T004. US2: T007 independent of the model; T008 needs T007; T009 needs T001; T010 needs T007, T008 and T009; T011 needs T010; T012 after T011. US3: T013 needs T009 and T008. T014 after all.

## Implementation strategy

MVP is US1 (freeze) shipped and committed first, then reverse. Each story ends green and committed before the next.
