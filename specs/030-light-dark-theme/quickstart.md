# Quickstart: Light and Dark Theme

## Prerequisites

Phone connected over USB (debug build). Switching mode from the shell: `adb shell cmd uimode night yes` and `adb shell cmd uimode night no` (this changes only the display mode setting that the user can also change in quick settings).

## Automated checks

- `gradle --offline :core:designsystem:test` (palette completeness and contrast)
- `gradle --offline testDebugUnitTest` (Paparazzi: dark goldens unchanged, light goldens recorded and verified)
- `gradle --offline :feature:editor:connectedDebugAndroidTest` (mode switch without restart; accessibility checks in both modes)
- `gradle --offline detekt`

## Phone scenarios

1. **Follow the system (US1)**: set light, open Projects, Templates, Exports, Settings, import, an editor project; all light. Set dark; all as before.
2. **Live switch (US2)**: open the editor, select a clip, open the Audio panel, play; switch the mode; the look changes within 1 s, the playhead, selection, panel and playback continue; logcat shows no activity recreation.
3. **Export running (US2)**: start an export, switch the mode; the export finishes and the exported file is identical in length and size class to one made without a switch.
4. **Readable everywhere (US3)**: in both looks open every editor panel, lane, dialog and sheet; check lane colors stay distinct and the playhead and selection are visible.
5. **First frame (US4)**: force-stop, set light, start: no dark flash and dark status bar icons; repeat in dark.
6. **Layouts**: compact, medium, expanded, tabletop, cover screen, split-screen, fold and unfold in both looks.

## Expected outcome

All scenarios pass. Return the phone to its original mode after testing.
