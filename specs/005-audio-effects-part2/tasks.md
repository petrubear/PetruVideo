# Tasks: Audio Tools Part 2 - Fades, Noise Reduction, EQ, Voice Effects

**Input**: Design documents from `/specs/005-audio-effects-part2/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-screens.md, quickstart.md

**Tests**: Included (Principle V: commands, presets and DSP test-first; migration test; view model, Compose, golden and export checks).

**Organization**: Grouped by user story. `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `P` = `core/media/src/main/kotlin/com/petruvideo/core/media/preview`, `X` = `core/media/src/main/kotlin/com/petruvideo/core/media/dsp`, `B` = `core/media/src/main/kotlin/com/petruvideo/core/media/composition`, `E` = `feature/editor/src/main/kotlin/com/petruvideo/feature/editor`, `D` = `core/data/src/main/kotlin/com/petruvideo/core/data/db`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallel (different files, no dependencies)
- **[Story]**: US1 fade, US2 noise reduction, US3 equalizer, US4 voice effects, US5 audio group, layout, persistence, export match

---

## Phase 1: Setup

- [ ] T001 Run the 004 baseline (`gradle --offline testDebugUnitTest detekt`) and confirm it is green
- [ ] T002 [P] Add designsystem pieces if missing in `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/`: icons for Audio, Fade, Noise, Equalizer, Voice; a labeled switch row; a vertical band slider style (icons and components only)

---

## Phase 2: Foundational (blocks all stories)

- [ ] T003 [P] Write failing tests in `core/model/src/test/kotlin/com/petruvideo/core/model/AudioFxTest.kt`: neutral defaults, clamping of every range, EQ preset values and `Custom` matching, fade limiting rule (in + out <= length, each <= half)
- [ ] T004 Add `AudioFx.kt` in `M/` (Fade, NoiseReduction, NoiseStrength, Equalizer, EqPreset, Echo, Robot, Pitch, Bass, VoiceFx, limits) and the fields on `AudioClip` and `VideoClip` in `M/Models.kt`; make T003 pass
- [ ] T005 [P] Extend `M/engine/TimelineInvariants.kt` with the new ranges and a test in `core/model/src/test/.../engine/TimelineInvariantsTest.kt`
- [ ] T006 Room v5 in `D/`: columns on `Entities.kt`, mapping in `Mappers.kt`, `PetruDatabase.kt` version 5 with `AutoMigration(from = 4, to = 5)`, exported `core/data/schemas/.../5.json`
- [ ] T007 [P] Migration test in `core/data/src/androidTest/kotlin/com/petruvideo/core/data/db/Migration4To5Test.kt`: a v4 project opens in v5 with every tool neutral; round trip of non-default values
- [ ] T008 Spike S1 on the emulator: extend `P/GainAudioProcessor.kt` with a pass-through log of the stream position offset at flush and the frames processed; confirm the clip time after a seek and with a 2x clip (R2, R3); record findings in `research.md`
- [ ] T009 [P] Add `P/PreviewEngine.setClipFxLive(clipId, fx)` and `setClipFadeLive(clipId, fade)` to the interface, `Media3PreviewEngine`, and the fake in `feature/editor/src/test/.../EditorTestFixtures.kt`; add `voiceFx` to `core/media/src/main/kotlin/com/petruvideo/core/media/pressure/PreviewQuality.kt` (false when Reduced) with its test
- [ ] T009a [P] Timing tests: a live `update` on `VoiceFxProcessor` and `GainAudioProcessor` takes effect within one output buffer (`core/media/src/test/.../preview/LiveUpdateTest.kt`), and an emulator playback test drags a setting with no dropout (`core/media/src/androidTest/.../LiveDragPlaybackTest.kt`) for SC-002
- [ ] T010 Create `P/VoiceFxProcessor.kt` as a pass-through chain holder (16-bit PCM, mono and stereo, live `update(fx)`, `onFlush` resets, no tail at end) and wire `[VoiceFxProcessor, ReverbAudioProcessor, GainAudioProcessor]` in `B/CompositionBuilder.kt` with `voiceFxs` map; keep existing behaviour unchanged and tests green
- [ ] T011a Audio entry shell in `E/ui/AudioToolList.kt`, `E/ui/ClipToolBar.kt`, `E/EditorContract.kt`, `E/EditorViewModel.kt`: one Audio button replaces Volume, Mute and Reverb buttons, the list opens and routes to the existing Volume, Mute and Reverb panels; each later panel task adds its own row; update the existing tool bar tests (`TrimAndAudioFlowTest`, `CoverWidthToolBarTest`, `AccessibilityTest`)
- [ ] T011 Run the full existing suite (`gradle --offline testDebugUnitTest verifyPaparazziDebug detekt`) and fix regressions

