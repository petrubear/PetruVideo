# Tasks: Export Video

**Input**: Design documents from `/specs/004-export-video/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-screens.md, quickstart.md

**Tests**: Included (Principle V: pure logic and the state machine test-first; device tests for the pipeline, saving, and the service).

**Organization**: Grouped by user story. Abbreviations: `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `X` = `core/export/src/main/kotlin/com/petruvideo/core/export`, `F` = `feature/export/src/main/kotlin/com/petruvideo/feature/export`, `D` = `core/data/src/main/kotlin/com/petruvideo/core/data/db`, `P` = `core/media/src/main/kotlin/com/petruvideo/core/media`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallel (different files, no dependencies)
- **[Story]**: US1 export with defaults, US2 settings and estimate, US3 background, cancel, recovery, blockers, US4 layouts on the Fold

---

## Phase 1: Setup

- [X] T001 Run the 003 baseline (`gradle --offline testDebugUnitTest verifyPaparazziDebug detekt`) and confirm it is green
- [X] T002 Create the modules `core:export` and `feature:export`: add them to `settings.gradle.kts`, give each a `build.gradle.kts` with the project's convention plugins (`petru.android.library`, compose and hilt for `feature:export`), dependencies on `core:model`, `core:media`, `core:data`, `core:designsystem`, and a namespace; `core:export` also declares the service in its `AndroidManifest.xml` (`foregroundServiceType="mediaProcessing"`, permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROCESSING`, `POST_NOTIFICATIONS`, and `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`)

---

## Phase 2: Foundational (blocks all stories)

- [X] T003 [P] Write failing tests in `core/model/src/test/kotlin/com/petruvideo/core/model/export/BitrateTableTest.kt`: Recommended video bitrates at 30 fps (6, 12, 20, 45 Mbit/s), frame-rate scaling `(fps/30)^0.75`, Smaller 0.6x, Higher 1.5x, audio 128 kbit/s, `estimatedBytes = (video + audio) x seconds / 8`
- [X] T004 Add `Resolution`, `FrameRate`, `Quality`, `ExportSettings`, and `ExportState` in `M/export/ExportSettings.kt` and `M/export/ExportState.kt` (data-model.md), and `BitrateTable` in `M/export/BitrateTable.kt`; make T003 pass
- [X] T005 [P] Write failing tests in `core/model/src/test/kotlin/com/petruvideo/core/model/export/ExportCapsTest.kt`: max resolution is the largest source in use (photos by pixel size, at least 720), max frame rate is the highest source rate (photos at the project rate, at least 24), defaults (largest option not above 1080p or the cap, nearest frame rate within the cap, Recommended), saved choices lowered into the caps, option reasons `AboveFootage` and `NotSupported`
- [X] T006 Add `ExportCaps` and the defaults and option rules in `M/export/ExportCaps.kt`; make T005 pass
- [X] T007 Extract the composition construction from `P/preview/Media3PreviewEngine.kt` into `P/composition/CompositionBuilder.kt` with parameters for output size, frame rate, and whether processors are live (preview) or fixed (export); make the preview use it; every existing `PreviewEngineTest` must still pass
- [X] T008 [P] Room v4 in `D/`: entities `ExportSettingsEntity` and `ExportJobEntity` in `Entities.kt`, a DAO in `Daos.kt`, mappers, `PetruDatabase.kt` version 4 with `AutoMigration(from = 3, to = 4)`, exported `core/data/schemas/.../4.json`, and a repository for settings and the latest job
- [X] T009 [P] Migration test in `core/data/src/androidTest/kotlin/com/petruvideo/core/data/db/Migration3To4Test.kt`: a v3 project opens in v4 with no saved settings or job; round trip of non-default values
- [X] T010 [P] `X/EncoderCapabilities.kt`: an interface with a `MediaCodecList`/`EncoderUtil` implementation (largest supported H.264 size and frame rate, supported bitrate range, hardware preferred) and a fake for tests
- [X] T011 Spike S3 on the emulator: a minimal `X/TransformerRunner.kt` that builds the export composition with `CompositionBuilder`, runs `Transformer` (H.264, AAC, `DefaultEncoderFactory` with constant bitrate), and writes a file; confirm with `MediaMetadataRetriever` that length, size, and an audio track are right for a two-clip project with a 2x clip and one with reverb; record the findings (method names, bitrate accuracy, speed) in `specs/004-export-video/research.md`

