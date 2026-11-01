# Tasks: Audio Ducking

Format: `- [ ] ID [P] [Story] description with path`. Stories: US1 keep music under speech, US2 see which clips are ducked, US3 kept through edits, saved, exported. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Model: `Ducking`, `DuckSmoothness` (Quick, Medium, Slow with attack and release), `DuckEnvelope` (20 ms grid, `gainAt` interpolation), `AudioClip.ducking`; tests first (`core/model/.../Ducking.kt`, `Models.kt`, `DuckingTest`)
- [X] T002 Planner: `DuckingPlanner.envelopeFor` (trigger rule, trim, position and speed mapping, volume, mute, self and two-ducked rules, threshold, look-ahead, one-pole smoothing, 90% maximum, start-at-target); tests first including no jumps and a trigger at the very start (`core/model/.../DuckingPlanner.kt`, `DuckingPlannerTest`)
- [X] T003 Persistence: Room v12 columns `duck_enabled`, `duck_amount`, `duck_smoothness`, mapping, auto migration 11 to 12; `Migration11To12Test` and a round-trip plus project duplicate test (`core/data/.../db`, `OriginalsUnchangedTest`)

## Phase 2: US1 Keep music under speech (P1)

- [X] T004 [US1] Command `SetDucking` (enabled, amount, smoothness; audio-lane clips only; one undo step); invariants; tests first (`core/model/.../engine/DuckingCommands.kt`, `TimelineInvariants.kt`, `DuckingCommandsTest`)
- [X] T005 [US1] `GainAudioProcessor.setEnvelope` applied after volume and fade, seek-safe, never above 1; tests including a seek mid-clip (`core/media/.../preview/GainAudioProcessor.kt`, `GainAudioProcessorTest`)
- [X] T006 [US1] `DuckingAnalyzer` (peaks via `WaveformPeaksGenerator`, planner, envelopes for all ducked clips); builder hands envelopes to processors; export computes them before building; export level test: music level in a voice section vs a pause matches the amount within 2 dB, no change over 3 dB in any 5 ms (`core/media/.../ducking/DuckingAnalyzer.kt`, `CompositionBuilder.kt`, `core/export/.../TransformerRunner.kt`, `ExportPipelineTest`)
- [X] T007 [US1] Preview: after each load compute and push envelopes in the background, cancel on the next load; androidTest that a ducked clip's processor receives its envelope (`Media3PreviewEngine.kt`, `DuckingPlaybackTest`)
- [X] T008 [US1] Editor: Ducking section in the Audio panel (switch, Amount, Smoothness), view model events, tap count test (SC-007) (`feature/editor/.../ui/AudioPanel.kt`, `EditorViewModel.kt`, `EditorContract.kt`)

## Phase 3: US2 See which clips are ducked (P2)

- [X] T009 [US2] Badge on ducked audio clips with an accessible description; the "nothing is playing over this clip" note from the trigger rule; control offered for audio-lane clips only (`feature/editor/.../ui/AudioLaneView.kt`, `AudioPanel.kt`, `EditorStateBuilders.kt`)
- [X] T010 [US2] Goldens, accessibility and Compose tests for the section and badge in compact, unfolded and tabletop

## Phase 4: US3 Kept, saved, exported (P3)

- [X] T011 [US3] Split, duplicate, trim, move, speed change, delete, relink, project duplicate keep each clip's settings and the plan follows the new arrangement; tests (`SplitDuplicateDelete.kt`, `AudioClipCommands.kt`, planner tests with edited timelines)
- [X] T012 [US3] Ducking together with volume, fade, noise reduction, equalizer, reverb and voice effects: lowering applies last and never raises the level; test (`GainAudioProcessorTest`, `ExportPipelineTest`)

## Phase 5: Polish

- [X] T013 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest` (no build cache); stop the emulator; fix findings; record results and a preview stall check in quickstart.md
- [ ] T014 Device checks (fold, a listening check on the phone) and designer review: deferred until the user is back

## Dependencies

T001 first. T002 and T003 need T001. US1: T004 needs T001; T005 is independent of T002; T006 needs T002, T003 and T005; T007 needs T006; T008 needs T004. US2: T009 after T008, T010 after T009. US3: T011 needs T002 and T004; T012 needs T005 and T006. T013 after all.

## Implementation strategy

MVP is US1: music ducks under a voice-over in preview and export with the panel switch. Then the badge and note, then edit consistency. Each story ends green and committed before the next.