**Checkpoint**: model, persistence, builder plumbing ready.

---

## Phase 3: User Story 1 - Fade in and out (P1) - MVP

**Goal**: set fade in and out on a clip, hear the ramps, undo.

**Independent Test**: music clip, 2 s in and 3 s out; hear both; seek into a fade; set 0 and hear the original.

### Tests (write first, must fail)

- [ ] T012 [P] [US1] Command tests in `core/model/src/test/.../engine/AudioFxCommandsTest.kt` for `SetFade`: limits and message when the sum exceeds the length, rejects photos and soundless clips, undo and redo exact
- [ ] T013 [P] [US1] Rules tests in `core/model/src/test/.../engine/FadeRulesTest.kt`: split gives fade in to the first half and fade out to the second, duplicate and detach copy, replace keeps, render-time clamp after trim and speed change (stored values kept)
- [ ] T014 [P] [US1] Gain/fade tests in `core/media/src/test/.../preview/GainFadeTest.kt`: raised-cosine ramp shape, exact 0 at the start and 1 at the end, clip time after a seek, volume and fade multiply, no click (adjacent sample step bounded)
- [ ] T015 [P] [US1] View model tests in `feature/editor/src/test/.../EditorViewModelFadeTest.kt`: panel open/close, preview while dragging, commit is one undo step, dimmed reason, limited message

### Implementation

- [ ] T016 [US1] `SetFade` in `M/engine/AudioFxCommands.kt`; fade rules in `M/engine/SplitDuplicateDelete.kt`, `AudioClipCommands.kt`; effective-fade helper on the clips
- [ ] T017 [US1] Fade ramps in `P/GainAudioProcessor.kt` using the clip time from S1 (T008), and `CompositionBuilder` passing fades and clip timeline length; `setClipFadeLive` implemented
- [ ] T018 [US1] `E/EditorContract.kt`, `E/EditorViewModel.kt`: `AudioTool.Fade`, `FadePanelState`, events, limited message
- [ ] T019 [P] [US1] `E/ui/FadePanel.kt` (two sliders with numeric readouts, Reset, Done, 48 dp targets, semantics)
- [ ] T020 [P] [US1] Paparazzi golden and Compose test `FadePanelTest.kt`

---

## Phase 4: User Story 2 - Noise reduction (P1)

**Goal**: turn on Noise reduction with a strength and hear less background noise.

**Independent Test**: voice clip with steady noise; Medium lowers noise; Off equals the original.

### Tests (write first, must fail)

- [ ] T021 [P] [US2] DSP tests in `core/media/src/test/.../dsp/NoiseReductionDspTest.kt`: white noise plus a 440 Hz tone: noise power drops by at least 6, 12 and 20 dB-ish per strength (tolerance bands) while the tone stays within 3 dB; silence in, silence out; neutral when off; no allocation per block; strength change without a jump
- [ ] T022 [P] [US2] Command and view model tests: `SetNoiseReduction` (rejects photos and soundless clips, undo/redo) in `AudioFxCommandsTest.kt`, `EditorViewModelNoiseTest.kt`

### Implementation