**Checkpoint**: settings, caps, bitrate, storage, shared composition, and a working render are in place.

---

## Phase 3: User Story 1 - Export with the recommended settings (P1) - MVP

**Goal**: Export, Save to gallery, finished video in the gallery that matches the preview.

**Independent Test**: a project with a 2x clip and a reverb clip, two taps, the file in the gallery with the right length, speed, volume, and reverb.

### Tests (write first, must fail)

- [X] T012 [P] [US1] Coordinator state machine tests in `core/export/src/test/kotlin/com/petruvideo/core/export/ExportCoordinatorTest.kt` (fake runner and saver): Waiting to Rendering(progress) to Finished, one job at a time, snapshot is a copy and later edits do not change it, failures end in Failed and clean up
- [X] T013 [P] [US1] View model tests in `feature/export/src/test/kotlin/com/petruvideo/feature/export/ExportViewModelTest.kt`: default settings from the caps, empty project blocked with a reason, Start moves state to Rendering, finished shows Open and Share
- [X] T014 [P] [US1] Device test in `core/export/src/androidTest/kotlin/com/petruvideo/core/export/ExportPipelineTest.kt`: export a small project and check length within one frame, resolution (short side), frame rate, audio track present, audio absent for a muted video, and that the output length follows a 2x clip
- [X] T015 [P] [US1] Device test in `core/export/src/androidTest/kotlin/com/petruvideo/core/export/GallerySaverTest.kt`: success publishes a MediaStore row under `Movies/PetruVideo` that is not pending and removes the cache file; failure or cancel leaves no row

### Implementation

