# Quickstart: Effects, Audio Tab, and Templates

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline connectedDebugAndroidTest
```

## Scenarios
1. Two clips, Dissolve 1 s at the cut: play across it, see the blend; the project is 1 s shorter; undo restores.
2. Blur at 60% on a clip: preview and exported frame match.
3. Add music at 2 s: audible in preview and export; fade out works.
4. Templates, Memories with 4 videos: ready project, editable, exports.
5. Split, duplicate, delete around a transition: no dangling transition.
6. Close and reopen: all kept (Migration9To10Test).

Performance check (Principle III): record preview fps with one transition and two effects on the emulator, and on device when the user is back.

Deferred: Fold and device checks, designer review.

## Results

Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, goldens, detekt, lint and every connected suite pass. Export tests check each transition kind, each clip effect, and a filled template; preview tests play every kind to the end and reload while playing. Not done: T019 (device checks, designer review).
