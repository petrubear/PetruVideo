---

description: "Task list for Project Creation, Media Import, and Basic Timeline with Trim"
---

# Tasks: Project Creation, Media Import, and Basic Timeline with Trim

**Input**: Design documents from `/specs/001-project-media-timeline/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The constitution (Principle V) requires test-first development of the editing engine and automated checks for flows, so test tasks come before the code they cover and MUST fail first.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested on its own.

## Format: `[ID] [P?] [Story] Description`


- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 Projects, US2 Import, US3 Timeline, US4 Trim/edit/audio, US5 Foldable layouts

## Path Conventions

Base package `com.petruvideo`, shortened below as `<pkg>` = `com/petruvideo`. Modules follow plan.md:

- `app/`, `core/model/`, `core/data/`, `core/media/`, `core/designsystem/`
- `feature/projects/`, `feature/importmedia/`, `feature/editor/`
- Sources at `<module>/src/main/kotlin/<pkg>/...`; tests at `<module>/src/test/kotlin/<pkg>/...` (JVM) or `<module>/src/androidTest/kotlin/<pkg>/...` (device)
- Design reference images live in `design/reference/` (exported from the designer's canvas, https://claude.ai/artifact/D8dMeHZE4XUAvNj4wERyNo)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Gradle project, modules, and tooling

- [X] T001 Create root Gradle build: `settings.gradle.kts` (include app, core:model, core:data, core:media, core:designsystem, feature:projects, feature:importmedia, feature:editor), root `build.gradle.kts`, `gradle.properties`, and `gradle/libs.versions.toml` pinning Kotlin, AGP, Compose BOM, Material 3 adaptive, WindowManager, Media3, Room, Hilt, Navigation Compose, Coil, JUnit, Turbine, Kotest
- [X] T002 Create convention plugins in `build-logic/convention/src/main/kotlin/` (`AndroidLibraryConventionPlugin.kt`, `AndroidComposeConventionPlugin.kt`, `HiltConventionPlugin.kt`) and `build-logic/settings.gradle.kts` (minSdk 26, targetSdk latest stable, JVM 17)
- [X] T003 [P] Create `core/model/build.gradle.kts` as a pure Kotlin/JVM module (no Android plugins)
- [X] T004 [P] Create `core/data/build.gradle.kts` (Room, KSP), `core/media/build.gradle.kts` (Media3, Coil), `core/designsystem/build.gradle.kts` (Compose, WindowManager)
- [X] T005 [P] Create `feature/projects/build.gradle.kts`, `feature/importmedia/build.gradle.kts`, `feature/editor/build.gradle.kts` (Compose + Hilt, depend only on core modules)
- [X] T006 Create `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` (resizeableActivity true; MainActivity handling `screenSize|smallestScreenSize|screenLayout|orientation|keyboard|keyboardHidden|navigation|density|layoutDirection` so fold, resize, and rotation never restart it), `app/src/main/kotlin/<pkg>/app/PetruVideoApp.kt` (`@HiltAndroidApp`), and `MainActivity.kt`
- [X] T007 [P] Add static analysis: `.editorconfig` (ktlint rules) and `config/detekt/detekt.yml`; wire into the convention plugins
- [X] T008 [P] Add `.github/workflows/ci.yml` running `:core:model:test`, `testDebugUnitTest`, and `lint` on every push

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Engine core, design tokens, storage, session, adaptive scaffolding, test tooling that every story needs

**CRITICAL**: No user story work begins until this phase is complete

- [X] T009 [P] Create model types in `core/model/src/main/kotlin/<pkg>/core/model/Models.kt`: `Project`, `ProjectFormat`, `MediaItem`, `MediaKind`, `Availability`, `Fingerprint`, `Timeline`, `VideoClip`, `AudioClip`, `AudioOrigin`, and constants `MIN_CLIP_MS = 100`, `PHOTO_DEFAULT_MS = 5000`, `PHOTO_MIN_MS = 500`, `PHOTO_MAX_MS = 60000` (per data-model.md)
- [X] T010 [P] Write failing tests `core/model/src/test/kotlin/<pkg>/core/model/engine/EditHistoryTest.kt`: apply, undo, redo, redo cleared on new edit, capacity 100, `canUndo`/`canRedo`
- [X] T011 Implement `Command`, `CommandResult` (`Applied`, `Rejected(reason)`), and `EditHistory` in `core/model/src/main/kotlin/<pkg>/core/model/engine/` to pass T010
- [X] T012 [P] Write `TimelineInvariants.kt` (invariants 1 to 3 and 6 from contracts/editing-engine.md) in `core/model/src/main/kotlin/<pkg>/core/model/engine/`, with random timeline generators in `core/model/src/test/kotlin/<pkg>/core/model/engine/TimelineGenerators.kt` and `TimelineInvariantsTest.kt`
- [X] T013 [P] Implement design tokens from the designer's canvas in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/`: `Color.kt` (background, surfaces, accent, track colors for video/audio/music/pip/effects), `Type.kt` (Manrope + JetBrains Mono bundled in `res/font/`, with `licenses/` entry), `Shape.kt`, `Spacing.kt`, `PetruTheme.kt` (dark theme)
- [X] T014 [P] Create vector icons used by the designed screens in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/icons/PetruIcons.kt` (search, settings, back, close, undo, redo, play, fullscreen, split, speed, volume, detach, duplicate, delete, mute, replace, add, tool bar icons)
- [X] T015 [P] Create shared components in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/components/`: `PillButton.kt`, `PetruChip.kt`, `SegmentedControl.kt`, `LabeledSlider.kt`, `SheetSurface.kt`; every icon-only control takes a required content description and enforces a 48dp touch target
- [ ] T016 Create Room database in `core/data/src/main/kotlin/<pkg>/core/data/db/`: `PetruDatabase.kt` (version 1, exportSchema true), entities `ProjectEntity.kt`, `MediaItemEntity.kt`, `VideoClipEntity.kt`, `AudioClipEntity.kt`, DAOs, and mappers to the model types, matching contracts/project-storage.md
- [ ] T017 [P] Set up migration test harness: `core/data/schemas/` export directory and `core/data/src/androidTest/kotlin/<pkg>/core/data/db/MigrationTestBase.kt`
- [ ] T018 Implement `ProjectRepository` interface and `RoomProjectRepository` in `core/data/src/main/kotlin/<pkg>/core/data/repo/` (observeProjects with filter and query, loadProject, createProject, saveTimeline, duplicateProject, deleteProject, rename, setStatus)
- [ ] T019 [P] Write failing tests `core/data/src/test/kotlin/<pkg>/core/data/autosave/AutosaveWriterTest.kt` using virtual time: 1.5 s debounce, single writer, flush on stop, status flow ("saving"/"saved"), write failure keeps the last good rows and retries when space is freed
- [ ] T020 Implement `AutosaveWriter` in `core/data/src/main/kotlin/<pkg>/core/data/autosave/AutosaveWriter.kt` to pass T019
- [ ] T021 [P] Write failing tests `core/designsystem/src/test/kotlin/<pkg>/core/designsystem/adaptive/ArrangementResolverTest.kt`: compact width -> Compact; medium width (600 to 839 dp) -> Compact with wide-content flag; expanded flat (840 dp and up) -> Expanded; half-opened horizontal fold -> Tabletop; separating hinge bounds returned for hinge avoidance; cover screen -> Compact
- [ ] T022 Implement `ArrangementInput`, `Arrangement`, and `ArrangementResolver` (pure function) in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/adaptive/` to pass T021
- [ ] T023 Implement `WindowArrangementProvider.kt` in the same package: observes window size class and the fold feature through WindowManager and exposes `StateFlow<ArrangementInput>`
- [ ] T024 Implement `AdaptiveScaffold.kt` in the same package: slot-based composable that renders compact, expanded, or tabletop content from the resolved arrangement and exposes the hinge bounds to its slots (avoidance logic is built in T116)
- [ ] T025 [P] Write failing tests `core/data/src/test/kotlin/<pkg>/core/data/session/ProjectSessionTest.kt`: apply command updates timeline and enqueues autosave, undo/redo, rejected commands leave state unchanged, session survives being re-collected (state retention)
- [ ] T026 Implement `ProjectSession` in `core/data/src/main/kotlin/<pkg>/core/data/session/ProjectSession.kt` (holds current project, `StateFlow<Timeline>`, `EditHistory`, playhead, save status; scoped so it survives configuration changes) to pass T025
- [ ] T027 [P] Create `AppError` types (`LowStorage`, `MissingMedia`, `UnsupportedFile(name)`, `PermissionDenied`) in `core/model/src/main/kotlin/<pkg>/core/model/error/AppError.kt` with user-message mapping in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/ErrorMessages.kt`
- [ ] T028 Create Hilt modules in `app/src/main/kotlin/<pkg>/app/di/` (`DataModule.kt`, `MediaModule.kt`) providing the database, repository, autosave writer, and session
- [ ] T029 Create navigation skeleton `app/src/main/kotlin/<pkg>/app/navigation/PetruNavHost.kt` with Projects, AddMedia, and Editor routes using placeholder screens
- [ ] T030 [P] Write failing instrumented test `core/media/src/androidTest/kotlin/<pkg>/core/media/probe/MediaProbeTest.kt`: duration, display size with rotation, frame rate, `hasAudio`, fingerprint, unreadable file error
- [ ] T031 Implement `MediaProbe` in `core/media/src/main/kotlin/<pkg>/core/media/probe/MediaProbe.kt` to pass T030
- [ ] T032 [P] Add small redistributable test media in `core/media/src/androidTest/assets/` (video with sound, video without sound, portrait video, rotated video, photo, audio file, corrupted file) with a `LICENSES.md`
- [ ] T033 [P] Set up screenshot testing: choose the tool (Paparazzi or Compose preview screenshot testing), configure a `verifyScreenshots` task in the convention plugins that compares against golden images generated from the Compose UI and approved by the designer (never pixel-compared with the designer's mock images), and export the designer's artboards to `design/reference/` for side-by-side review only, with `design/reference/README.md` mapping files to artboard names

**Checkpoint**: Foundation ready - user story work can begin

---

## Phase 3: User Story 1 - Create and manage projects (Priority: P1)

**Goal**: The Projects screen as designed: continue-editing card, filters, grid with New project tile, options menu, search, and autosaved projects that reopen in their previous state.

**Independent Test**: Create a project, close the app, reopen it, and confirm it appears in the grid and in "Continue editing" and opens in its previous state; rename, duplicate, delete (with confirmation), filter, and search work.

### Tests for User Story 1

- [ ] T034 [P] [US1] Write failing `feature/projects/src/test/kotlin/<pkg>/feature/projects/ProjectsViewModelTest.kt`: filter counts, search narrowing, continue-editing selection, rename, duplicate, delete requires confirmation
- [ ] T035 [P] [US1] Write failing `core/data/src/androidTest/kotlin/<pkg>/core/data/repo/RoomProjectRepositoryTest.kt`: create, observe ordering by `modifiedAt`, filters, query, duplicate deep copy, delete leaves media untouched
- [ ] T036 [P] [US1] Write failing Compose UI test `feature/projects/src/androidTest/kotlin/<pkg>/feature/projects/ProjectsScreenTest.kt`: New project tile, card options menu, empty state, dimmed navigation entries show "coming soon"

### Implementation for User Story 1

- [ ] T037 [US1] Create `ProjectsContract.kt` (state, events per contracts/ui-screens.md) in `feature/projects/src/main/kotlin/<pkg>/feature/projects/`
- [ ] T038 [US1] Implement `ProjectsViewModel.kt` in the same package to pass T034
- [ ] T039 [P] [US1] Create `ContinueEditingCard.kt` and `FilterChipRow.kt` in `feature/projects/src/main/kotlin/<pkg>/feature/projects/ui/` matching the designed Projects screen
- [ ] T040 [P] [US1] Create `ProjectCard.kt`, `NewProjectTile.kt`, and `ProjectOptionsMenu.kt` in the same package (thumbnail, duration badge, "Exported" badge, options)
- [ ] T041 [P] [US1] Create `ProjectDialogs.kt` (rename dialog, delete confirmation) in the same package
- [ ] T042 [US1] Create `ProjectsScreen.kt` in the same package assembling header with search and settings buttons, search field, empty states, the grid, and the dimmed bottom navigation (depends on T039, T040, T041)
- [ ] T043 [US1] Wire the Projects route in `PetruNavHost.kt`: New project creates a project and navigates to a minimal `EditorRoute` stub in `feature/editor/src/main/kotlin/<pkg>/feature/editor/EditorRoute.kt` that loads the project through `ProjectSession`
- [ ] T044 [US1] Add screenshot test `feature/projects/src/test/kotlin/<pkg>/feature/projects/ProjectsScreenshotTest.kt` recording the golden image for the compact Projects screen, reviewed against the `design/reference/` Projects image

**Checkpoint**: User Story 1 works on its own

---

## Phase 4: User Story 2 - Import multiple media items (Priority: P1)

**Goal**: The Add media screen as designed, with ordered multi-select, permission flow, per-file errors, and adding to the timeline in selection order, including format derivation from the first clip.

**Independent Test**: In a project, select five mixed items in a chosen order and tap "Add to timeline in order"; confirm numbered badges, tray totals, correct order on the timeline data, a per-file message for the corrupted file, and a working no-broad-access path.

### Tests for User Story 2

- [ ] T045 [P] [US2] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/AddClipsTest.kt`: video and photos inserted in selection order after the clip under the playhead (at the playhead on a boundary, at the start when empty), `attachedAudio` from `hasAudio`, audio-only files starting at the playhead in selection order, audio clips snapping to the next free spot with a `Snapped` notice when they would overlap another audio clip but allowed to overlap attached sound, missing media rejection, project format set exactly once
- [ ] T046 [P] [US2] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/FormatDerivationTest.kt`: aspect ratio from rotated size, 1080 cap on the short side without upscaling, frame rate rounding to 24/30/60, default 30, photo-only start
- [ ] T047 [P] [US2] Write failing `feature/importmedia/src/test/kotlin/<pkg>/feature/importmedia/AddMediaViewModelTest.kt`: selection numbering and renumbering, persistence across tab and album changes, totals, partial failure reporting
- [ ] T048 [P] [US2] Write failing `core/data/src/androidTest/kotlin/<pkg>/core/data/importing/ImportMediaUseCaseTest.kt`: per-file error does not stop the batch, progress, cancellation, same file twice creates two items, originals unchanged
- [ ] T049 [US2] Run Spike 2 (picker URI persistence across process death and restart on min and target OS) and record the result in `specs/001-project-media-timeline/research.md` section 6

### Implementation for User Story 2

- [ ] T050 [US2] Implement `FormatDerivation.kt` in `core/model/src/main/kotlin/<pkg>/core/model/engine/` to pass T046
- [ ] T051 [US2] Implement `AddVideoClips` and `AddAudioClips` commands in `core/model/src/main/kotlin/<pkg>/core/model/engine/AddClips.kt` to pass T045 (depends on T050); results carry `Snapped` notices
- [ ] T052 [US2] Implement `MediaLibrarySource.kt` in `core/media/src/main/kotlin/<pkg>/core/media/library/` (albums, Videos/Photos/Audio queries, paging, partial "selected items" access)
- [ ] T053 [P] [US2] Implement `PermissionCoordinator.kt` in `feature/importmedia/src/main/kotlin/<pkg>/feature/importmedia/` (just-in-time request, partial access, denied rationale)
- [ ] T054 [P] [US2] Implement `SystemPickerLauncher.kt` in the same package as the no-broad-access path, following the Spike 2 outcome for persistable access
- [ ] T055 [US2] Implement `ImportMediaUseCase.kt` in `core/data/src/main/kotlin/<pkg>/core/data/importing/` (probe, fingerprint, per-file `UnsupportedFile` errors, progress, cancellation); media items are recorded only when items are added to the timeline, and items no longer referenced by any clip are removed on save, to pass T048
- [ ] T056 [US2] Create `AddMediaContract.kt` and `AddMediaViewModel.kt` in `feature/importmedia/src/main/kotlin/<pkg>/feature/importmedia/` to pass T047
- [ ] T057 [P] [US2] Implement `GridThumbnailLoader.kt` in `core/media/src/main/kotlin/<pkg>/core/media/thumbs/` (video frame and photo thumbnails via Coil, bounded and cancellable)
- [ ] T058 [P] [US2] Create `SelectionGrid.kt` (three columns, duration labels, numbered badges) and `AlbumChipRow.kt` in `feature/importmedia/src/main/kotlin/<pkg>/feature/importmedia/ui/`
- [ ] T059 [P] [US2] Create `SelectionTray.kt` (selected thumbnails, count, total duration, single "Add to timeline in order" action) in the same package
- [ ] T060 [US2] Create `AddMediaScreen.kt` in the same package assembling the header (close button and title only), segmented Videos/Photos/Audio, albums, grid, and tray (depends on T058, T059)
- [ ] T061 [US2] Wire the AddMedia route in `PetruNavHost.kt` and connect "Add to timeline in order" to `ProjectSession` (`AddVideoClips`/`AddAudioClips`), including the editor stub's add-clip entry; show an import progress state with a Cancel button for imports longer than one second (FR-011)
- [ ] T062 [US2] Add screenshot test `feature/importmedia/src/test/kotlin/<pkg>/feature/importmedia/AddMediaScreenshotTest.kt` recording the golden image (tray with the single action), reviewed against the Multi-import reference image

**Checkpoint**: User Stories 1 and 2 work independently

---

## Phase 5: User Story 3 - Arrange clips on the editor timeline (Priority: P1)

**Goal**: The editor as designed (top bar, preview, transport, ruler, playhead, video lane, audio lane, add clip button, tool bars) with scrubbing, playback across clips, reordering, insertion at the playhead, photo duration, fullscreen, missing-media handling, and autosave status.

**Independent Test**: Add three clips, scrub, play across boundaries, drag one to a new position, and confirm preview, time display, and total length follow; delete a source file and confirm the clip shows as missing with Relink and Remove.

### Tests for User Story 3

- [ ] T063 [P] [US3] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/MoveAndRelinkCommandsTest.kt`: `MoveVideoClip` reorders and attached audio follows, `RelinkMedia` keeps trim and clamps to the new length
- [ ] T064 [P] [US3] Write failing `feature/editor/src/test/kotlin/<pkg>/feature/editor/TimelineGeometryTest.kt`: time-to-pixel mapping, ruler ticks, playhead-centered scrolling, clip hit-testing
- [ ] T065 [P] [US3] Write failing `feature/editor/src/test/kotlin/<pkg>/feature/editor/EditorViewModelTest.kt`: seek, play/pause, selection, undo/redo availability, inactive tool tap shows "coming soon", missing clip handling, thumbnail refresh when the first clip changes
- [ ] T066 [P] [US3] Write failing `core/media/src/androidTest/kotlin/<pkg>/core/media/waveform/WaveformPeaksTest.kt`: peaks computed once, cached by fingerprint, no waveform for silent video
- [ ] T067 [P] [US3] Write failing `core/media/src/androidTest/kotlin/<pkg>/core/media/preview/PreviewEngineTest.kt`: continuous playback across clip boundaries, seek accuracy, A/V sync tolerance, clips of a different size or frame rate fitted inside the frame without cropping or stretching, missing clips skipped with a placeholder, overlapping audio mixed

### Implementation for User Story 3

- [ ] T068 [US3] Run Spike 1 (two-lane preview with scrub and boundary continuity on a reference mid-range device; 30 fps and A/V sync within 40 ms) and record the decision and the reference device model and class in `specs/001-project-media-timeline/research.md` section 1
- [ ] T069 [US3] Implement `MoveVideoClip` and `RelinkMedia` commands in `core/model/src/main/kotlin/<pkg>/core/model/engine/` to pass T063
- [ ] T070 [US3] Define the `PreviewEngine` interface in `core/media/src/main/kotlin/<pkg>/core/media/preview/PreviewEngine.kt` (load timeline, play, pause, seek, position flow, release)
- [ ] T071 [US3] Implement `Media3PreviewEngine.kt` in the same package per the Spike 1 decision to pass T067 (depends on T068, T070); done only when a 30 fps / A/V sync measurement on the reference device is recorded in research.md section 1 (constitution Principle III); fits mismatched clips inside the frame and skips missing clips
- [ ] T072 [P] [US3] Implement `ThumbnailStripGenerator.kt` in `core/media/src/main/kotlin/<pkg>/core/media/thumbs/` (fixed time steps, disk cache keyed by fingerprint, bounded dispatcher, cancellable)
- [ ] T073 [P] [US3] Implement `WaveformPeaksGenerator.kt` in `core/media/src/main/kotlin/<pkg>/core/media/waveform/` to pass T066
- [ ] T074 [P] [US3] Implement `MissingMediaChecker.kt` in `core/media/src/main/kotlin/<pkg>/core/media/` (recompute availability on project open using URI access and fingerprint)
- [ ] T075 [US3] Implement `ProjectThumbnailUpdater.kt` in `core/media/src/main/kotlin/<pkg>/core/media/thumbs/` (project thumbnail is the first frame of the first clip, saved to `thumbnailRef` through `ProjectSession`) (depends on T072)
- [ ] T076 [US3] Implement `ResourcePressureMonitor.kt` in `core/media/src/main/kotlin/<pkg>/core/media/pressure/` (memory trim callbacks and thermal status) that lowers preview resolution and frame rate and thumbnail size, emits a non-blocking notice, and pauses playback only if it cannot continue (FR-031); add `ResourcePressureMonitorTest.kt` with simulated pressure signals in `core/media/src/androidTest/kotlin/<pkg>/core/media/pressure/` (depends on T071, T072)
- [ ] T077 [US3] Create `EditorContract.kt` (state and events per contracts/ui-screens.md) and `TimelineGeometry.kt` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/` to pass T064
- [ ] T078 [US3] Implement `EditorViewModel.kt` in the same package (playback, scrubbing, selection, undo/redo wiring, autosave status, missing-media actions, and project thumbnail refresh through `ProjectThumbnailUpdater` when the first clip changes) to pass T065 (depends on T026, T069, T071, T074, T075, T077)
- [ ] T079 [P] [US3] Create `EditorTopBar.kt` (back, name, format and save status, dimmed Export) and `TransportRow.kt` (time readout, undo, play, redo, fullscreen) in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ui/`
- [ ] T080 [P] [US3] Create `PreviewSurface.kt` and `FullscreenPreview.kt` in the same package (playback continues from the same position)
- [ ] T081 [P] [US3] Create `TimelineRuler.kt` and `Playhead.kt` in the same package (fixed center playhead, highlighted current second)
- [ ] T082 [P] [US3] Create `VideoLaneView.kt` (thumbnail strips, drag to reorder, photo clips) and `AudioLaneView.kt` (attached waveform under video clips, audio clips) in the same package
- [ ] T083 [P] [US3] Create `MissingClipPlaceholder.kt` with Relink and Remove actions in the same package
- [ ] T084 [P] [US3] Create `MainToolBar.kt` (Edit active; Audio, Text, Overlay, Effects, Filters dimmed with "coming soon") in the same package
- [ ] T085 [US3] Create `TimelineView.kt` combining ruler, lanes, playhead, scrolling/scrubbing gestures, the add clip button (inserts after the clip under the playhead), and the empty-timeline state (helpful message, playback controls disabled) (depends on T081, T082, T083)
- [ ] T086 [US3] Replace the stub with the compact `EditorScreen.kt` and `EditorRoute.kt` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/` assembling top bar, preview, transport, timeline, and main tool bar (depends on T078 to T085)
- [ ] T087 [US3] Add Compose UI test `feature/editor/src/androidTest/kotlin/<pkg>/feature/editor/TimelineFlowTest.kt`: add three clips, scrub, play across boundaries, reorder, photo duration, missing media Relink and Remove
- [ ] T088 [US3] Add screenshot test `feature/editor/src/test/kotlin/<pkg>/feature/editor/EditorScreenshotTest.kt` recording the golden image for the compact editor, reviewed against the Timeline and trim reference image

**Checkpoint**: User Stories 1 to 3 work independently

---

## Phase 6: User Story 4 - Trim, split, duplicate, delete, and audio editing (Priority: P2)

**Goal**: Trim handles with duration badge, split at playhead, duplicate, delete, Detach audio, and independent editing and replacement of detached or added audio, all undoable, with originals untouched.

**Independent Test**: Trim a clip's start and end, split another, duplicate and delete, detach a video's audio and trim, move, mute, and replace it, then undo and redo each step and confirm timeline and preview match; checksum the source file before and after.

### Tests for User Story 4

- [ ] T089 [P] [US4] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/TrimCommandsTest.kt`: `TrimStart`/`TrimEnd` bounds, 100 ms minimum, photo clips lengthened and shortened between 500 and 60000 ms, restoring trimmed footage, later clips shift
- [ ] T090 [P] [US4] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/SplitDuplicateDeleteTest.kt`: split halves are contiguous and keep attached audio, duplicate rules, delete removes attached audio but not detached audio, split-then-join property
- [ ] T091 [P] [US4] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/DetachAudioTest.kt`: creates a `Detached` clip with the same range and start, clears `attachedAudio`, rejects clips without sound, undo restores
- [ ] T092 [P] [US4] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/AudioClipCommandsTest.kt`: `MoveAudioClip` overlap snaps to the next free position with a `Snapped` notice, `SetMuted`, `ReplaceAudio` keeps position and length, shorter replacement emits `LengthChanged`
- [ ] T093 [P] [US4] Write failing `core/model/src/test/kotlin/<pkg>/core/model/engine/UndoRedoAllCommandsTest.kt`: invariant 4 (`undo(apply(c, t)) == t`) for every command
- [ ] T094 [P] [US4] Write failing Compose UI test `feature/editor/src/androidTest/kotlin/<pkg>/feature/editor/TrimAndAudioFlowTest.kt`: drag handles (including lengthening a photo), badge updates live, minimum length stop, split, detach, replace, delete detached audio to keep only added audio, overlap snap message, undo/redo button states

### Implementation for User Story 4

- [ ] T095 [US4] Implement `TrimStart`/`TrimEnd` in `core/model/src/main/kotlin/<pkg>/core/model/engine/TrimCommands.kt` to pass T089
- [ ] T096 [US4] Implement `Split`, `Duplicate`, and `Delete` in `core/model/src/main/kotlin/<pkg>/core/model/engine/SplitDuplicateDelete.kt` to pass T090
- [ ] T097 [US4] Implement `DetachAudio` in `core/model/src/main/kotlin/<pkg>/core/model/engine/DetachAudio.kt` to pass T091
- [ ] T098 [US4] Implement `MoveAudioClip`, `SetMuted`, and `ReplaceAudio` in `core/model/src/main/kotlin/<pkg>/core/model/engine/AudioClipCommands.kt` to pass T092 and T093
- [ ] T099 [P] [US4] Create `TrimHandles.kt` (white handles, live drag, stops at minimum length, preview shows the frame at the handle, photo clips can also be lengthened up to 60 s) and `DurationBadge.kt` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ui/`
- [ ] T100 [P] [US4] Create `VideoClipToolBar.kt` (Split, Speed dimmed, Volume dimmed, Detach only when attached audio exists, Duplicate, Delete) and `AudioClipToolBar.kt` (Split, Mute, Replace, Duplicate, Delete) in the same package
- [ ] T101 [P] [US4] Create `MutedBadge.kt` and the "Detached" label for audio clips in the same package, matching the Detached audio selected design
- [ ] T102 [US4] Extend `EditorViewModel.kt` and `EditorContract.kt` with trim, split, duplicate, delete, detach, replace, and mute events, tool bar derivation from selection, and `LengthChanged` and `Snapped` messaging
- [ ] T103 [US4] Implement the replace-audio flow: audio picker (Add media audio tab or system picker) returning a media item to `ReplaceAudio` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ReplaceAudioFlow.kt`
- [ ] T104 [US4] Integrate trim handles, selection outline, and the two tool bars into `TimelineView.kt` and `EditorScreen.kt`, including dimmed undo/redo states (depends on T099 to T103)
- [ ] T105 [US4] Add screenshot tests recording golden images for the Timeline and trim (clip selected) and Detached audio selected states in `EditorScreenshotTest.kt`, reviewed against the matching reference images
- [ ] T106 [US4] Add instrumented test `core/data/src/androidTest/kotlin/<pkg>/core/data/OriginalsUnchangedTest.kt`: checksum of source files identical before and after a full edit session (SC-007)

**Checkpoint**: User Stories 1 to 4 work independently

---

## Phase 7: User Story 5 - Foldable and large-screen layouts (Priority: P1)

**Goal**: The designed unfolded multi-pane editor, tabletop posture, and two-pane projects view; state retention across fold, unfold, rotation, and resize; hinge avoidance; cover screen support; drag and drop; keyboard shortcuts.

**Independent Test**: Edit on the cover screen, unfold mid-playback, then half-fold into tabletop; project, playhead, selection, and playback stay unchanged at every step and nothing interactive sits under the hinge.

Note: US5 delivers the expanded and tabletop layouts; the adaptive foundation (T021 to T024) and compact layouts are built earlier so every screen is adaptive from the start (constitution Principle VIII).

### Tests for User Story 5

- [ ] T107 [P] [US5] Write failing `app/src/androidTest/kotlin/<pkg>/app/ConfigChangeRetentionTest.kt`: rotate, resize, enter split-screen, and fold/unfold via the foldable emulator test APIs while playing; assert same project, playhead, selection, and no activity recreation
- [ ] T108 [P] [US5] Write failing `feature/editor/src/test/kotlin/<pkg>/feature/editor/HingeAvoidanceTest.kt`: no interactive bounds intersect a separating hinge or occlusion in the tabletop and expanded layouts
- [ ] T109 [P] [US5] Write failing `feature/editor/src/androidTest/kotlin/<pkg>/feature/editor/KeyboardAndDragDropTest.kt`: Space, Left/Right, Delete, undo/redo chords; Media panel-to-timeline drag inserts at the drop position; mouse and stylus can select, drag trim handles, scrub, and reorder like touch; hover highlight and wheel scrolling of the timeline
- [ ] T110 [P] [US5] Write failing `feature/projects/src/androidTest/kotlin/<pkg>/feature/projects/ProjectsTwoPaneTest.kt`: selecting a project shows details and media in the right pane without navigation

### Implementation for User Story 5

- [ ] T111 [P] [US5] Create `ToolRail.kt` and `MediaPanel.kt` (media grid with Import button) in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ui/expanded/`
- [ ] T112 [P] [US5] Create `InspectorPanel.kt` (clip name, length, trim details; Video/Audio/Speed tabs with inactive controls dimmed) in the same package
- [ ] T113 [US5] Create `ExpandedEditorLayout.kt` composing rail, Media panel, preview with transport, inspector, and a full-width timeline with labeled lanes (depends on T111, T112)
- [ ] T114 [P] [US5] Create `TabletopEditorLayout.kt` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ui/tabletop/` (preview above the hinge; previous clip, play, next clip, split; timeline; tool bar below)
- [ ] T115 [US5] Make `EditorRoute.kt` choose compact, expanded, or tabletop from `AdaptiveScaffold` and keep a single `EditorViewModel` across arrangements (depends on T113, T114)
- [ ] T116 [US5] Implement `HingeAvoidance.kt` in `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/adaptive/` to pass T108 and apply it in the expanded and tabletop layouts
- [ ] T117 [P] [US5] Create `ProjectDetailPane.kt` (thumbnail, clip count, length, format, Open editor, Duplicate, Export dimmed, Delete, project media grid) in `feature/projects/src/main/kotlin/<pkg>/feature/projects/ui/`
- [ ] T118 [US5] Create `ProjectsTwoPane.kt` and wire it into `ProjectsScreen.kt` for expanded arrangements to pass T110 (depends on T117)
- [ ] T119 [P] [US5] Implement Media panel-to-timeline drag and drop in `feature/editor/src/main/kotlin/<pkg>/feature/editor/ui/expanded/DragDrop.kt` and drop handling in `TimelineView.kt`
- [ ] T120 [P] [US5] Create `AddMediaPanel.kt` in `feature/importmedia/src/main/kotlin/<pkg>/feature/importmedia/ui/`: large centered panel over the editor on expanded width, the half below the hinge in tabletop, 6 grid columns on expanded and 5 on medium width (3 on compact) (FR-027a)
- [ ] T121 [P] [US5] Write `feature/importmedia/src/androidTest/kotlin/<pkg>/feature/importmedia/AddMediaLayoutTest.kt`: panel on expanded, lower half in tabletop, phone layout with 5 columns on medium, 6 columns in the expanded panel, 3 on compact; selection survives arrangement changes
- [ ] T122 [P] [US5] Write `feature/editor/src/test/kotlin/<pkg>/feature/editor/MediumWidthLayoutTest.kt` and `feature/projects/src/test/kotlin/<pkg>/feature/projects/MediumWidthLayoutTest.kt`: medium width uses the compact layouts with wider content; multi-pane starts at 840 dp (FR-027b)
- [ ] T123 [US5] Apply the Projects tabletop rule in `ProjectsScreen.kt` (two-pane on one side of the hinge, compact layout if that area is too narrow) (FR-027b)
- [ ] T124 [P] [US5] Implement `KeyboardShortcuts.kt` in `feature/editor/src/main/kotlin/<pkg>/feature/editor/` (active only when a hardware keyboard is present) to pass T109
- [ ] T125 [US5] Add instrumented test `app/src/androidTest/kotlin/<pkg>/app/CoverScreenFlowTest.kt` on the cover-screen profile: open a project, trim a clip, unfold, assert continuity (FR-026); adjust paddings and touch targets in the compact composables where needed
- [ ] T126 [US5] Measure layout change time after fold, unfold, and rotation inside `ConfigChangeRetentionTest.kt` (must be under 1 second, SC-009) and confirm `MainActivity` handles configuration changes (see T006)
- [ ] T127 [US5] Add screenshot tests for Foldable unfolded editor, Foldable tabletop posture, and Foldable projects and media in `feature/editor/src/test/kotlin/<pkg>/feature/editor/FoldScreenshotTest.kt` and `feature/projects/src/test/kotlin/<pkg>/feature/projects/FoldProjectsScreenshotTest.kt`

**Checkpoint**: All five user stories work independently

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates from the constitution and success criteria

- [ ] T128 [P] Accessibility pass: content descriptions, 48dp targets, TalkBack order, large-font layouts on every screen and arrangement; fix findings in the affected composables
- [ ] T129 [P] Create `benchmark/` module with startup and scrolling macrobenchmarks (including a project with 100+ clips), a 1080p playback frame-rate check for SC-003, and a 20-file import timing check for SC-004
- [ ] T130 Implement low-storage handling: surface the `LowStorage` warning on failed autosave, keep last good rows, and retry after space is freed (`core/data/src/main/kotlin/<pkg>/core/data/autosave/AutosaveWriter.kt`, `feature/editor/.../ui/EditorTopBar.kt`)
- [ ] T131 [P] Add `app/src/test/kotlin/<pkg>/app/ManifestPermissionsTest.kt` asserting the merged manifest declares no network permission (FR-028, offline)
- [ ] T132 [P] Add `core/data/src/androidTest/kotlin/<pkg>/core/data/ProcessDeathRecoveryTest.kt`: edit, kill the process within the debounce window, reopen, and assert at most 5 seconds of work is lost (SC-005)
- [ ] T133 [P] Record third-party and bundled asset licenses (fonts, test media, libraries) in `licenses/README.md`
- [ ] T134 [P] Add `docs/architecture.md` summarizing modules, state flow, and how arrangements map to layouts
- [ ] T135 Run the full `quickstart.md` manual scenarios 1 to 14 on the owner's physical Samsung Fold (cover screen, inner display, flat and half-open, split-screen, pop-up window), plus one phone profile and one tablet emulator profile; every scenario must pass on the Fold with no workaround; record results and the exact device model, OS version, and window sizes in `specs/001-project-media-timeline/checklists/validation.md`
- [ ] T136 Design review with the designer using the screenshot results; record approved deviations in `design/reference/README.md`
- [ ] T137 Run `./gradlew :core:model:test testDebugUnitTest connectedDebugAndroidTest verifyScreenshots lint` and fix remaining failures

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup; blocks all user stories
- **US1 (Phase 3)**: After Foundational
- **US2 (Phase 4)**: After Foundational; uses the editor stub from US1 (T043) to hand results back
- **US3 (Phase 5)**: After Foundational; consumes clips added by US2's commands (T051) and the project session
- **US4 (Phase 6)**: After US3 (builds on the timeline UI and selection)
- **US5 (Phase 7)**: After US3 for the editor layouts and after US1 for the projects two-pane; US4's tool bars appear in the expanded layout once available
- **Polish (Phase 8)**: After the desired stories are complete

### Within Each Story

- Tests are written first and MUST fail before implementation
- Engine commands before view models; view models before screens; sub-composables before the assembling screen
- Spikes (T049, T068) gate the tasks that depend on them

### Parallel Opportunities

- Setup: T003 to T005, T007, T008
- Foundational: T009, T010, T012 to T015, T017, T019, T021, T025, T027, T030, T032, T033 (different files)
- US1 tests T034 to T036 together; sub-composables T039 to T041 together
- US2 tests T045 to T048; T053, T054, T057 to T059 together
- US3 tests T063 to T067; generators T072 to T074; sub-composables T079 to T084
- US4 tests T089 to T094; UI parts T099 to T101
- US5 tests T107 to T110 and T121 to T122; panels T111, T112, T114, T117, T119, T120, T124
- Polish: T128, T129, T131 to T134
- After Foundational, US1 and US2 can proceed in parallel with different people; US3 starts once T051 lands

---

## Parallel Example: User Story 4 tests

```text
Task: "T089 Write failing TrimCommandsTest in core/model/src/test/kotlin/<pkg>/core/model/engine/TrimCommandsTest.kt"
Task: "T090 Write failing SplitDuplicateDeleteTest in core/model/src/test/kotlin/<pkg>/core/model/engine/SplitDuplicateDeleteTest.kt"
Task: "T091 Write failing DetachAudioTest in core/model/src/test/kotlin/<pkg>/core/model/engine/DetachAudioTest.kt"
Task: "T092 Write failing AudioClipCommandsTest in core/model/src/test/kotlin/<pkg>/core/model/engine/AudioClipCommandsTest.kt"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup, Phase 2 Foundational
2. Phase 3 (US1) then Phase 4 (US2) then Phase 5 (US3): a user can create a project, import media, and arrange and play clips. Stop and validate here (quickstart scenarios 1 to 4, 9).
3. Phase 6 (US4) adds trimming and audio editing, completing the feature's core promise.
4. Phase 7 (US5) delivers the foldable layouts. Foldable support is a release requirement (constitution Principle VIII), so the feature is not releasable until Phase 7 is done, even though US5 comes last in build order.

### Incremental Delivery

- Each phase ends with a checkpoint that can be demoed on a phone emulator; US5 is demoed on a foldable emulator.
- Run the screenshot tests at every checkpoint to catch design drift early.

## Notes

- [P] tasks touch different files and have no dependency on an unfinished task.
- Commit after each task or logical group.
- Features outside this scope (Speed, Volume, Audio tab, Text, Overlay, Effects, Filters, Export, Templates) stay dimmed with the "coming soon" message.