- [ ] T023 [US2] `SetNoiseReduction` in `M/engine/AudioFxCommands.kt`
- [ ] T024 [US2] `X/NoiseReductionDsp.kt` (1024 frame FFT, 50% overlap, running-minimum noise floor, over-subtraction, smoothing, latency report); make T021 pass
- [ ] T025 [US2] Add the stage to `P/VoiceFxProcessor.kt` with latency compensation (drop leading latency frames after every flush, including a seek, where state is reset first; flush the remainder at end of stream); add a seek test; test A/V length equality in `core/media/src/androidTest/.../VoiceFxPlaybackTest.kt`
- [ ] T025a [US2] Chain-level neutral test in `core/media/src/androidTest/.../VoiceFxPlaybackTest.kt`: every tool off or neutral equals the input and nothing sounds after the clip end (SC-006)
- [ ] T026 [US2] `E/EditorViewModel.kt` state and events; `E/ui/NoiseReductionPanel.kt` (switch, Low/Medium/High chips, note); golden and Compose test
- [ ] T027 [US2] Listening check and tuning by ear of strengths on the Fold (manual)

---

## Phase 5: User Story 3 - Equalizer (P2)

**Goal**: pick a preset or drag Low, Mid, High and hear the tone change.

**Independent Test**: Bass boost, then High down; Reset returns to the preset.

### Tests (write first, must fail)

- [ ] T028 [P] [US3] DSP tests in `core/media/src/test/.../dsp/EqualizerDspTest.kt`: gain at 100 Hz, 1 kHz and 8 kHz matches the bands within 1 dB, flat equals input, stable at extremes, soft limit keeps within full scale, coefficient smoothing without a jump
- [ ] T029 [P] [US3] Command and view model tests: `SetEqualizer` and `ApplyEqPreset` (Custom after a band change, Reset to preset, rejects photos and soundless clips) in `AudioFxCommandsTest.kt`, `EditorViewModelEqTest.kt`

### Implementation

- [ ] T030 [US3] `SetEqualizer`, `ApplyEqPreset` in `M/engine/AudioFxCommands.kt`
- [ ] T031 [US3] `X/EqualizerDsp.kt` (low shelf 200 Hz, peaking 1 kHz, high shelf 4 kHz, smoothing, soft limit); stage in `P/VoiceFxProcessor.kt`
- [ ] T032 [US3] View model state and events; `E/ui/EqualizerPanel.kt` (preset chips, three sliders, Reset); golden and Compose test
- [ ] T033 [US3] Tune preset values by ear (manual)

---

## Phase 6: User Story 4 - Voice effects (P2)

**Goal**: turn on Echo, Robot, Pitch and Bass, alone or stacked, and hear them live.

**Independent Test**: Pitch +5, add Echo, add Robot; turn each off; no gaps while dragging.

### Tests (write first, must fail)

- [ ] T034 [P] [US4] DSP tests in `core/media/src/test/.../dsp/`: `PitchShiftDspTest` (detected frequency ratio within 2% for +12, +5, -12, same duration, 0 semitones within 1 dB of the input), `EchoDspTest` (tap times and levels, repeats count, nothing after the input ends), `RobotDspTest` (carrier energy at 70 Hz, intensity 0 equals input), `BassDspTest` (low-frequency gain grows with amount, bounded output)
- [ ] T035 [P] [US4] Command and view model tests: `SetVoiceFx` in `AudioFxCommandsTest.kt` (rejects photos and soundless clips), effects keep settings when off, `EditorViewModelVoiceTest.kt`

### Implementation

- [ ] T036 [US4] `SetVoiceFx` in `M/engine/AudioFxCommands.kt`
- [ ] T037 [P] [US4] `X/PitchShiftDsp.kt` (two-head granular, live ratio, latency report), spike S2: listen on the Fold, switch to WSOLA behind the same class if quality is poor
- [ ] T038 [P] [US4] `X/EchoDsp.kt`, `X/RobotDsp.kt`, `X/BassDsp.kt`
- [ ] T039 [US4] Stages in `P/VoiceFxProcessor.kt` in the fixed order (bass, pitch, robot, echo), bypass of noise reduction, pitch and echo only under `PreviewQuality.voiceFx` (EQ, bass, robot, fade and volume stay on), live update; pressure notice text in the editor view model
- [ ] T040 [US4] View model state and events; `E/ui/VoiceEffectsPanel.kt` (four sections with switches and sliders); golden and Compose test
- [ ] T041 [US4] Five clips with several effects on the Fold and a mid-range phone: record fps and CPU in `quickstart.md` (SC-003, R1)

