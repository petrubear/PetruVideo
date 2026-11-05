# Implementation Plan: Theme Setting

**Branch**: `033-theme-setting` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Summary

Add a `ThemeChoice` (System, Light, Dark) to the app's stored settings, show it as a three-option control in Settings, and make `MainActivity` pick the dark or light look from it instead of only from the phone. System stays the default and behaves as today.

## Technical Context

**Language/Version**: Kotlin, Jetpack Compose, Hilt. **Storage**: the existing `petruvideo-settings` shared preferences behind `SettingsRepository`. **Testing**: JUnit/Kotest unit tests, Paparazzi screenshots, instrumented repository test. **Constraints**: no restart on change; first frame in the chosen look; English goldens unchanged.

## Constitution Check

No principle is affected: no new module, no new dependency, no network, the setting is local, and logic is covered by tests first (Principle V). PASS.

## Design

- `core/model` `AppSettings.theme: ThemeChoice = System`; `ThemeChoice` enum without labels (labels are string resources, as for `Quality`).
- `core/data` `SettingsRepository.setTheme`; stored as the enum name under `theme_choice`; an unknown or missing value reads as System (covers updates from older versions, FR-002 and SC-004). The repository reads preferences when created, so the first read in `MainActivity` is already correct and there is no flash (FR-005).
- `core/designsystem` a pure function `isDark(choice, systemDark)` so the rule is unit tested.
- `app` `MainActivity` gets the repository by injection, collects `settings`, and passes `isDark(...)` to `PetruTheme` and to the system bar style, replacing the direct `isSystemInDarkTheme()` use (FR-003, FR-006). The activity already handles `uiMode` changes without recreation, so a change is a recomposition only (FR-004).
- `feature/projects` Settings gets a Theme section with the existing `SegmentedControl`; labels `themechoice_<Entry>` live in `core/designsystem` strings next to `quality_*` and are found by `enumLabel` (FR-007); the control already announces its selection to screen readers (FR-009).

## Files

`core/model/.../AppSettings.kt`, `core/data/.../repo/SettingsRepository.kt`, `core/designsystem/.../PetruTheme.kt` (+ strings en/es), `app/.../MainActivity.kt`, `feature/projects/.../settings/{SettingsScreen,SettingsRoute,SettingsViewModel}.kt`, their tests and screenshot tests.
