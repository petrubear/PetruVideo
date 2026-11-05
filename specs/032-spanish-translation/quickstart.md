# Quickstart: Spanish Translation

## Prerequisites

Phone or emulator. Change the language in the phone's settings (System, Languages) or, on Android 13 and newer, in Settings, Apps, PetruVideo, Language. Return the phone to its original language afterwards.

## Automated checks

- `./gradlew lint` (a missing Spanish text is an error)
- `scripts/check-no-literals.sh` (no literal text in screens)
- `./gradlew detekt testDebugUnitTest verifyPaparazziDebug` (English goldens unchanged, Spanish goldens verified)
- `./gradlew :app:connectedDebugAndroidTest` (language switch keeps the activity)

## Phone scenarios

1. **Spanish phone (US1)**: set Spanish; open Projects, Templates, Exports, Settings, import, an editor project, every tool bar tab and panel, export; no English except names and units.
2. **English and other languages (US1)**: set English: nothing changed. Set Japanese: English.
3. **Messages (US2)**: trigger a refused edit, a disabled tool with its reason, a missing file, and an export with its notification; all Spanish.
4. **Plurals and times (US3)**: projects with 0, 1, several clips; edited just now, an hour, yesterday, last month.
5. **Per-app language (US4, Android 13+)**: with the editor open and a panel showing, change the app's language; the texts update within 1 s, the playhead, selection and panel stay, and a running export continues.
6. **Layouts**: compact width, 1.3x font, tool bar, cover screen, tabletop; no cut-off text.
7. **Spanish review**: the maintainer reads every screen and the glossary and notes changes.

## Expected outcome

All scenarios pass and the English app is identical to before.