---

## Phase 7: User Story 5 - One audio group, saved, exported and unfolded (P3)

**Goal**: one Audio entry, list, badges, inspector, tabletop, persistence and export match.

**Independent Test**: set every tool on a clip, save, reopen, export, compare to the preview; unfold and half-fold.

### Tests (write first, must fail)

- [ ] T042 [P] [US5] List tests: rows and state hints for every tool, 2x font, screen reader order, in `feature/editor/src/androidTest/`
- [ ] T043 [P] [US5] Badge tests in `feature/editor/src/test/.../ClipBadgeTest.kt`: badges for each tool, collapse into "+N" when narrow
- [ ] T044 [P] [US5] Export parity test in `core/export/src/androidTest/.../AudioFxExportTest.kt`: a clip with fade, noise reduction, EQ and effects exports with the right length, audio present, fade start level below 5% of the body level, spectrum check against the preview chain
- [ ] T045 [P] [US5] Persistence test in `feature/editor/src/test/.../AudioFxPersistenceTest.kt`: save and reload all values; a force-close case with the 5 s autosave bound

### Implementation

- [ ] T046 [US5] Row state hints and back-to-list behaviour in `E/ui/AudioToolList.kt` for all tools (the Audio button and routing exist from T011a)
- [ ] T047 [US5] `E/ui/ClipBadge.kt` badges "Fd", "Nr", "Eq", "Vx" with collapse; ramp marker at faded ends
- [ ] T048 [US5] Inspector sections in `E/ui/expanded/InspectorPanel.kt` and tabletop placement in `E/ui/tabletop/TabletopEditorLayout.kt`; open tool and preview value survive fold and rotation
- [ ] T049 [US5] Goldens: list, each panel on compact, expanded and tabletop in `EditorScreenshotTest.kt`, `EditorFoldScreenshotTest.kt`, `CoverScreenshotTest.kt`
- [ ] T050 [US5] Emulator run of all connected suites (core:data, core:media, core:export, feature:editor, feature:projects); stop the emulator afterwards

---

## Phase 8: Polish and Fold checks

- [ ] T051 [P] Originals-unchanged test extension in `OriginalsUnchangedTest` for all new tools (SC-009)
- [ ] T052 Run `gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and fix findings
- [ ] T053 Fold manual checks from `quickstart.md` items 1 to 8 (A/V sync within 40 ms, seek into a fade, tail cut, fold/unfold, 2x font) plus a first-attempt timing check: fade or noise reduction on within 15 s of selecting a clip (SC-001)
- [ ] T054 Designer review of the Audio list, the five panels and the badges; record deviations in `contracts/ui-screens.md`
- [ ] T055 Update `quickstart.md` Results so far and mark the tasks

---

## Dependencies

- Phase 2 blocks everything. T008 (S1) gates T017.
- US1 and US2 are P1 and independent after Phase 2; US3 and US4 are independent of each other; each adds its stage to `VoiceFxProcessor.kt` (same file: do them one at a time).
- US5 depends on the panels it lists (US1 to US4) for the list rows, but its tool bar change can land after US1.
- Polish last.

## Parallel examples

- After T011: T012 to T015 together; T021 and T022 together; T028 and T029 together; T034 and T035 together.
- DSP classes T037 and T038 are separate files and run together.

## Implementation strategy

MVP is Phase 1 to 3 (Fade). Then US2, US3, US4, then US5 grouping and export parity. Keep the emulator off except for runs, and stop it afterwards.
