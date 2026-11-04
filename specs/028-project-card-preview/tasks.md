# Tasks: Project Card Preview

**Input**: Design documents from `/specs/028-project-card-preview/` (plan.md, spec.md, research.md, data-model.md, quickstart.md)

**Tests**: Included, because the constitution asks for tests first on core logic (Principle V) and screenshot tests for UI.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US3 from spec.md (the motion preview story was removed in clarification)

---

## Phase 1: Setup

- [X] T001 Add `implementation(libs.coil.compose)` to `feature/projects/build.gradle.kts` and check the module builds with `gradle --offline :feature:projects:compileDebugKotlin`

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: the picture file and the one view that draws it.

- [X] T002 [P] Write the failing test `core/media/src/test/kotlin/com/petruvideo/core/media/thumbs/ProjectThumbFilesTest.kt` (JVM; the file naming and cleanup moved into `ProjectThumbFiles`, because a bitmap cannot be made on the JVM): a first clip gives a file named `<projectId>-<key>.png` under `project-thumbs` and `setThumbnail` gets its path; changing the first clip (other media or other trimmed start) gives a new path and deletes the old file; no clip, or unreadable media, saves null and deletes the file; the same first clip twice makes no new file
- [X] T003 Change `core/media/src/main/kotlin/com/petruvideo/core/media/thumbs/ProjectThumbnailUpdater.kt`: file name `<projectId>-<hash of mediaItemId and sourceStartMs>.png`, delete the project's previous picture file when the path changes or the result is null, and expose the current key so a restart does not skip a missing file (make T002 pass)
- [X] T004 [P] Add `ProjectThumbnail(project, modifier, content)` in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ui/ProjectThumbnail.kt`: Coil `AsyncImage` of `project.thumbnailRef` with `ContentScale.Crop` and a fade, drawing the gradient (`PlaceholderThumbnail`) while loading, when the ref is null, and when the file is missing or fails; keep `PlaceholderThumbnail.kt` as the fallback and update its doc comment

**Checkpoint**: a project with a ref shows its picture, any other shows the colored box.

---

## Phase 3: User Story 1 - See what a project is by its picture (Priority: P1) MVP

**Goal**: all four project views show the picture.

**Independent Test**: scenario 1 in quickstart.md.

- [X] T005 [P] [US1] (Done as: the existing screenshot tests still pass with no picture; a picture is not rendered in a static snapshot, so it was checked on the phone) Add screenshot tests in `feature/projects/src/test/kotlin/com/petruvideo/feature/projects/ProjectsScreenshotTest.kt` and `FoldProjectsScreenshotTest.kt` for a project with a picture file and one without, at compact, medium and expanded widths and tabletop posture; record the goldens
- [X] T006 [US1] Use `ProjectThumbnail` in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ui/ProjectCard.kt` (keep the badges and the selected border)
- [X] T007 [P] [US1] Use `ProjectThumbnail` in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ui/ContinueEditingCard.kt`
- [X] T008 [P] [US1] Use `ProjectThumbnail` in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ui/ProjectRow.kt`
- [X] T009 [P] [US1] Use `ProjectThumbnail` in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ui/ProjectDetailPane.kt` (both places)
- [X] T010 [US1] The picture is decorative (the project name is beside it), so it has no content description; touch targets unchanged. Original text: content descriptions for the picture (the project name) and check touch targets stay at 48 dp in the four views; update the goldens from T005

**Checkpoint**: MVP. Install and check scenario 1 on the phone.

---

## Phase 4: User Story 2 - The picture follows the first clip (Priority: P1)

**Goal**: the picture and its file always match the first clip and the project's life.

**Independent Test**: scenarios 2 and 5 in quickstart.md.

- [X] T011 [P] [US2] Write the failing test in `core/data/src/test/kotlin/com/petruvideo/core/data/repo/ProjectThumbnailCleanupTest.kt`: deleting a project deletes its picture file; duplicating gives the copy its own file or a null ref, and the original's file is untouched
- [X] T012 [US2] Implement the cleanup in `core/data/src/main/kotlin/com/petruvideo/core/data/repo/RoomProjectRepository.kt` (`deleteProject` deletes the file at `thumbnailRef`; `duplicateProject` sets the copy's ref to null so the background pass makes its own) and make T011 pass
- [X] T013 [US2] (Existing refresh call confirmed; test added in `EditorViewModelTest`) Check `feature/editor/src/main/kotlin/com/petruvideo/feature/editor/EditorViewModel.kt` (call at `thumbnails.refresh`) runs after every edit that changes the first clip (move, trim start, replace, delete all) and add or extend a test in `feature/editor/src/test/kotlin/com/petruvideo/feature/editor/` using a fake `ThumbnailRefresher` to prove it is called with the new timeline

**Checkpoint**: scenarios 2 and 5 pass.

---

## Phase 5: User Story 3 - Projects never opened still get a picture (Priority: P2)

**Goal**: the Projects screen makes missing pictures in the background.

**Independent Test**: scenario 3 in quickstart.md.

- [X] T014 [P] [US3] Extend `feature/projects/src/test/kotlin/com/petruvideo/feature/projects/FakeProjectsRepository.kt` with `loadProject` returning a timeline for a project id and a fake `ThumbnailRefresher` recording calls
- [X] T015 [US3] Write the failing tests in `feature/projects/src/test/kotlin/com/petruvideo/feature/projects/ProjectsViewModelTest.kt`: a project with clips and no ref is refreshed once; a project with a ref or no clips is not; projects are refreshed one at a time in list order; a project that fails is not retried in the same session; work stops when the ViewModel is cleared
- [X] T016 [US3] Implement in `feature/projects/src/main/kotlin/com/petruvideo/feature/projects/ProjectsViewModel.kt`: inject `ThumbnailRefresher`, collect the project summaries, and run a sequential background pass over `clipCount > 0 && thumbnailRef == null` with `loadProject` then `refresh`, skipping ids in a session `failed` set; make T015 pass
- [X] T017 [US3] Provide `ThumbnailRefresher` to `ProjectsViewModel` through Hilt if not already available (check `app/src/main/kotlin/com/petruvideo/app/di/MediaModule.kt`) and run `gradle --offline :feature:projects:kspDebugKotlin :app:kspDebugKotlin --rerun-tasks` if Hilt fails on stale generated code

**Checkpoint**: scenario 3 passes on the phone with a template project that was never opened.

---

## Phase 6: Polish & Cross-Cutting

- [X] T018 Run `gradle --offline :feature:projects:testDebugUnitTest :core:media:testDebugUnitTest :core:data:testDebugUnitTest :feature:editor:testDebugUnitTest`, then detekt and lint, and fix findings
- [ ] T019 Verify on the phone over USB: quickstart scenarios 1 to 7, including fold and unfold, split screen, the cover screen and scrolling with many projects; note any problem in plan.md
- [X] T020 Bump `appVersion` to 0.3.0 in `app/build.gradle.kts`, add the 0.3.0 entry to `CHANGELOG.md`, and commit with the tag `v0.3.0` (do not push)

---

## Dependencies & Execution Order

- Phase 1 first, then Phase 2 (T002 and T004 in parallel, T003 after T002).
- US1 (Phase 3) needs T003 (for the file) and T004 (the view). It is the MVP.
- US2 (Phase 4) needs T003; T011 to T012 and T013 are independent of US1 and can run beside it.
- US3 (Phase 5) needs T003 and benefits from US1 to be visible; its tests (T014 to T015) can start after Phase 2.
- Polish last.

```text
T001 -> T002 -> T003 -> US1 (T005..T010)
            \-> T004 -/
T003 -> US2 (T011 -> T012, T013)
T003 -> US3 (T014 -> T015 -> T016 -> T017)
all -> T018 -> T019 -> T020
```

## Parallel Example

After T003: T005, T007, T008, T009 (US1 views), T011 (US2 test) and T014 (US3 fake) touch different files and can run together; T006 and T010 touch shared goldens, so they go after.

## Implementation Strategy

1. **MVP**: Phases 1, 2 and 3. Projects that were opened in the editor show their picture. Install and check.
2. Add US2 so the picture and its file stay correct, then US3 so older and template projects get one.
3. Finish with Polish and release 0.3.0.
