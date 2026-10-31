# Tasks: Audio Tools - Mute and Reverb

**Input**: Design documents from `/specs/003-audio-mute-reverb/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-screens.md, quickstart.md

**Tests**: Included (Principle V: engine, presets, and DSP test-first; migration test; view model, Compose, and golden checks).

**Organization**: Grouped by user story. `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `P` = `core/media/src/main/kotlin/com/petruvideo/core/media/preview`, `E` = `feature/editor/src/main/kotlin/com/petruvideo/feature/editor`, `D` = `core/data/src/main/kotlin/com/petruvideo/core/data/db`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallel (different files, no dependencies)
- **[Story]**: US1 reverb on/off and presets, US2 reverb settings, US3 mute for video clips, US4 audio tool group, badge, inspector, tabletop

---

## Phase 1: Setup

- [X] T001 Run the 002 baseline (`gradle --offline :core:model:test testDebugUnitTest detekt`) and confirm it is green
- [X] T002 [P] Add a Reverb icon and an on/off switch styled for the dark theme in `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/` (icons and components only)

---

## Phase 2: Foundational (blocks all stories)

- [X] T003 [P] Write failing tests in `core/model/src/test/kotlin/com/petruvideo/core/model/ReverbTest.kt`: defaults (off, MediumRoom, 50, 1500, 50, 20, 30), the five presets' values from the table in `data-model.md`, `Custom` when numbers match no preset, clamping of every range
- [X] T004 Add `Reverb`, `ReverbPreset`, and `ReverbPresets` in `M/ReverbPresets.kt`; add `reverb` to `AudioClip` and `VideoClip` and `muted` to `VideoClip` in `M/Models.kt`; make T003 pass (preset values are tuned by ear later, keep them as constants)
- [X] T005 [P] Extend `M/engine/TimelineInvariants.kt` with the reverb ranges and a test in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/TimelineInvariantsTest.kt`
- [X] T006 Room v3 in `D/`: columns on `Entities.kt`, mapping in `Mappers.kt` (a `Reverb` to and from the seven columns), `PetruDatabase.kt` version 3 with `AutoMigration(from = 2, to = 3)`, exported `core/data/schemas/.../3.json`
- [X] T007 [P] Migration test in `core/data/src/androidTest/kotlin/com/petruvideo/core/data/db/Migration2To3Test.kt`: a v2 project opens in v3 with reverb off and not muted; round trip of non-default values
- [X] T009 Run the whole 002 suite again and fix regressions (`gradle --offline testDebugUnitTest verifyPaparazziDebug detekt`)

**Checkpoint**: model, persistence, and processor plumbing ready.

---

## Phase 3: User Story 1 - Add reverb to a clip (P1) - MVP

**Goal**: turn Reverb on for a clip, pick a preset, hear it, undo it.

**Independent Test**: voice clip, Hall on, hear the tail, off, dry again; undo and redo each.

- [X] T008 [US1] Spike S2 (first in US1, gates T015) on the emulator then the Fold: a pass-through `ReverbAudioProcessor` in `P/ReverbAudioProcessor.kt` attached with `Effects(...)` next to the gain processor in `P/Media3PreviewEngine.kt`; confirm formats (mono, stereo, sample rates), seek and flush behaviour, and that the tail ends with the input; record findings in `specs/003-audio-mute-reverb/research.md`

### Tests (write first, must fail)

- [X] T010 [P] [US1] DSP tests in `core/media/src/test/kotlin/com/petruvideo/core/media/preview/ReverbDspTest.kt`: impulse response decays about 60 dB in the decay time, mix 0 equals the input, mono and stereo, no sample beyond full scale, no output after input ends, state reset on flush, no allocation per block, and reverb then gain 0 gives silence
- [X] T011 [P] [US1] Command tests in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/AudioToolCommandsTest.kt`: `SetReverbEnabled` and `ApplyReverbPreset` on video clips with sound and audio clips, rejects photos, soundless or detached video, unknown ids, no-ops; settings kept when turned off; other clips untouched; undo and redo exact
- [X] T012 [P] [US1] View model tests in `feature/editor/src/test/kotlin/com/petruvideo/feature/editor/EditorViewModelReverbTest.kt`: panel opens and closes, toggle and preset are one undo step each, dimmed for photos and soundless clips with a reason, selecting another clip closes the panel

### Implementation

