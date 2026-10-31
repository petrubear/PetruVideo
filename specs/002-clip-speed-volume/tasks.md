# Tasks: Clip Speed and Volume

**Input**: Design documents from `/specs/002-clip-speed-volume/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-screens.md, quickstart.md

**Tests**: Included. The plan requires test-first on the engine (Principle V), a Room migration test, view model tests, Compose and accessibility checks, and Paparazzi goldens.

**Organization**: Grouped by user story. Paths are relative to the repo root; `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `E` = `feature/editor/src/main/kotlin/com/petruvideo/feature/editor`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependencies)
- **[Story]**: US1 volume, US2 speed, US3 audio-clip speed and volume, US4 badges, inspector, tabletop

---

## Phase 1: Setup

- [X] T001 Run the 001 baseline (`gradle --offline :core:model:test testDebugUnitTest detekt`) and confirm it is green before changing anything
- [X] T002 [P] Add Speed and Volume icons if missing and a boost color token for the slider in `core/designsystem/src/main/kotlin/` (icons and slider styling only)

---

## Phase 2: Foundational (blocks all stories)

**Purpose**: the data model, derived length, persistence, and the playback spike. No story can start before these.

- [X] T004 [P] Write failing JVM tests for the derived length in `core/model/src/test/kotlin/com/petruvideo/core/model/ClipLengthTest.kt`: `sourceLengthMs`, `lengthMs = round(source*1000/speedMilli)`, photos unaffected, defaults 1000 and 100
- [X] T005 Add `speedMilli` (default 1000) and `volumePercent` (default 100) to `VideoClip` and `AudioClip` in `M/Models.kt`; add `sourceLengthMs`; make `lengthMs` derived (data-model.md); make T004 pass
- [X] T006 Grep every use of `sourceEndMs - sourceStartMs` and fix each to use `lengthMs` or `sourceLengthMs` as intended (risk R3), across `core/`, `feature/` and tests; list the findings in the commit message
- [X] T007 [P] Extend `M/engine/TimelineInvariants.kt` with the new invariants (ranges, minimum derived length, photos speed 1000, total duration equals the sum of `lengthMs`) and cover them in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/TimelineInvariantsTest.kt`
- [X] T008 Room v2 in `core/data/src/main/kotlin/com/petruvideo/core/data/db/`: add `speedMilli` and `volumePercent` columns (defaults 1000 and 100) to the video and audio clip entities in `Entities.kt`, map them in `Mappers.kt`, bump `PetruDatabase.kt` to version 2 with the 1-to-2 migration, export `core/data/schemas/` v2
- [X] T009 [P] Migration test in `core/data/src/androidTest/kotlin/com/petruvideo/core/data/db/MigrationTest.kt`: a v1 project opens in v2 with speed 1000 and volume 100 and its clips intact; round-trip test for non-default values
- [X] T010 Run the whole 001 JVM suite again and fix regressions caused by the derived length (existing property tests must still pass)

**Checkpoint**: model, invariants, and persistence ready; spike result known.

---

## Phase 3: User Story 1 - Change a clip's volume (P1) - MVP

**Goal**: drag a volume slider on a clip with sound, hear it live, undo it.

**Independent Test**: music over a video with sound; set the music to 30%; the video's own sound is unchanged; undo restores the balance.

### Tests (write first, must fail)

- [X] T011 [P] [US1] `SetVolume` tests in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/SpeedVolumeCommandsTest.kt`: applies to a video clip with attached sound and to an audio clip, clamps 0 to 200, rejects photos, a video without attached sound, and unknown ids, leaves other clips untouched, independent of `muted`, undo and redo exact
- [X] T012 [P] [US1] View model tests in `feature/editor/src/test/kotlin/com/petruvideo/feature/editor/EditorViewModelVolumeTest.kt`: sheet open and dismiss, `PreviewValue` does not touch history, `CommitValue` creates one undo step, `Reset` returns to the default as one step, `canSetVolume` false for detached or soundless video

### Implementation

