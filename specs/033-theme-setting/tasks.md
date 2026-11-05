# Tasks: Theme Setting

- [X] T001 [US1] Add `ThemeChoice` and `AppSettings.theme` in `core/model/src/main/kotlin/com/petruvideo/core/model/AppSettings.kt`
- [X] T002 [US1] Test then add `isDark` in `core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/PetruTheme.kt` with `core/designsystem/src/test/.../ThemeChoiceTest.kt`
- [X] T003 [US3] Add `setTheme` and reading with fallback in `core/data/.../repo/SettingsRepository.kt`; extend `SettingsRepositoryTest.kt`
- [X] T004 [US1] Add `themechoice_*` strings (English, Spanish) in `core/designsystem/src/main/res/values{,-es}/strings.xml` and `projects_theme` in `feature/projects` strings
- [X] T005 [US1] Add the Theme section to `SettingsScreen.kt`, wire `SettingsViewModel.kt` and `SettingsRoute.kt`; update every `SettingsScreen(` call in tests
- [X] T006 [US1][US2] Use the choice in `app/.../MainActivity.kt` for the theme and the system bars
- [X] T007 Add Spanish and light Settings screenshots, run detekt, lint, unit tests, Paparazzi verify, assembleRelease
- [X] T008 Update CHANGELOG, README if it lists settings, bump version to 0.7.0, commit
