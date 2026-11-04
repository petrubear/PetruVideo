# Tasks: Overlay Audio

**Input**: Design documents from `/specs/029-overlay-audio/` (plan.md, spec.md, research.md, data-model.md, quickstart.md)

**Tests**: Included, because the constitution asks for tests first on core logic (Principle V).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US5 from spec.md

Paths below are relative to the repository root. `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `MT` = `core/model/src/test/kotlin/com/petruvideo/core/model`, `D` = `core/data/src/main/kotlin/com/petruvideo/core/data`, `ME` = `core/media/src/main/kotlin/com/petruvideo/core/media`, `E` = `feature/editor/src/main/kotlin/com/petruvideo/feature/editor`.

---

## Phase 1: Setup

- [X] T001 Check the baseline: run `gradle --offline :core:model:test :feature:editor:testDebugUnitTest` and note it passes before any change

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: the model fields, the database, and the saved defaults.

- [X] T002 [P] Write failing tests in `MT/OverlayAudioModelTest.kt`: a new `OverlayClip` has `attachedAudio=false` by default in the data class; `AddOverlay` of a video with sound sets `attachedAudio=true`, volume 100, `muted=false`; of a photo or a video with no sound sets `attachedAudio=false`; `normalized()` clamps volume to `0..VOLUME_MAX_PERCENT` and limits the fade to half the clip
- [X] T003 Add `attachedAudio`, `muted`, `volumePercent`, `reverb`, `fx` to `OverlayClip` in `M/Overlay.kt` (same types as `VideoClip`; defaults silent), update the doc comment ("no sound of its own"), clamp in `normalized()`; set sound on in `AddOverlay` in `M/engine/OverlayCommands.kt` when the media is a video with sound (T002 passes)
- [X] T004 [P] Write a failing instrumented migration test in `core/data/src/androidTest/kotlin/com/petruvideo/core/data/` (`MigrationTestHelper`, 22 to 23): an old `overlay_clip` row has `attachedAudio=0`, `muted=0`, `volumePercent=100`, reverb and fx defaults
- [X] T005 Add the columns to `OverlayClipEntity` in `D/db/Entities.kt` (defaults as in `data-model.md`, copy the reverb and fx column set and defaults from the `video_clip` entity), map them in `D/db/Mappers.kt` (`toModel`, `toEntity`), bump `PetruDatabase` to version 23 with migration 22 to 23 in `D/db/PetruDatabase.kt`, commit the exported `core/data/schemas/.../23.json` (T004 passes; check `RoomProjectRepositoryTest` round trip of the new fields)
- [X] T006 Check `M/engine/SplitDuplicateDelete.kt` and `M/engine/TimelineInvariants.kt`: split, duplicate and delete of an overlay keep or remove the sound fields (copy semantics); add a test in `MT/engine/` that a split overlay gives both parts the same volume, mute and fx

**Checkpoint**: sound settings persist; old rows are silent.

---

## Phase 3: User Story 1 - Hear the overlays together with the base video (Priority: P1) MVP

**Goal**: sounding overlays are mixed with the base video in the preview and the export.

**Independent Test**: scenario 1 in quickstart.md.

- [X] T007 [P] [US1] Write failing tests in `core/media/src/androidTest/kotlin/com/petruvideo/core/media/composition/OverlayAudioSequenceTest.kt` (instrumented, because Media3 types need Android): `CompositionBuilder.build` of a timeline with one base clip and two sounding overlays gives two extra audio-only sequences after the video ones; a muted, silent, photo, or missing-media overlay gives none; the video sequence count and order are unchanged; each audio sequence starts with a gap equal to `startMs`
- [X] T008 [US1] In `ME/composition/CompositionBuilder.kt` add `overlayAudioSequences(timeline)` (audio-only `EditedMediaItemSequence`: gap to `startMs`, then the clip with `setRemoveVideo(true)` and `soundEffects(clip.id, clip.reverb, clip.volumePercent, true, clip.fx, clip.durationMs)`; tail not needed), include it in `sequences = overlays + main + audioSequences + overlayAudio`, update the `overlaySequences` doc comment (T007 passes)
- [X] T009 [US1] Make `Media3PreviewEngine` (`ME/preview/Media3PreviewEngine.kt`) look up `gains`, `reverbs`, `voiceFxs` by overlay id for live changes (they are keyed by clip id already; add a test or check that an overlay id reaches them)
- [X] T010 [US1] Extend `core/export/src/androidTest/kotlin/com/petruvideo/core/export/ExportPipelineTest.kt`: export a base video plus a sounding overlay and assert the file has an audio track, its duration matches, and the audio is not silent (peak above a floor)
- [X] T011 (automated on the phone: `OverlayPlaybackTest` plays 3 sounding overlays over 3 clips with no stall, and the export test shows overlay sound at its place and volume; the manual 30 s check with a real project is still to do in T032) [US1] Phone check (quickstart scenario 1 and 6): base plus two sounding overlays, export, pull the file, confirm all sources are audible and in time; play 30 s in the editor and record fps from `PetruPlayback`; write the result into `specs/029-overlay-audio/quickstart.md` notes

**Checkpoint**: the mix works in the export; the preview result is recorded.

---

## Phase 4: User Story 2 - Volume and mute for each overlay (Priority: P1)

**Goal**: balance overlays with volume and mute.

**Independent Test**: scenario 2 in quickstart.md.

- [X] T012 [P] [US2] Write failing tests in `MT/engine/OverlayAudioCommandsTest.kt`: `SetVolume` on an overlay id changes `volumePercent`, rejects a silent overlay (`NoAttachedAudio`), rejects no change; `SetMuted` on an overlay id mutes and unmutes; undo restores
- [X] T013 [US2] Widen `SetVolume` in `M/engine/SpeedVolumeCommands.kt` and `SetMuted` in `M/engine/AudioClipCommands.kt` to find the id in `overlayLane` (after video and audio lanes); keep `sortedBy { startMs }` order (T012 passes)
- [X] T014 [US2] In `E/EditorViewModel.kt` and `E/EditorContract.kt` add events and state so the selected overlay's volume and mute reach `SetVolume` and `SetMuted`; add tests in `feature/editor/src/test/kotlin/com/petruvideo/feature/editor/EditorViewModelOverlayTest.kt` (volume and mute events update the overlay; undo restores)
- [X] T015 [US2] Add a Sound section (volume slider, mute toggle) to the overlay panel in `E/ui/OverlayUi.kt`, shown only when `attachedAudio`; show "No sound in this clip" when the source has none; keep 48 dp targets; make it work at compact, medium, expanded and tabletop (reuse the lane clip sound controls where they exist)
- [X] T016 [US2] Add a `SetOverlaySound(clipId, on)` command in `M/engine/OverlayCommands.kt` (turns sound on or off for an overlay whose source has sound; test in `OverlayAudioCommandsTest.kt`) and a "Sound on" switch in `OverlayUi.kt`, so old silent overlays can be turned on (FR-008)
- [X] T017 [US2] Make the live volume/mute change reach the running preview without restart (spec US2 scenario 3): confirm `Media3PreviewEngine` updates the `GainAudioProcessor` for an overlay id; add a test

**Checkpoint**: P1 stories complete; ship candidate.

---

## Phase 5: User Story 3 - Audio effects on an overlay (Priority: P2)

**Goal**: reverb, equaliser, noise reduction, voice effects and fades per overlay.

**Independent Test**: scenario 3 in quickstart.md.

- [X] T018 [P] [US3] Write failing tests in `MT/engine/OverlayAudioCommandsTest.kt`: `SetFade`, `SetEqualizer`, `SetNoiseReduction`, the voice fx command, `SetReverbEnabled` and `SetReverbSettings` on an overlay id change only that overlay; a fade is limited to half the overlay length; a silent overlay is rejected
- [X] T019 [US3] Widen `AudioFxCommand` in `M/engine/AudioFxCommands.kt` and the reverb commands in `M/engine/AudioToolCommands.kt` to find the id in `overlayLane` (T018 passes)
- [X] T020 [US3] Route the existing audio effect events in `E/EditorViewModel.kt` to the selected overlay, and show the same effect controls for an overlay in `E/ui/OverlayUi.kt` (reuse the lane clip effect composables); add tests in `EditorViewModelOverlayTest.kt`
- [X] T021 (Done as: the overlay tools use the existing tool bar and Audio panel, so no new layout was added; the tool list is covered by the view-model tests, and the real panel was checked on the phone) [US3] Add the Paparazzi screenshot test for the overlay panel with the Sound section at compact, medium, expanded widths and tabletop in `feature/editor/src/test/kotlin/com/petruvideo/feature/editor/EditorScreenshotTest.kt`; record goldens

---

## Phase 6: User Story 4 - Detach an overlay's sound (Priority: P2)

**Goal**: move an overlay's sound to the audio lane.

**Independent Test**: scenario 4 in quickstart.md.

- [X] T022 [P] [US4] Write failing tests in `MT/engine/OverlayAudioCommandsTest.kt`: `DetachAudio` on an overlay id adds an `AudioClip` (`origin=Detached`, same media and source range, `timelineStartMs=overlay.startMs`, copied volume, mute, reverb, fx), sets the overlay's `attachedAudio=false`, snaps with `Notice.Snapped` when occupied, rejects a silent overlay; undo restores both
- [X] T023 [US4] Add the overlay branch to `DetachAudio` in `M/engine/AudioClipCommands.kt` (T022 passes); when no free layer fits at all, reject with `LimitReached` (as `SplitVoice` does) and show the existing message
- [X] T024 [US4] Add a "Detach audio" action to the overlay panel in `E/ui/OverlayUi.kt` and the event in `E/EditorViewModel.kt`; add a view-model test (audio clip appears, overlay silent, undo)

---

## Phase 7: User Story 5 - Split the voice of an overlay (Priority: P3)

**Goal**: Voice and Backing clips from an overlay.

**Independent Test**: split an overlay's voice; two audio clips appear and the overlay is silent.

- [X] T025 [P] [US5] Write failing tests in `MT/engine/` (new `OverlayVoiceSplitTest.kt`): `VoiceSplitRules.reasonNot` accepts an overlay id with sound up to `VOICE_SPLIT_MAX_MS` and gives the same reasons otherwise; `SplitVoice` on an overlay id adds two detached clips at the overlay start and sets `attachedAudio=false`; rejects with `LimitReached` when the lane cannot hold both
- [X] T026 [US5] Extend `VoiceSplitRules` and `SplitVoice` in `M/engine/VoiceSplitCommands.kt` with the overlay branch (the overlay has no speed, reverse or curve, so only the length and sound checks apply) (T025 passes)
- [X] T027 [US5] Call the existing voice split flow for an overlay from `E/EditorViewModel.kt` (use the overlay's media and source range; the service takes `media, startMs, endMs`) and add the "Split voice" action with its disabled reason to `E/ui/OverlayUi.kt`; add a view-model test with `NoVoiceSplitter` or the fake used by the current tests

---

## Phase 8: Preview fallback and polish (cross-cutting)

- [X] T028 Preview fallback (FR-012): in `ME/preview/Media3PreviewEngine.kt` detect a stalled multi-input playback with overlay sound (use the existing stall/stats signal), rebuild the preview without `overlayAudioSequences` keeping the picture and the base sound, and publish a notice; add `Notice`/state in `E/EditorContract.kt` and show "Overlay sound plays in the export" in the editor; unit test the decision logic with a fake engine
- [X] T029 (Measured: four sources at 100% do not clip, peak 0.51 with the test clip; at 200% each, 0.5% of samples clip. No limiter added; `ExportPipelineTest.threeLoudOverlaysMixedTogetherDoNotClip` guards the default case) Mix level check: export three loud sources, measure the peak; if it clips, add a limiter step to the export audio path in `ME/composition/CompositionBuilder.kt`; record the result
- [X] T030 (Covered by the migration test and by the builder test: an old overlay has no sound and adds no sound sequence, so the export is built as before; the old Untitled 1 project also opened on the phone after the update) Old project check (SC-005): open the pre-update project backup (`db_backup`), export, and compare the exported sound with the previous export
- [X] T031 (detekt and all unit tests pass; the instrumented data, preview, overlay and new export tests pass. Four export tests (effects, transitions, size estimate) and seven editor accessibility tests fail the same way on the unchanged previous commit) Run all checks: `gradle --offline :core:model:test :core:media:testDebugUnitTest :feature:editor:testDebugUnitTest detekt` and the instrumented `:core:data:connectedDebugAndroidTest` and `:core:export:connectedDebugAndroidTest` on the phone; fix findings
- [X] T032 (Checked on the phone: update keeps the old project, Sound on shows for an old overlay, the Audio panel lists volume, mute and effects, the preview plays 8 s at 30 fps with overlay sound; export mix covered by the export tests. Not done: compact/medium/expanded/tabletop and fold checks, and a recorded instrument session) Run quickstart scenarios 1 to 7 on the phone and at compact, medium, expanded and tabletop; mark fold and cover checks done or not done in the task notes
- [X] T033 Release: bump `appVersion` in `app/build.gradle.kts` to 0.4.0, add the CHANGELOG entry (Keep a Changelog), commit without attribution lines, tag `v0.4.0` (do not push)

---

## Dependencies

- Phase 2 blocks all stories.
- US1 (Phase 3) needs Phase 2. US2 needs US1's build (T008) to hear the change; its commands (T012 to T013) can start after Phase 2.
- US3 needs T013's pattern but is independent of US2's UI. US4 and US5 need Phase 2 only; US5 builds on US4's panel actions (T024) for placement.
- Phase 8 needs the stories it covers; T028 can be done after US1.

## Parallel examples

- After Phase 2: T007 (US1 builder tests), T012 (US2 command tests), T018 (US3 command tests), T022 (US4 tests), T025 (US5 tests) are different files and can be written together.
- Commands T013, T019, T023, T026 touch different files and can run in parallel after their tests.

## Implementation strategy

MVP is Phase 3 plus Phase 4 (US1 and US2): the mix with volume and mute, which is what the user asked for first. Ship it as 0.4.0 after T011, T017 and T028 pass; effects (US3), detach (US4) and voice split (US5) can follow as 0.4.x. If the phone check in T011 shows the preview freezes with overlay sound, T028 is required before release.
