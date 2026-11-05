# Tasks: Spanish Translation

**Input**: Design documents from `/specs/032-spanish-translation/`

**Tests**: The checks that guard the work are written first (missing translation, literal texts); unit tests that assert English sentences change to assert ids; Spanish screenshots are added per screen family.

## Format: `[ID] [P?] [Story] Description`

Texts are converted module by module. For each module: add `values/strings.xml` (English, exactly today's text), add `values-es/strings.xml` (Spanish from the glossary), replace each literal with `stringResource`, `pluralStringResource`, or `UiText`, then run the module's tests and screenshots: English goldens must not change.

---

## Phase 1: Setup

- [X] T001 Baseline: run `./gradlew detekt testDebugUnitTest verifyPaparazziDebug` and list the literals per module with `grep -rEn 'Text\(\s*"|contentDescription = "|say\("' --include=*.kt feature core app` (saved as the working list in the task notes)
- [X] T002 Write `docs/glossary-es.md` from the proposed glossary in research.md, add the words found while listing literals (tool names, presets, errors), and note the style rules (neutral Latin American Spanish, no regional slang, impersonal forms)

## Phase 2: Foundational (blocks the modules)

- [X] T003 [P] Add `scripts/check-no-literals.sh` with its allow-list file `scripts/literals-allowed.txt` and a test `scripts/test-check-no-literals.sh` (catches `Text("x")`, `contentDescription = "x"`, `say("x")`, `title = "x"` and `label = "x"` in `src/main` screens; passes on resources and on allow-listed product names)
- [ ] T004 [P] Turn on lint `MissingTranslation` as an error in the convention plugins under `build-logic/convention` for every Android module, and check a deliberately missing Spanish text fails `./gradlew lint`
- [X] T005 Add `UiText` (`Res`, `Plural`, `Raw`, composable `asString()`) in `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/UiText.kt` with a unit test of how ids and arguments are kept
- [ ] T006 [P] Add `res/xml/locales_config.xml` (`en`, `es`) and `android:localeConfig` in `app/src/main/AndroidManifest.xml`; add `locale` to the activity's `android:configChanges` and update its comment; write the instrumented test `app/src/androidTest/.../LanguageSwitchTest.kt` (language change keeps the same activity; compiled, run when a device is connected)
- [X] T007 Move the 4 existing designsystem error texts and `app_name` into the module-prefixed names and add their Spanish; run the English goldens

**Checkpoint**: the checks exist and fail on a missing text or a literal; modules can now be converted.

## Phase 3: User Story 1 and 2 - screens, messages and errors in Spanish (P1) MVP

### Shared and small modules

- [ ] T008 [US1] `core/designsystem`: convert the 21 literals (common actions, component texts, accessibility descriptions) to `ds_` resources with Spanish
- [ ] T009 [US2] `core/export`: the export notification (title, texts, channel name) from resources with Spanish; check with a unit or instrumented test that the channel is created with the current language
- [X] T010 [US1] `feature/export`: convert the export screen and the Exports list (about 28 texts, with plurals) with Spanish; update its screenshot tests; English goldens unchanged
- [X] T011 [US1] `feature/importmedia`: convert the add-media screen and panel (about 14 texts, permission explanations) with Spanish
- [X] T012 [US1] `feature/projects`: convert Projects, detail pane, Templates (names and descriptions), Settings (about 50 texts), including the clip-count plurals and the relative times ("2 min ago", "Yesterday") with Spanish
- [X] T013 [US3] `core/data` and `feature/projects`: the default project name from a provider (`Untitled %d` / `Sin título %d`), the count of untitled projects covering both prefixes, with a repository test for both languages

### Engine reasons

- [ ] T014 [US2] Write the failing tests first in `core/model`, then change `VoiceSplitRules.reasonNot` and every other engine function that returns an English sentence (found by the T001 list for `core/model`) to return an enum or code; map each in the editor with a `when`

### The editor (largest module, in slices)

- [ ] T015 [US1] `feature/editor` tool names: `ToolbarTab`, `ClipTool`, `AudioToolKind`, `TextToolKind` and the other enums with a `label` take `@StringRes labelRes`; the clip tool bar and main tool bar read them; add Spanish
- [ ] T016 [US1] `feature/editor` panels, part 1: volume and speed sheets, audio panels (fade, noise, equalizer, voice effects, reverb, ducking), voice-over, and the labels of presets (`label()` extensions with exhaustive `when`)
- [ ] T017 [US1] `feature/editor` panels, part 2: text, stickers, captions, filters and adjustments, effects and transitions, transform, crop, freeze, reverse, speed curve
- [ ] T018 [US1] `feature/editor` panels, part 3: overlay and keyframes panels, chroma key, masks, canvas background, timeline lane labels, ruler, handles, and every `contentDescription`
- [ ] T019 [US2] `feature/editor` messages: `EditorState.message` becomes `UiText?`; every `say("...")`, notice, refusal, and "coming soon" text in `EditorViewModel` and the helpers becomes a `UiText`; update the view-model tests to compare ids (about 55 assertions) and run them

## Phase 4: User Stories 3 and 4 - plurals, language switching (P2)

- [ ] T020 [US3] Review every text with a count or inserted values for Spanish plural and word order (`<plurals>` and numbered placeholders `%1$s`), and add tests that render 0, 1 and many in both languages
- [ ] T021 [US4] Check the per-app language entry and live switch on a device or emulator on Android 13 or newer with the editor open and an export running; write the result in the task notes

## Phase 5: Checks and release

- [ ] T022 Add Spanish Paparazzi goldens (`DeviceConfig.copy(locale = "es")`) for each screen family at compact width and at 1.3x font (projects, settings, templates, exports, export, add media, editor with tool bar and the audio, text, filter and overlay panels, cover and fold layouts); look at each for cut-off text and shorten the text or relax the layout where needed
- [ ] T023 Make `scripts/check-no-literals.sh` run in `.github/workflows/ci.yml`, and run it plus `./gradlew lint detekt testDebugUnitTest verifyPaparazziDebug`; fix everything it finds
- [ ] T024 Walk the whole app in Spanish and in English on a device or emulator (quickstart scenarios 1 to 6) and list what was not checked
- [ ] T025 Ask the maintainer to review the glossary and the Spanish texts; apply the requested changes
- [ ] T026 Release: bump `appVersion` to 0.6.0, add the CHANGELOG entry, update the README (languages), commit without attribution lines, tag `v0.6.0` locally, do not push

## Dependencies

Phase 2 before every module; T014 before T019; T015 to T018 before T019; T020 after the modules; T022 after the modules. Modules T008 to T013 are independent and can go in any order; the editor slices are done in order because they share files.

## Implementation strategy

MVP is Phase 2 plus T008 to T019: everything translated. The work is mechanical but long; it is kept shippable by converting a module at a time and running its checks, with the release only after all modules and the maintainer's review.
