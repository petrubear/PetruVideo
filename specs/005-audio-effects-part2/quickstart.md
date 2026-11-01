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
(none yet)
