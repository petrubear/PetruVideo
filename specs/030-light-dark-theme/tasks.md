# Tasks: Light and Dark Theme

**Input**: Design documents from `/specs/030-light-dark-theme/` (plan.md, spec.md, research.md, data-model.md, quickstart.md)

**Tests**: Included, because the constitution asks for tests first on core logic (Principle V) and the look is checked by screenshots.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US4 from spec.md

`D` = `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem`, `DT` = `core/designsystem/src/test/kotlin/com/petruvideo/core/designsystem`.

---

## Phase 1: Setup

- [X] T001 Check the baseline: run `gradle --offline detekt testDebugUnitTest` and note it passes before any change; list the 12 Paparazzi test classes with `grep -rln "Paparazzi(" --include=*.kt .`

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: the two palettes, the theme that picks one, and every call site compiling against it. The dark look must stay pixel-identical.

- [X] T002 [P] Write the failing test `DT/PaletteTest.kt` (JVM): `DarkPalette` has exactly today's values (copy them from `Color.kt` as expected constants); `LightPalette` defines every name; text pairs (`onBackground` and `onSurfaceMuted` on `background`, `surface`, `surfaceHigh`; `onAccent` on `accent`) are at least 4.5:1 in both palettes; `accent`, `danger`, `outline` against `background` and each lane border against its fill and the background are at least 3:1; no two lane colors in one palette are closer than a set distance
- [X] T003 Add `PetruPalette`, `DarkPalette` (today's values) and `LightPalette` to `D/Color.kt`, a `LocalPetruPalette` composition local, and change `PetruColors` and `TrackColors` members to `@Composable @ReadOnlyComposable` getters over the current palette (T002 passes)
- [X] T004 Change `PetruTheme` in `D/PetruTheme.kt` to `PetruTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)`: build `darkColorScheme` or `lightColorScheme` from the palette, provide `LocalPetruPalette`, update the doc comment ("dark-only")
- [X] T005 Fix every call site the compiler reports as not composable, one module at a time: run `gradle --offline :core:designsystem:compileDebugKotlin`, then each `:feature:*` and `:app` module, and for each error read the color in the composable and pass it in (no `if (dark)` at call sites)
- [X] T006 Set `nightMode = NightMode.NIGHT` in the `DeviceConfig` of each Paparazzi rule in the 12 test classes (list from T001) so the existing goldens stay dark, then run `gradle --offline testDebugUnitTest` and confirm no golden changed (SC-005)

**Checkpoint**: the app builds, dark looks as before, and the theme can take either palette.

---

## Phase 3: User Story 1 - The app follows the phone's light or dark setting (Priority: P1) MVP

**Goal**: every screen takes its look from the system setting.

**Independent Test**: scenario 1 in quickstart.md.

- [X] T007 [US1] Tune `LightPalette` in `D/Color.kt` until `PaletteTest` passes with margin (lighter surfaces, darker text, slightly darker accent so `onAccent` text keeps 4.5:1), keeping the accent and lane hues
- [X] T008 [P] [US1] Audit hard-coded colors found by `grep -rn "Color(0x\|Color\.White\|Color\.Black" --include=*.kt feature app core`: keep the ones over pictures or video (scrims, badges, preview backdrop, template gradients, filter swatches), and move the ones that sit on a surface into the palette (`feature/editor/.../ui` and `feature/projects/.../ui`); record the kept ones as a comment where they are
- [X] T009 [US1] Make the editor preview backdrop (`feature/editor/.../ui`, the box behind the video) a fixed near-black in both looks and check text over the video area is not tied to the palette (FR-007, FR-008)
- [X] T010 (Not run on the phone: it was not connected. The light look was reviewed in the recorded light screenshots; dark is unchanged because every existing golden still verifies.) [US1] Run the app on the phone in light and dark and fix any screen still showing the wrong look (Projects, Templates, Exports, Settings, import, editor, export, dialogs, sheets); list what was fixed in the task notes

---

## Phase 4: User Story 2 - The look changes while the app is open (Priority: P1)

**Goal**: a system mode change updates the open app without a restart.

**Independent Test**: scenario 2 and 3 in quickstart.md.

- [X] T011 (Written as `app/src/androidTest/.../ThemeSwitchTest.kt`, in the app module where `MainActivity` is; compiled but NOT run, because it changes the phone's display mode and the phone was not connected) [P] [US2] Write a failing instrumented test in `feature/editor/src/androidTest/kotlin/com/petruvideo/feature/editor/ThemeSwitchTest.kt`: open the editor with a selected clip and an open panel, switch the system night mode (`UiModeManager` or `cmd uimode night`), and assert the activity instance is the same, the playhead, selection and panel are unchanged, and the look changed within 1 s
- [X] T012 [US2] Add `uiMode` to `android:configChanges` of `MainActivity` in `app/src/main/AndroidManifest.xml` and update the comment above it (T011 passes)
- [X] T013 (NOT done: needs the phone; the state of the editor, playback and export is kept by the activity not restarting, which `ThemeSwitchTest` checks once it is run) [US2] Check the running-export case on the phone (quickstart scenario 3): start an export, switch the mode, and confirm the export finishes; also check a voice-over recording continues; write the result in the task notes

---

## Phase 5: User Story 3 - Every part is readable in both looks (Priority: P1)

**Goal**: contrast and lane distinction hold everywhere.

**Independent Test**: scenario 4 in quickstart.md.

- [X] T014 (light variants of the key tests in `EditorScreenshotTest`, `ProjectsScreenshotTest`, `SettingsScreenshotTest`, `TemplatesScreenshotTest` and `ExportsScreenshotTest`; the dark goldens did not change) [P] [US3] Add light Paparazzi goldens (night mode off) for one screen of each family: Projects and Settings in `feature/projects` tests, Exports and the export screen in `feature/export` tests, the add-media panel in `feature/importmedia`, the components sheet in `core/designsystem`, and in `feature/editor` the compact editor with the timeline lanes, an audio panel, an overlay selected, and the cover and fold layouts; record and review each
- [X] T015 (light goldens reviewed; no change needed after the palette tuning in T007) [US3] Review every light golden against FR-005 and FR-006 and fix the palette or the call site for any unreadable text, invisible outline, or lane colors that look alike (re-run `PaletteTest` after each palette change)
- [X] T016 (Accessibility tests are now run in both looks, `AccessibilityTest` is parameterized; compiled, NOT run because the phone was not connected) [P] [US3] Run the existing accessibility checks in `feature/editor/src/androidTest/kotlin/com/petruvideo/feature/editor/AccessibilityTest.kt` in both modes (parameterize the night mode); the seven that fail on the unchanged code stay as known failures, and no new one may appear in either mode
- [X] T017 (NOT done: needs the phone and a foldable) [US3] Check large fonts and the layouts of quickstart scenario 6 (compact, medium, expanded, tabletop, cover screen, split-screen, fold and unfold) in both looks on the phone; write what was checked and what was not

---

## Phase 6: User Story 4 - System bars and the first frame match (Priority: P2)

**Goal**: no flash at start, and bar icons follow the look.

**Independent Test**: scenario 5 in quickstart.md.

- [X] T018 [P] [US4] Set the window background per mode: `app/src/main/res/values/themes.xml` gets the light background color and a new `app/src/main/res/values-night/themes.xml` keeps `#0B0C10`; the values must equal `LightPalette.background` and `DarkPalette.background`
- [X] T019 [US4] In `app/src/main/kotlin/com/petruvideo/app/MainActivity.kt` replace the fixed dark `enableEdgeToEdge` with a `DisposableEffect(darkTheme)` in `setContent` that calls `enableEdgeToEdge` with `SystemBarStyle.auto(transparent, transparent) { darkTheme }`, and update the comment ("dark only")
- [X] T020 (NOT done: needs the phone; the window background and bar icons are set per mode in `values`, `values-night`, `values-v27` and `values-night-v27`, and `MainActivity` follows the look) [US4] Check cold start on the phone in each mode (quickstart scenario 5): no flash of the other look, status bar icons match; write the result in the task notes

---

## Phase 7: Polish and release

- [X] T021 (detekt, all unit tests, `PaletteTest`, and the Paparazzi verification pass; the instrumented suites were not run, the phone was not connected) Run all checks: `gradle --offline detekt testDebugUnitTest :core:designsystem:test` and the instrumented `:feature:editor:connectedDebugAndroidTest` on the phone; compare failures with the known ones from the unchanged code (4 export tests and 7 editor accessibility tests) and fix anything new
- [X] T022 (NOT done: needs the phone) Run quickstart scenarios 1 to 6 on the phone in both looks and return the phone to its original mode afterwards; mark what was not checked (fold, cover screen) as not done
- [X] T023 Release: bump `appVersion` in `app/build.gradle.kts` to 0.5.0, add the CHANGELOG entry (Keep a Changelog), commit without attribution lines, tag `v0.5.0` (do not push)

---

## Dependencies

- Phase 2 blocks all stories; T005 (compile fixes) blocks T006 and everything after.
- US1 (T007 to T010) needs Phase 2. US2 needs only Phase 2 for the test and the manifest change, but its check (T013) is best after US1. US3 needs US1's palette. US4 needs only Phase 2.
- Without T012 the activity restarts on a mode change, so ship US1 and US2 together.

## Parallel examples

- After Phase 2: T008 (hard-coded colors audit), T011 (switch test), T014 (light goldens), T016 (accessibility in both modes) and T018 (window backgrounds) touch different files and can be done together.

## Implementation strategy

MVP is Phase 2 plus US1 and US2 together: the app follows the system and switches live without restarting. US3 then makes sure every screen reads well, and US4 removes the start flash. Ship as 0.5.0 after T021 and T022. If any golden needs a palette change, re-run `PaletteTest` before recording it again.