- [X] T013 [US1] `SetReverbEnabled` and `ApplyReverbPreset` in `M/engine/AudioToolCommands.kt`
- [X] T014 [P] [US1] Pure Kotlin `ReverbDsp` in `P/ReverbDsp.kt` (8 damped combs and 4 all-pass per channel, pre-delay line, equal-power mix, decay-to-feedback mapping from research 2, preallocated buffers); make T010 pass
- [X] T015 [US1] Make `P/ReverbAudioProcessor.kt` run `ReverbDsp` (16-bit PCM, `onFlush` resets state, no tail at end of stream); add `setClipReverbLive(clipId, reverb)` to `P/PreviewEngine.kt` and implement it in `P/Media3PreviewEngine.kt` (one processor per audio-carrying item, built from the clip's stored reverb, placed before the gain processor so volume applies after reverb)
- [X] T016 [US1] `E/EditorContract.kt` and `E/EditorViewModel.kt` (reuse the 002 sheet and live-restore helpers where they fit): `ClipTool.Reverb`, `ReverbPanelState`, events `OpenPanel`, `ToggleReverb`, `PickPreset`, `Dismiss`, disabled reasons; add a fake `setClipReverbLive` to `feature/editor/src/test/.../EditorTestFixtures.kt`
- [X] T017 [P] [US1] `E/ui/ReverbPanel.kt`: header with switch and Done, preset chips (Custom shows selected when no match), 48 dp targets and semantics
- [X] T018 [US1] Add Reverb to `E/ui/ClipToolBar.kt` (video with sound and audio clips) and show the panel above the bar in `E/ui/EditorScreen.kt` (preview shrinks as for the 002 sheet) and `E/ui/tabletop/TabletopEditorLayout.kt`
- [X] T019 [P] [US1] Paparazzi goldens for the panel (off, on with Hall) in `feature/editor/src/test/.../EditorScreenshotTest.kt` and a Compose test in `feature/editor/src/androidTest/.../ReverbPanelTest.kt` (toggle, preset tap, Done)
- [ ] T020 [US1] Device check on the emulator and the Fold: reverb audible, off is dry, tail cut at clip end across a boundary; time a first-attempt reverb on under 15 s (SC-001); record in `specs/003-audio-mute-reverb/checklists/validation.md`

**Checkpoint**: Reverb with presets works end to end and ships as the MVP.

---

## Phase 4: User Story 2 - Tune the reverb settings (P1)

**Goal**: five settings, heard live, with Custom and Reset.

**Independent Test**: drag each setting during playback, hear it under 100 ms with no gap; Mix 0 equals dry; reopen the project and find the values.

### Tests (write first, must fail)

- [X] T021 [P] [US2] `SetReverbSettings` tests in `AudioToolCommandsTest.kt`: clamping, preset recomputed (Custom when no match, matching preset when equal), `enabled` kept, one undo step, Reset to preset
- [X] T022 [P] [US2] DSP tests in `ReverbDspTest.kt`: parameter changes are smoothed (no sample jump above a threshold when mix, decay, or damping change mid-stream), room size changes apply on commit (not during the drag), decay mapping at 0.2 s and 10 s
- [X] T023 [P] [US2] View model tests in `EditorViewModelReverbTest.kt`: `PreviewSetting` goes live to the engine without an edit, `CommitSetting` is one undo step, dismiss restores the live value, Reset returns to the preset

### Implementation

- [X] T024 [US2] `SetReverbSettings` in `M/engine/AudioToolCommands.kt`
- [X] T025 [US2] Smoothed live parameters in `P/ReverbDsp.kt` and `P/ReverbAudioProcessor.kt` (volatile `Reverb` read per block, about 20 ms ramps; Room size applied on commit, as FR-004 allows)
- [X] T026 [US2] View model events `PreviewSetting`, `CommitSetting`, `Reset` in `E/EditorViewModel.kt` with `restoreLiveReverb` like the 002 live volume
- [X] T027 [P] [US2] Mix and Decay sliders and the "More" expander (Room size, Damping, Pre-delay), value labels and Reset in `E/ui/ReverbPanel.kt`
- [X] T028 [P] [US2] Goldens for the panel with More open and Custom selected; Compose test for dragging a slider and Reset in `ReverbPanelTest.kt`
- [ ] T029 [US2] Device check: each setting heard live under 100 ms with no click or gap; save, force-stop, reopen keeps the values; record in `checklists/validation.md`

---

## Phase 5: User Story 3 - Mute a clip's sound (P2)

**Goal**: Mute and Unmute for video clips with sound.

**Independent Test**: mute a video with music over it, only the music plays; unmute restores volume and reverb; undo.

- [X] T030 [P] [US3] Tests in `AudioToolCommandsTest.kt` and `core/model/src/test/.../engine/DetachAudioTest.kt`: `SetVideoMuted` rejects clips without sound and photos, mute independent of volume and reverb, detach of a muted clip gives a muted audio clip, split and duplicate keep `muted`
- [X] T031 [US3] `SetVideoMuted` in `M/engine/AudioToolCommands.kt`; update `DetachAudio` in `M/engine/AudioClipCommands.kt` (new clip `muted` from the video, video `muted` cleared) and `Split`, `Duplicate` in `M/engine/SplitDuplicateDelete.kt`
- [X] T032 [US3] `P/Media3PreviewEngine.kt`: a muted video clip is played with its sound removed; settings kept
- [X] T033 [US3] Show Mute (Unmute while muted) for video clips in `E/ui/ClipToolBar.kt`, route it in `E/EditorViewModel.kt` (`useTool` for video clips), dimmed for clips without sound
- [X] T034 [P] [US3] Muted indicator on video clips in `E/ui/VideoLaneView.kt` (reuse the audio lane's muted badge) and a Compose test for the tool label toggle
- [ ] T035 [US3] Device check: scenarios 5 and 6 of the quickstart; record in `checklists/validation.md`

---

## Phase 6: User Story 4 - Audio tools together, badge, inspector, tabletop (P3)

**Goal**: one group of audio tools, reverb badge, inspector sections, panel below the hinge.

**Independent Test**: reverb on, see the badge, unfold and edit in the inspector, half-fold and open the panel below the hinge.

- [X] T036 [P] [US4] Reverb badge "Rv" in `E/ui/ClipBadge.kt` (alongside speed and volume) and use it in `E/ui/VideoLaneView.kt` and `E/ui/AudioLaneView.kt`
- [X] T037 [P] [US4] Volume, Mute, and Reverb sections (all five settings) in `E/ui/expanded/InspectorPanel.kt`; use the bottom panel below 840 dp in `E/ui/expanded/ExpandedEditorLayout.kt`
- [X] T038 [P] [US4] Tool bar order Volume, Mute, Reverb for video and audio clips in `E/EditorViewModel.kt` (`clipToolsFor`) and the tabletop panel placement in `E/ui/tabletop/TabletopEditorLayout.kt`
- [X] T039 [US4] Tests: open panel survives configuration change (view model and an androidTest), arrangement checks for the panel in each layout
- [X] T040 [P] [US4] Goldens for badges, the inspector sections, and the tabletop panel
- [ ] T041 [US4] Fold check: badge, inspector, tabletop, fold and rotate with the panel open, split-screen; record in `checklists/validation.md`

---

## Phase 7: Polish and cross-cutting

- [X] T042 [P] Reverb on split, duplicate, replace, detach, and sped clips: tests in the engine suites and an emulator playback test (seek into a reverb clip, split mid-tail) in `core/media/src/androidTest/.../PreviewEngineTest.kt`
- [X] T043 [P] Pressure monitor bypasses reverb under heavy pressure with a notice (`core/media/.../pressure/` and `E/EditorViewModel.kt`), with a test
- [X] T044 [P] Accessibility with large font on the panel and inspector (pattern of `AccessibilityTest`) and screen reader values (FR-019)
- [X] T045 [P] Sources byte-identical (SC-008) and save, force-stop, reopen keeps mute and reverb (SC-005): androidTest in `core/data`
- [X] T046 Missing media keeps reverb and mute editable (edge case), test in `feature/editor/src/test/`
- [ ] T047 Five reverb clips: 30 fps and no dropouts on the Fold, CPU on a mid-range phone (SC-003, risk R1); record the result or open item
- [X] T048 Update `specs/003-audio-mute-reverb/quickstart.md` with the final commands and results
- [X] T049 Run everything: `:core:model:test`, `testDebugUnitTest`, `verifyPaparazziDebug`, `detekt`, `lintDebug`, `connectedDebugAndroidTest`; fix failures
- [ ] T050 Designer review of the reverb panel and inspector (SC-007); record deviations or the open item

---

## Dependencies

- Phase 1, then Phase 2 (T004 gates everything; the T008 spike, now first in Phase 3, gates T015).
- US1 depends on Phase 2. US2 depends on US1 (panel, processor, view model). US3 depends on Phase 2 only (can run beside US1). US4 depends on US1 and US3.
- Within a story: tests, commands, DSP and preview, view model, UI, device check.

## Parallel examples

- Phase 2: T003, T005, T007 after T004 lands.
- US1: T010, T011, T012 together; T014 and T017 together.
- US2: T021, T022, T023 together; T027 and T028 together.
- US4: T036, T037, T038 together.

## Implementation strategy

1. MVP: Phases 1, 2, and 3 (Reverb with presets) and the device check.
2. Add US2 (settings), then US3 (mute), then US4, then Polish.
3. Stop at any checkpoint; each phase leaves the app working.