- [X] T016 [US1] `X/TransformerRunner.kt` (finish from the spike; clamp the bitrate to the encoder's range and report the value used; export HDR sources as SDR): progress polling once a second, cancel, error mapping to `Failed(reason)`, frame-rate effect, `Presentation` to the output short side with aspect ratio kept, output to the cache file
- [X] T017 [P] [US1] `X/GallerySaver.kt`: MediaStore insert with `IS_PENDING`, copy, publish, delete cache; API 26 to 28 path with the legacy permission; delete the pending row on failure
- [X] T018 [US1] `X/ExportCoordinator.kt` with the `ExportRunner` and `ExportSaver` interfaces (implemented by `TransformerRunner` and `GallerySaver`, faked in tests): singleton holding the job state flow, the snapshot, settings, runner and saver; `start`, `cancel`, `retry`; persists the latest job via the repository
- [X] T019 [US1] `X/ExportService.kt`: foreground service started from the Start action, notification channel and a progress notification, stops when the job ends; wire it in the manifest (T002) and the coordinator
- [X] T020 [US1] `F/ExportContract.kt` and `F/ExportViewModel.kt`: state (settings, options, bitrate, estimate, blocker, job) and events from the contract; reads the coordinator; remembers nothing yet
- [X] T021 [P] [US1] `F/ui/ExportScreen.kt`, `F/ui/ExportSettingsPanel.kt`, `F/ui/ExportProgressCard.kt` (compact layout per the Export artboard): preview with length, segmented controls, Format and Bitrate and Estimated size rows, progress card with Cancel, result row with Open and Share, Save to gallery button
- [X] T022 [US1] App wiring: add the Export destination to the navigation graph in `app/`, open it from the editor's Export button (replace the `TapInactiveTool("Export")` calls in `EditorScreen.kt`, `ExpandedEditorLayout.kt`, and the tabletop layout of `feature/editor`) and the project detail Export button, DI for the coordinator, saver, and capabilities in `app/src/main/kotlin/com/petruvideo/app/di/`
- [X] T023 [P] [US1] Paparazzi goldens for the compact screen (idle, rendering at 64%, finished, failed) and a Compose test for the two-tap flow in `feature/export/src/androidTest/`
- [ ] T024 [US1] Device check on the emulator and the Fold: two taps produce a gallery video that matches the preview; record in `specs/004-export-video/checklists/validation.md`

**Checkpoint**: export with defaults works end to end and ships as the MVP.

---

## Phase 4: User Story 2 - Choose resolution, frame rate, and quality (P1)

**Goal**: change the settings, see bitrate and size update, unavailable options explained, choices remembered.

**Independent Test**: switch 1080p to 720p and the size drops; a 1080p-only project dims 2K and 4K with a reason; reopen and the choices return.

### Tests (write first, must fail)

- [X] T025 [P] [US2] View model tests in `ExportViewModelTest.kt`: selecting an option updates bitrate and estimate immediately, dimmed options explain themselves and cannot start, choices saved per project and lowered into new caps, short-side rule for portrait projects
- [ ] T026 [P] [US2] Device test in `ExportPipelineTest.kt`: real file size within 15% of the estimate for three settings (SC-003), output matches the chosen resolution and frame rate, a frame rate below the source drops frames and keeps the length

### Implementation

- [X] T027 [US2] `F/ExportViewModel.kt`: options with reasons from `ExportCaps` and `EncoderCapabilities`, live bitrate and estimate from `BitrateTable`, remember and restore settings through the repository (T008)
- [X] T028 [P] [US2] `F/ui/ExportSettingsPanel.kt`: dimmed options with the reason shown on tap, selected state, 48 dp targets, semantics with state and reason
- [ ] T029 [US2] Tune `M/export/BitrateTable.kt` from measured files (T026) if the estimate is outside 15%, and record the correction in `research.md`
- [X] T030 [P] [US2] Goldens for the settings with a dimmed 2K and 4K and for Higher quality; Compose test that tapping a dimmed option explains it and does not select it
- [ ] T031 [US2] Device check: scenarios 2, 3, and 10 of the quickstart; record in `checklists/validation.md`

---

## Phase 5: User Story 3 - Background, cancel, recovery, blockers (P2)

**Goal**: the render survives leaving the app, cancels cleanly, recovers honestly, and refuses to start when it cannot succeed.

**Independent Test**: start a 4K export, press Home, see the notification advance, cancel, and find no partial file; kill the app mid-render and see the interrupted notice.

### Tests (write first, must fail)

- [X] T032 [P] [US3] Coordinator tests: cancel moves to Cancelled and deletes the cache file, a second start while one runs is refused, Failed reasons (`MissingMedia`, `NotEnoughStorage`, `EncoderError`, `Pressure`, `SaveFailed`), `ExportRecovery` turns a persisted Waiting or Rendering job into `Failed(Interrupted)` and removes cache leftovers
- [X] T033 [P] [US3] View model tests: blockers (no clips, missing media with the file name, not enough storage with smaller-settings hint), Retry restarts with the same settings, interrupted notice shown once on open
- [ ] T034 [P] [US3] Device tests in `core/export/src/androidTest/`: cancel stops within 2 seconds with no MediaStore row and no cache file; the service keeps rendering with the activity closed (instrumentation with `am start`/home); recovery after a simulated kill

### Implementation

- [X] T035 [US3] `X/ExportCoordinator.kt`: cancel with cleanup, single-job rule, reasons, pressure handling (lower the render priority; at Critical pressure stop with `Failed(Pressure)` and offer Retry, per FR-021)
- [X] T036 [US3] `X/ExportService.kt`: Cancel action on the notification, finished and failed notifications that open the app on the export screen, notification permission requested at the first export without blocking it
- [X] T037 [P] [US3] `X/ExportRecovery.kt`: run at app start (hook in the `Application` class in `app/`), mark interrupted jobs failed and clean `cacheDir/exports`
- [X] T038 [US3] `F/ExportViewModel.kt` and `F/ui/ExportScreen.kt`: storage check (`2 x estimate` plus 50 MB), missing-media blocker with relink path, Retry, interrupted notice, plain-language failure messages
- [X] T039 [P] [US3] Goldens for the blockers, the failed state with Retry, and the interrupted notice
- [ ] T040 [US3] Device check: scenarios 4, 5, 6, and 7 on the Fold; record in `checklists/validation.md`

---

## Phase 6: User Story 4 - Layouts on the Fold (P3)

**Goal**: unfolded and tabletop layouts, render and settings unaffected by fold and rotation.

**Independent Test**: open Export on the cover screen, unfold, half-fold during a render; nothing sits under the hinge and the render continues.

- [X] T041 [P] [US4] `F/ui/ExportExpandedLayout.kt`: preview left, settings and progress right, vertical hinge split with nothing interactive under it
- [X] T042 [P] [US4] `F/ui/ExportTabletopLayout.kt`: preview above the hinge, settings, progress, and button below
- [X] T043 [US4] `F/ui/ExportScreen.kt`: choose the layout with `AdaptiveScaffold` and `LocalArrangement` like the editor
- [X] T044 [P] [US4] Tests: a render and the selected settings survive arrangement changes (androidTest like `ArrangementPanelsTest`); goldens in `feature/export/src/test/` for compact, unfolded, hinge-split, and tabletop; accessibility checks including a 2x font scale
- [ ] T045 [US4] Fold check: scenario 11 of the quickstart (cover, unfold, tabletop, fold and rotate during a render); record in `checklists/validation.md`

---

## Phase 7: Polish and cross-cutting

- [X] T046 [P] Test that the export matches the preview in `ExportPipelineTest.kt`: a project with 2x and 0.5x clips, a muted clip, volume 150%, and reverb exports with the expected length and an audio track, the sound-processing chain is the same as the preview's (shared builder), and peaks stay below full scale with 150% volume plus reverb
- [X] T047 [P] Sources and project unchanged (SC-008): extend `core/data/src/androidTest/.../OriginalsUnchangedTest.kt` with an export run and compare checksums
- [X] T048 [P] Photo-only project, audio-only project (black video), a project with a gap, and an HDR source exported as SDR with the notice: tests in `ExportPipelineTest.kt`
- [X] T049 [P] Accessibility: progress announced at 10% steps and Cancel reachable at large font sizes (FR-024)
- [ ] T050 Measure A/V sync (within 40 ms), preview smoothness while a render runs, render speed (SC-002) for a one-minute 1080p 30 fps project on the Fold and on a mid-range phone, and a one-minute 4K background run (SC-006); record the numbers or the open item
- [X] T051 [P] Update `specs/004-export-video/quickstart.md` with final commands and results
- [X] T052 Run everything: `testDebugUnitTest`, `verifyPaparazziDebug`, `detekt`, `lintDebug`, `connectedDebugAndroidTest`; fix failures
- [ ] T053 Designer review of the Export screen against the artboard and confirmation of the buttons under Save to gallery (SC-007); record deviations or the open item in `design/reference/README.md`

---

## Dependencies

- Phase 1, then Phase 2 (T004 gates the model; T007 gates T011 and T016; T008 gates T020 persistence; T011 gates T016).
- US1 depends on Phase 2. US2 depends on US1 (screen, view model). US3 depends on US1 (coordinator and service) and can run beside US2. US4 depends on US1.
- Within a story: tests, then pipeline and coordinator, then service, then view model, then UI, then device check.

## Parallel examples

- Phase 2: T003, T005, T008, T009, and T010 touch different files (T003 and T005 are written before T004 and T006).
- US1: T012 to T015 together; T017 and T021 together.
- US3: T032, T033, T034 together; T037 and T039 together.
- US4: T041 and T042 together.

## Implementation strategy

1. MVP: Phases 1, 2, and 3 (export with the defaults) and the device check.
2. Add US2 (settings and estimate), then US3 (background, cancel, recovery), then US4, then Polish.
3. Stop at any checkpoint; each phase leaves the app working.
