# Tasks: Filters and Color Adjustments

**Tests**: included, test-first on core logic. `M`=core/model, `D`=core/data db, `P`=core/media, `E`=feature/editor. Device checks deferred.

## Phase 1: Foundational
- [ ] T001 Baseline green (`gradle --offline testDebugUnitTest detekt`)
- [ ] T002 [P] Failing tests `core/model/.../FilterTest.kt` and `ColorMathTest.kt`: neutral identity, intensity 0 equals identity, B&W equal channels, ranges clamped, order, brightness offset in elements 12 to 14
- [ ] T003 `M/Filter.kt` (FilterLook, ClipFilter, limits) and `M/ColorMath.kt`; `filter` on `VideoClip`
- [ ] T004 Room v7 (entity columns, mappers, `AutoMigration(6, 7)`, schema 7), migration test and round trip
- [ ] T005 Spike S1 (emulator, export test): brightness and B&W through `LiveRgbMatrix`, vignette overlay corner darkening, neutral equality; record in research

## Phase 2: US1 look (P1)
- [ ] T006 [P] Command tests `FilterCommandsTest.kt` (look, intensity, remove, rejects audio and text, undo)
- [ ] T007 `M/engine/FilterCommands.kt`
- [ ] T008 `P/filter/LiveRgbMatrix.kt`, `P/filter/VignetteOverlay.kt`, builder wiring, `setClipFilterLive` in engine and fake, refresh
- [ ] T009 VM (Filters tab, selection tools Filters and Adjust, panel state), `E/ui/FilterPanels.kt` Looks panel with tiles, MainToolBar tab active; golden and Compose test

## Phase 3: US2 adjust (P1)
- [ ] T010 [P] Tests for `SetFilterAdjustments`, `ResetFilterAdjustments`; VM tests (live preview, one undo step, reset keeps look)
- [ ] T011 Commands, Adjust panel, inspector sections, "Fl" badge

## Phase 4: US3 apply to all and rules (P2)
- [ ] T012 [P] Tests for `ApplyFilterToAll` (one undo step, dimmed with one clip), split and duplicate keep the filter
- [ ] T013 `ApplyFilterToAll` and rules in `SplitDuplicateDelete`

## Phase 5: US4 layouts, saved, exported (P3)
- [ ] T014 [P] Export tests: average color with a filter differs from none as the matrix predicts; neutral equals no filter; vignette darkens corners
- [ ] T015 [P] Persistence test in `OriginalsUnchangedTest`; accessibility and arrangement tests
- [ ] T016 Tabletop placement, goldens, run all suites on the emulator and stop it
- [ ] T017 detekt, lint, quickstart results, mark tasks
- [ ] T018 Device checks and designer review: deferred