- [X] T013 [US1] Add `SetVolume` in `M/engine/SpeedVolumeCommands.kt`
- [X] T014 [P] [US1] Add `GainAudioProcessor` with a volatile gain and a soft limiter above 100% in `core/media/src/main/kotlin/com/petruvideo/core/media/preview/GainAudioProcessor.kt`, with a JVM test for the gain maths and limiter in `core/media/src/test/`
- [X] T015 [US1] Add `setClipVolumeLive(clipId, percent)` to `PreviewEngine.kt` and implement it in `Media3PreviewEngine.kt`: one processor per audio-carrying item; rebuilds use the stored `volumePercent` (0% silences, mute stays separate)
- [X] T016 [US1] `EditorContract.kt` and `EditorViewModel.kt`: `SheetState`, `canSetVolume`, `selectedVolumePercent`, events `OpenSheet`, `PreviewValue`, `CommitValue`, `Reset`, `Dismiss`; keep the open sheet and preview value in saved state so it survives configuration changes
- [X] T017 [P] [US1] `E/ui/SpeedVolumeSheet.kt`: bottom sheet with title, clip name, readout, slider (0 to 200%, boost part in the boost color with a label), Reset, Done; 48 dp targets, semantics with value, range, and step actions
- [X] T018 [US1] Activate Volume in `E/ui/ClipToolBar.kt` (dimmed with a short reason for photos, soundless or detached video) and show the sheet from the editor screen on compact and medium widths
- [X] T019 [P] [US1] Paparazzi goldens for the Volume sheet (default, boost, dimmed button) in `feature/editor/src/test/`, and a Compose test in `feature/editor/src/androidTest/` for open, drag, Done, and survival across rotation
- [ ] T020 [US1] Fold check: volume drag heard within 100 ms and audio balance correct (quickstart scenario for volume); time first-attempt volume change under 15 s (SC-001); record in `specs/002-clip-speed-volume/checklists/validation.md`

**Checkpoint**: Volume works end to end and ships as the MVP.

---

## Phase 4: User Story 2 - Change a clip's speed (P1)

**Goal**: set 0.25x to 4x on a video clip; length changes, later clips ripple, sound follows with pitch kept.

**Independent Test**: three clips, middle at 2x: it halves, the third slides left, playback runs twice as fast, undo restores everything.

- [ ] T003 [US2] Spike S1 on the Fold (do this first in US2; gates T027): in `core/media/src/main/kotlin/com/petruvideo/core/media/preview/Media3PreviewEngine.kt` try per-item speed with pitch kept at 0.25x, 2x, and 4x on a 1080p H.264 clip with sound; read fps with `adb logcat -s PetruPlayback`; record exact Media3 method names, fps, pitch, and A/V sync across a clip boundary in `specs/002-clip-speed-volume/research.md` section 1. If 4x misses 30 fps, record the decision on the lower maximum (risk R1)

### Tests (write first, must fail)

