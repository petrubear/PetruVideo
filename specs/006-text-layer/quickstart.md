# Quickstart: Text Layer

## Automated
```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline :core:data:connectedDebugAndroidTest :core:media:connectedDebugAndroidTest :core:export:connectedDebugAndroidTest :feature:editor:connectedDebugAndroidTest
```
Start the emulator `Pixel_7_Pro_API_34` only for a run and stop it with `adb emu kill`.

## Manual (when the user is back with the Fold)
1. Add text at 3 s, type "Day one", see it in the preview and the lane.
2. Style: bold font, yellow, box at 60%, outline; drag to the bottom third, pinch.
3. Overlap two clips; trim, move, split, delete; undo each.
4. Animate Fade in and Slide down out; play and export; compare a frame.
5. Save, force-close, reopen; unfold and half-fold with a panel open.

## Results so far
Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, Paparazzi goldens, detekt, lint and all connected suites pass (core:data, core:media, core:export, feature:editor, feature:projects, feature:export). Findings:
- Spike S1 passed: composition-level overlay uses composition time, exports with text only, text beyond the video, and a title at 25% from the left.
- Bug fixed: a centered layout wider than the cropped bitmap showed one letter only.
- Bug fixed: snapping to the center while dragging kept the text stuck at the center; the drag now tracks the finger's own position.
- Project list duration still ignores text (SQL summary); not part of this feature.
Not done (device or designer): T045.
