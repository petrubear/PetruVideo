# Tasks: Text Layer - Titles and Captions

**Input**: Design documents from `/specs/006-text-layer/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-screens.md, quickstart.md

**Tests**: Included (Principle V: commands, bounds and animation math test-first; migration and round-trip; view model, Compose, golden, renderer and export checks).

**Organization**: Grouped by user story. `M` = `core/model/src/main/kotlin/com/petruvideo/core/model`, `T` = `core/media/src/main/kotlin/com/petruvideo/core/media/text`, `B` = `core/media/src/main/kotlin/com/petruvideo/core/media/composition`, `P` = `core/media/src/main/kotlin/com/petruvideo/core/media/preview`, `E` = `feature/editor/src/main/kotlin/com/petruvideo/feature/editor`, `D` = `core/data/src/main/kotlin/com/petruvideo/core/data/db`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallel (different files, no dependencies)
- **[Story]**: US1 add and edit words, US2 style, US3 position on the preview, US4 timeline timing, US5 animation, US6 layouts, saved, exported

Device checks on the Fold are deferred until the user is back; they are not listed here.

---

## Phase 1: Setup

- [X] T001 Run the 005 baseline (`gradle --offline testDebugUnitTest detekt`) and confirm it is green
- [X] T002 [P] Add Text, Style and Animation icons and a text-field style for the dark theme in `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/` (icons and components only)

---

## Phase 2: Foundational (blocks all stories)

- [X] T003 [P] Failing tests in `core/model/src/test/.../TextTest.kt`: defaults, clamping of every range, in plus out animation limiting, effective-length helper, `endMs`
- [X] T004 Add `Text.kt` in `M/` (TextClip, TextStyle, TextFont, TextAlign, Background, Outline, TextAnimation, AnimKind, limits) and `textLane` on `Timeline` in `M/Models.kt` with `totalDurationMs` including text; make T003 pass
- [X] T005 [P] Failing tests in `core/model/src/test/.../engine/TextRowsTest.kt` and implement `engine/TextRows.kt` (first-fit rows, deterministic)
- [X] T006 [P] Extend `engine/TimelineInvariants.kt` with the text ranges and a test
- [X] T007 Room v6 in `D/`: `TextClipEntity` and DAO queries in `Entities.kt` and `Daos.kt`, mapping in `Mappers.kt`, session save and load of the text lane with the other lanes, `PetruDatabase.kt` version 6 with `AutoMigration(from = 5, to = 6)`, `schemas/6.json`
- [X] T008 [P] Migration test `Migration5To6Test.kt` (a v5 project opens with no text clips) and a round-trip test of non-default text clips
- [X] T009 Spike S1 on the emulator in `core/media/src/androidTest/.../TextOverlaySpikeTest.kt`: a composition-level `OverlayEffect` with one `BitmapOverlay` whose settings use alpha 0 outside a time window; verify in preview (`CompositionPlayer`) and export (`Transformer`) that the text window follows composition time, and that returning the same bitmap does not re-upload; record findings in `research.md`
- [X] T010 Run the full existing suite and fix regressions

**Checkpoint**: model, persistence and the overlay approach confirmed.

---

## Phase 3: User Story 1 - Add a title and edit its words (P1) - MVP

**Goal**: add text at the playhead, type words, see them in the preview, undo.

### Tests (write first, must fail)

- [X] T011 [P] [US1] Command tests in `core/model/src/test/.../engine/TextCommandsTest.kt` for `AddText` (5 s default, shortened at the project end but at least 0.5 s, project without video) and `SetTextContent` (multi-line, empty, length limit), undo and redo exact
- [X] T012 [P] [US1] Renderer tests in `core/media/src/androidTest/.../text/TextRendererTest.kt`: non-empty bitmap for words, empty text gives a minimal transparent bitmap, size within the frame minus the margin, RTL and emoji do not crash
- [X] T013 [P] [US1] View model tests in `feature/editor/src/test/.../EditorViewModelTextTest.kt`: Text tab Add text, selection, panel opens, typing is live and commits as one undo step

### Implementation

- [X] T014 [US1] `AddText`, `SetTextContent` in `M/engine/TextCommands.kt`; `ClipSelection.Text` in `E/EditorContract.kt`
- [X] T015 [US1] `T/TextRenderer.kt` (StaticLayout, system font mapping, safe margin, size from permille of frame height) and `T/TextOverlays.kt` (a `BitmapOverlay` per clip with volatile state, bitmap cache, composition `OverlayEffect`); `B/CompositionBuilder.kt` adds the effect from the text lane (frame size from `BuildSpec` or the project format)
- [X] T016 [US1] `P/PreviewEngine.setTextLive(clipId, clip)` and a debounced refresh of the current frame, in `P/Media3PreviewEngine.kt` and the fake in `EditorTestFixtures.kt`
- [X] T017 [US1] Text tab active in `E/ui/MainToolBar.kt` with Add text; `E/EditorViewModel.kt` events `AddText`, `OpenTextTool`, `TextPreview`, `TextCommit`, `TextDismiss`; clip tool bar for text clips in `E/ui/ClipToolBar.kt`
- [X] T018 [P] [US1] `E/ui/TextEditPanel.kt` (multi-line field, count, Done; preview shrinks while typing); golden and Compose test
- [X] T019 [US1] A minimal `E/ui/TextLaneView.kt` showing text clips (selectable, first line of text) and wiring it into `TimelineView.kt`

---

## Phase 4: User Story 2 - Style the text (P1)

**Goal**: font, size, color, alignment, background, outline, live.

### Tests (write first, must fail)

- [X] T020 [P] [US2] Command tests: `SetTextStyle` clamping, Reset, undo; renderer tests: background box and outline change pixels, alignment moves the text, wrapping inside the margin at two frame sizes
- [X] T021 [P] [US2] View model tests: style preview is live, commit is one undo step, Reset keeps words

### Implementation

- [X] T022 [US2] `SetTextStyle` in `M/engine/TextCommands.kt`
- [X] T023 [US2] Renderer: outline pass, rounded background with opacity, alignment, font mapping with fallback
- [X] T024 [P] [US2] `E/ui/ColorPicker.kt` (12 swatches plus custom) and `E/ui/TextStylePanel.kt`; goldens and Compose test
- [X] T025 [US2] View model state and events for Style

---

## Phase 5: User Story 3 - Position the text on the preview (P1)

**Goal**: drag and pinch on the preview with center guides.

### Tests (write first, must fail)

- [X] T026 [P] [US3] Pure tests for placement math in `core/model/src/test/.../TextPlacementTest.kt` (clamping inside the frame margin, snap to center within 8 dp, size limits) and `SetTextPlacement` command tests
- [X] T027 [P] [US3] Compose test for the preview layer gestures (drag and pinch events) in `feature/editor/src/androidTest/.../TextPreviewLayerTest.kt`

### Implementation

- [X] T028 [US3] `SetTextPlacement` and the placement helper in `M/`
- [X] T029 [US3] `E/ui/TextPreviewLayer.kt` (selection box, drag, pinch, guides) over `PreviewSurface` in compact, expanded and tabletop layouts; live events while dragging, one commit on release

---

## Phase 6: User Story 4 - Time the text on the timeline (P2)

**Goal**: rows, move, trim, split, duplicate, delete.

### Tests (write first, must fail)

- [X] T030 [P] [US4] Command tests for `MoveText`, `TrimText` (0.2 s minimum, maximum, never below 0), split, duplicate, delete on text clips, project length follows the last end
- [X] T031 [P] [US4] Lane tests: overlapping clips on separate rows, handles and move gestures in `feature/editor/src/androidTest/.../TextLaneTest.kt`

### Implementation

- [X] T032 [US4] `MoveText`, `TrimText` and text support in `Split`, `Duplicate`, `Delete`
- [X] T033 [US4] `E/ui/TextLaneView.kt` full version (rows, trim handles, long-press move, snap with the playhead), geometry in `TimelineGeometry.kt`, view model events; each lane clip exposes its words, start and length to a screen reader (FR-018)

---

## Phase 7: User Story 5 - Animate in and out (P2)

**Goal**: in and out presets with lengths.

### Tests (write first, must fail)

- [X] T034 [P] [US5] `T/TextAnimator` tests in `core/media/src/test/.../text/TextAnimatorTest.kt`: alpha, offset and scale per kind over time, limiting for short clips, none outside the window; `SetTextAnimation` command tests; split sets the adjacent animations to None

### Implementation

- [X] T035 [US5] `SetTextAnimation` command and `T/TextAnimator.kt`; overlay settings use it per frame
- [X] T036 [US5] `E/ui/TextAnimationPanel.kt` (In and Out chips and length sliders); goldens and Compose test; view model events

---

## Phase 8: User Story 6 - Layouts, saved, exported (P3)

### Tests (write first, must fail)

- [X] T037 [P] [US6] Export test in `core/export/src/androidTest/.../ExportPipelineTest.kt`: a project with two text clips exports with the right length, and decoded frames show text inside its window and none outside; position and size match the plan within 1% at 720p and 1080p
- [X] T038 [P] [US6] Persistence test in `OriginalsUnchangedTest`: text clips reload exactly and sources are untouched
- [X] T039 [P] [US6] Accessibility and arrangement tests for the Text panels (2x font) in `AccessibilityTest.kt`, `ArrangementPanelsTest.kt`

### Implementation

- [X] T040 [US6] Inspector sections in `E/ui/expanded/InspectorPanel.kt` and tabletop placement in `E/ui/tabletop/TabletopEditorLayout.kt`; open panel and typed text survive fold and rotation (state in the view model)
- [X] T041 [US6] Goldens: text lane, each panel and the Text tab on compact (expanded and tabletop goldens not added yet; those layouts reuse the same composables)
- [X] T042 [US6] Emulator run of all connected suites; stop the emulator afterwards

---

## Phase 9: Polish

- [X] T043 [P] Originals-unchanged coverage for all text tools (SC-009)
- [X] T044 Run `gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and fix findings
- [ ] T045 Device checks from `quickstart.md` (Fold, mid-range phone fps with 10 text clips, designer review): deferred until the user is back
- [X] T046 Update `quickstart.md` Results so far and mark the tasks

---

## Dependencies

- Phase 2 blocks everything; T009 (spike) gates T015.
- US1 gives the entry point (Text tab, Add text, edit panel, minimal lane); US2 to US5 build on it.
- US2 and US3 are independent after US1; US4 and US5 are independent after US1; US6 is last.

## Parallel examples

- After T010: T011, T012, T013 together; later T020, T021; T026, T027; T030, T031.

## Implementation strategy

MVP is Phase 1 to 3 (add and edit text with rendering in preview and export). Then style, placement, timing, animation, layouts. The emulator is started only for runs and stopped afterwards.
