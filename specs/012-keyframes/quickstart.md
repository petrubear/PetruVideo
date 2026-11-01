# Quickstart: Keyframes

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline connectedDebugAndroidTest
```

## Scenarios
1. Overlay with two keyframes 2 s apart (upper right to lower left, 100% to 50%): play across it, see the move and fade; undo each step.
2. Export that project and compare frames at 0 s, 1 s, 2 s with the pose function and with the preview.
3. Text clip with a size keyframe and a Fade in: both apply.
4. Split an animated clip in the middle: each half starts and ends where the whole was.
5. Trim, duplicate, move, close and reopen: keyframes stay right.
6. Dragging on the preview between keyframes makes a keyframe at the playhead.

Performance check (Principle III): record preview fps with three animated clips on the emulator, and on device when the user is back.

Deferred: Fold and device checks, designer review.

## Results

Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, goldens, detekt, lint and every connected suite pass. Export tests compare an animated overlay's place and size at four moments and an animated text clip's move and fade against the baseline; the pose math is checked frame by frame for jumps; preview tests play animated clips and change keyframes while playing. Not done: T015 (device checks, designer review) and the fps check with three animated clips on a real device.

Build note: a stale KSP cache once left `EditorViewModel` out of the app's Hilt component; `--no-build-cache` (or `--rerun-tasks` on `:feature:editor:kspDebugKotlin`) fixes it.
