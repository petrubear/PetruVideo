# Tasks: Voice-over Recording

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 record, US2 permission and interruptions, US3 a normal clip. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Spike on the emulator (audio enabled): `MediaRecorder` AAC_ADTS records 2 s to a file, the file plays in Media3 as an audio clip at its estimated length, and a file cut short still plays; record findings and the start-to-sound offset in plan.md (`core/media/src/androidTest/.../VoiceRecorderTest.kt`)
- [X] T002 Pure logic: limits and `maxDurationMs` (30 min, next clip, storage, 0.5 s minimum), the `AddVoiceOver` command (exact start, no overlap, minimum length, one undo step); tests first (`core/model/.../VoiceOver.kt`, `engine/AddVoiceOver.kt`, `AddVoiceOverTest`)

## Phase 2: US1 Record a voice-over over the video (P1)

- [X] T003 [US1] `VoiceRecorder` interface and `MediaRecorder` implementation: start (a failure to open the microphone is reported), stop, level every 50 ms, elapsed time, error and interruption callbacks; `VoiceOverStore` (files, journal, media item from a file) (`core/media/.../voiceover`, `app/.../di/VoiceOverModule.kt`)
- [X] T004 [US1] Manifest `RECORD_AUDIO`; `MicAccess` pure permission-state logic and tests; `VoiceOverFlow` permission launcher and settings hand-off (`app/src/main/AndroidManifest.xml`, `feature/editor/.../MicAccess.kt`, `VoiceOverFlow.kt`)
- [X] T005 [US1] View model flow with a fake recorder: Record, 3 s count-in, cancel, start the recorder first and playback right after it has started, from the playhead, lock editing while counting in and recording, "The microphone is in use" on a start failure, Stop adds the clip selected, stop at the next clip, 30 minutes or low storage, too short discarded; state tests first (`feature/editor/.../EditorViewModel.kt`, `EditorContract.kt`, `EditorViewModelVoiceOverTest`)
- [X] T006 [US1] UI: Record voice-over on the Audio tab bar, `RecordingBar` (count-in digit and hint, level meter, timer, Stop, Cancel), accessibility live region, Back stops, timeline and tools disabled during a take (`feature/editor/.../ui/RecordingBar.kt`, `MainToolBar.kt`, `EditorBottomBars.kt`)

## Phase 3: US2 Permission and interruptions (P2)

- [X] T007 [US2] Permission outcomes in the flow: ask on first Record, Denied and Blocked messages, Open settings action; tests (`VoiceOverFlow.kt`, `EditorViewModelVoiceOverTest`)
- [X] T008 [US2] Interruptions keep the take: ON_STOP, microphone taken by another app, low storage; journal written at start and removed at the clip; recovery when a project opens; tests with a fake recorder and a real file (`EditorRoute.kt`, `EditorViewModel.kt`, `VoiceOverStore.kt`)

## Phase 4: US3 A normal audio clip (P3)

- [X] T009 [US3] Audio tools, save, reopen, preview and project duplicate work for a recorded clip; orphan-file sweep after a project is deleted that skips journal files and files under a minute old (`ProjectRepository.allMediaUris()`, `VoiceOverStore.sweep`, `ProjectsViewModel`); tests including `OriginalsUnchangedTest` style round trip
- [X] T010 [US3] Export test: record on the emulator, add the clip, export, check the file finishes and its length (`core/export/src/androidTest/.../ExportPipelineTest.kt`)
- [X] T011 [US3] Goldens, accessibility and Compose tests for the Audio tab bar, count-in, recording bar and large font in compact, unfolded and tabletop

## Phase 5: Polish

- [X] T012 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (use `--no-build-cache` for the app module if Hilt output looks stale); stop the emulator; fix findings; record results and the playback stats check in quickstart.md
- [ ] T013 Device checks (real microphone quality, echo, fold) and designer review: deferred until the user is back

## Dependencies

T001 and T002 first. US1: T003 needs T001; T004 in parallel with T003; T005 needs T002, T003, T004; T006 needs T005. US2: T007 needs T005, T004; T008 needs T002, T003, T005. US3: T009 needs T003; T010 needs T003; T011 last. T012 after all.

## Implementation strategy

MVP is US1 with a granted permission and a clean Stop. Interruptions and recovery come next, then the polish of tests. Each story ends green and committed before the next.
