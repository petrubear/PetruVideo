# Quickstart: Audio Tools Part 2

## Prerequisites
Emulator `Pixel_7_Pro_API_34` (start it only for a run, stop it with `adb emu kill`), Gradle offline.

## Automated
```bash
./gradlew --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
./gradlew --offline :core:media:connectedDebugAndroidTest :feature:editor:connectedDebugAndroidTest :core:export:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```
Expected: DSP, command, migration and view model tests pass; an emulator export of a clip with all tools on matches the preview chain (length, audio present, level and spectrum checks).

## Manual (Fold SM-F976B and a mid-range phone)
1. Fade: set 2 s in and 3 s out on a music clip; hear the ramps; seek into the middle of a fade and hear the right level.
2. Noise reduction: voice clip with hiss; Low, Medium, High each reduce the hiss; speech stays clear.
3. Equalizer: Bass boost, then drag High down; hear both; Reset.
4. Voice effects: Pitch +5, add Echo, add Robot, turn each off; no gaps while dragging.
5. Save, force-close, reopen: all settings as set. Export and listen: same as the preview.
6. Five clips with several tools: preview holds 30 fps and audio is smooth, or the app lowers quality and says so.
7. Unfold, half-fold, rotate with a panel open: it stays open with the same values; nothing under the hinge.
8. Cover screen: the tool bar shows Audio and the list is usable at 2x font.

## Results so far
Emulator Pixel_7_Pro_API_34 (Android 14), 2026-11-01: all JVM tests, Paparazzi goldens, detekt and lint pass; connected suites for core:data, core:media, core:export, feature:editor, feature:projects and feature:export pass. Findings:
- Spike S1: the seek offset given at flush is honoured, so fades are right after a seek.
- Noise reduction treats a perfectly steady tone as noise (a pure tone is removed); speech-like bursts and music with dynamics keep their level. The panel says it is for voice.
- Export of a clip with fade, noise reduction, equalizer and all four voice effects ends with the start and end near silent, the same as the preview chain.
Not yet done (needs the Fold or listening): T027 and T033 (tuning by ear), T041 (fps and CPU with 5 clips), T053 (manual checks, A/V sync, seek in a fade, fold layouts), T054 (designer review), and the ramp marker on faded clip ends (FR-023 part).