- [X] T021 [P] [US2] `SetSpeed` tests added to `SpeedVolumeCommandsTest.kt`: ripple of later clips, clamp to range, clamp to the 0.1 s minimum with `SpeedLimited`, rejects photos and unknown ids, exact undo and redo, source files untouched, 2x length within 1 frame of half the source length (SC-006)
- [X] T022 [P] [US2] Trim, split, duplicate, and detach tests on a sped clip in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/` (the existing test files for each command): trim handles work in timeline time, split cuts at the exact source position and both halves cover the source range within 1 ms, duplicate and detach keep speed and volume
- [X] T023 [P] [US2] Extend the 200-seed property test with `SetSpeed` and `SetVolume` in `core/model/src/test/kotlin/com/petruvideo/core/model/engine/` (invariants hold after any sequence, undo returns the identical timeline)

### Implementation

- [X] T024 [US2] Add `SetSpeed` and `Notice.SpeedLimited` in `M/engine/SpeedVolumeCommands.kt` (range clamp, then minimum-length clamp on the fast side)
- [X] T025 [US2] Trim handles work in timeline time: the conversion lives in `E/TrimLimits.kt` (the engine commands keep taking source positions, so no change to `M/engine/TrimCommands.kt`)
- [X] T026 [P] [US2] Update `Split`, `Duplicate` in `M/engine/SplitDuplicateDelete.kt` (cut in source time; copies keep speed and volume) and `DetachAudio` in `M/engine/AudioClipCommands.kt` (new audio clip copies speed and volume; undo restores both together)
- [X] T027 [US2] `Media3PreviewEngine.kt`: apply per-item speed with pitch kept using the API names recorded by T003; photos get none; rebuild on commit off the main thread; the resource-pressure monitor steps preview quality down at high speed
- [X] T028 [US2] View model: `canSetSpeed`, `selectedSpeedMilli`, speed sheet state, readout shows the new timeline length while dragging, commit on release or quick choice, `SpeedLimited` message above the tool bar
- [X] T029 [P] [US2] Speed variant in `E/ui/SpeedVolumeSheet.kt`: quick choices 0.25x, 0.5x, 1x, 2x, 4x, a logarithmic slider that snaps to them, readout like "1.50x"
- [X] T030 [US2] Activate Speed in `E/ui/ClipToolBar.kt` (dimmed for photos); make the ruler, total time, trim handles, and duration badge use `lengthMs` in `E/ui/VideoLaneView.kt` and the other lane code
- [X] T031 [P] [US2] Paparazzi goldens for the Speed sheet and a sped clip on the lane, plus a Compose test for open, quick choice, drag, undo, and the limit message
- [ ] T032 [US2] Fold check: 2x halves length, ripple, pitch kept, A/V sync within 40 ms across a boundary, 30 fps at 0.25x, 2x, 4x; time first-attempt speed change under 15 s (SC-002); record in `checklists/validation.md`

**Checkpoint**: Speed and Volume both work on video clips.

---

## Phase 5: User Story 3 - Speed and volume on audio clips (P2)

**Goal**: same controls on detached and added audio; no overlap on the audio lane.

**Independent Test**: 7 s audio at 0.5x becomes 14 s; volume 60%; slowing into a neighbour is limited with a message.

- [X] T033 [P] [US3] Tests in `SpeedVolumeCommandsTest.kt` and `AudioClipCommandsTest.kt`: audio `SetSpeed` keeps the start, clamps to the slowest speed that avoids overlap with `SpeedLimited`, Replace keeps speed and volume, Duplicate and Move use derived lengths
- [X] T034 [US3] Extend `SetSpeed` for the audio lane (no-overlap clamp) in `M/engine/SpeedVolumeCommands.kt`; make `ReplaceAudio` in `M/engine/AudioClipCommands.kt` keep both settings
- [X] T035 [US3] Show Speed and Volume in the audio branch of `E/ui/ClipToolBar.kt` (scrolls sideways on compact) and use derived lengths in `E/ui/AudioLaneView.kt`
- [X] T036 [P] [US3] Preview: audio items honor speed and gain in `Media3PreviewEngine.kt`; test mute-versus-volume independence
- [ ] T037 [P] [US3] Compose test and golden for the audio tool bar on the 360 dp cover width (all buttons reachable)
- [ ] T038 [US3] Fold check: scenario for audio speed, overlap limit message, and volume; record in `checklists/validation.md`

---

## Phase 6: User Story 4 - Badges, inspector, tabletop (P3)

**Goal**: values visible on the timeline; same controls in the inspector; sheet below the hinge in tabletop.

**Independent Test**: set 1.5x, see the badge, unfold, change it from the inspector, half-fold and open the sheet below the hinge.

- [X] T039 [P] [US4] `E/ui/ClipBadge.kt` ("2x", "40%", both can show) and place it at the top-left of clips in `E/ui/VideoLaneView.kt` and `E/ui/AudioLaneView.kt`; accessible description
- [X] T040 [P] [US4] Speed and Volume sections in `E/ui/expanded/InspectorPanel.kt` (from 840 dp) sharing the slider and quick choices; at 700 to 839 dp the sheet sits inside the timeline column
- [X] T041 [P] [US4] Tabletop: the sheet fills the lower half in `E/ui/tabletop/TabletopEditorLayout.kt`, nothing interactive crosses the hinge
- [ ] T042 [US4] Arrangement tests (state and events per arrangement, open sheet survives fold, unfold, and rotate) in `feature/editor/src/test/` and an androidTest for the open sheet across a configuration change
- [ ] T043 [P] [US4] Paparazzi goldens for badges, the inspector sections, and the tabletop sheet
- [ ] T044 [US4] Fold check: badge, inspector, tabletop, fold during an open sheet, split-screen; record in `checklists/validation.md`

---

## Phase 7: Polish and cross-cutting

- [ ] T045 [P] Accessibility: run the accessibility checks (`AccessibilityTest` pattern from 001) on the sheet and inspector with large font; labels, 48 dp targets, screen reader values (FR-021)
- [ ] T046 [P] Source files byte-identical after edits (SC-008) and save and reopen after force-stop keeps speed and volume (SC-005): androidTest in `core/data` and a manual check
- [ ] T047 Missing media: verify Speed and Volume stay available on a missing clip (edge case) with a test, and fix `canSetSpeed`/`canSetVolume` if they depend on media availability in `feature/editor/src/test/`
- [ ] T048 [P] Update `specs/002-clip-speed-volume/quickstart.md` with the final commands and results
- [ ] T049 Run everything: `:core:model:test`, `testDebugUnitTest`, `verifyPaparazziDebug`, `detekt`, lint, and the connected suites on the Fold; fix failures
- [ ] T050 Mid-range phone check of 30 fps at 4x (SC-003) and designer review of the controls (SC-007); both need outside input, record the result or the open item in `checklists/validation.md`

---

## Dependencies

- Phase 1 then Phase 2 (T003 spike, now first in Phase 4, gates T027; T005 gates everything in the model; T008 gates persistence checks).
- US1 depends on Phase 2 only. US2 depends on Phase 2 and reuses the sheet from US1 (T017) and the view model from T016. US3 depends on US2 (SetSpeed). US4 depends on US1 and US2.
- Within a story: tests, then commands, then preview, then view model, then UI, then device check.

## Parallel examples

- Phase 2: T004, T007, T009 can run together after T005 lands (T004 is written before it).
- US1: T011, T012, T014, T017 touch different files.
- US2: T021, T022, T023 together; T026 and T029 together.
- US4: T039, T040, T041 together.

## Implementation strategy

1. MVP: Phases 1, 2, and 3 (Volume) and the Fold check.
2. Add US2 (Speed), gated by the T003 spike result.
3. Add US3, then US4, then Polish.
4. Stop at any checkpoint; each phase leaves the app working.
