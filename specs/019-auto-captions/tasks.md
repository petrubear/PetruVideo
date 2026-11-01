# Tasks: Auto Captions

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 generate, US2 unavailable states, US3 edit and export. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model: `TextClip.caption`, `CaptionPhrase`, `CaptionLine`, `CaptionPlanner` (eligible clips, speech bins, bridging, minimum, long-phrase cut, timeline mapping, skipped notes, 200 limit); tests first (`core/model/.../Text.kt`, `Captions.kt`, `CaptionPlannerTest`)
- [X] T002 Persistence: Room v16 column `caption`, mapping, auto migration 15 to 16; `Migration15To16Test` and a round-trip plus project duplicate test (`core/data/.../db`, `OriginalsUnchangedTest`)

## Phase 2: US1 Generate captions (P1)

- [X] T003 [US1] Command `AddCaptions` (caption style, replace only captions, drops short lines, 200 limit, one undo step); invariants; tests first (`core/model/.../engine/CaptionCommands.kt`, `CaptionCommandsTest`)
- [X] T004 [US1] `PhraseAudio` (range to mono 16 kHz PCM) and `SpeechTranscriber` with `AndroidSpeechTranscriber`; androidTest for PCM preparation and for availability on the emulator (`core/media/.../captions`, `PhraseAudioTest`, `SpeechTranscriberTest`)
- [X] T005 [US1] `CaptionService` (plan, cut, recognise one phrase at a time, progress, cancel leaves nothing, counts skipped); tests with a fake recognizer (`core/media/.../captions/CaptionService.kt`, `CaptionServiceTest`)
- [X] T006 [US1] Editor: Auto captions button on the Text tab, panel with language, Replace, Start, progress and Cancel, result note, view model events and tests including the tap count (SC-001) (`feature/editor/.../CaptionsEditing.kt`, `ui/CaptionsPanel.kt`, `ui/MainToolBar.kt`, `EditorViewModel.kt`, `EditorContract.kt`)

## Phase 3: US2 Unavailable states (P2)

- [X] T007 [US2] Availability states in the panel (needs Android 13, language missing with the installed list, Open speech settings effect), tests (`CaptionsPanel.kt`, `EditorRoute.kt`, `EditorViewModelCaptionsTest`)
- [X] T008 [US2] Goldens, accessibility and Compose tests for the panel states in compact, unfolded and tabletop

## Phase 4: US3 Edit and export (P3)

- [X] T009 [US3] Captions edited, saved, reopened, duplicated, and exported like text clips; Replace finds edited captions; export test with a caption (`ExportPipelineTest`, `OriginalsUnchangedTest`)

## Phase 5: Polish

- [X] T010 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results in quickstart.md
- [ ] T011 Device checks (real recognition on the phone, fold) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 needs T001. US1: T003 needs T001; T004 independent; T005 needs T001 and T004; T006 needs T003 and T005. US2: T007 after T006; T008 after T007. US3: T009 needs T003 and T002. T010 after all.

## Implementation strategy

MVP is US1 with a fake recognizer proving the whole path, then the unavailable states, then the edit and export checks. Each story ends green and committed before the next.
