# Quickstart: Filters and Color Adjustments

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline :core:data:connectedDebugAndroidTest :core:media:connectedDebugAndroidTest :core:export:connectedDebugAndroidTest :feature:editor:connectedDebugAndroidTest
```
Start the emulator only for a run and stop it afterwards. Device checks (Fold, fps with 10 filtered clips, designer review) are deferred.

## Results so far
Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, goldens, detekt, lint, and all connected suites pass. Findings:
- Export checks: brightness raises the mean luma, black and white gives equal channels, vignette darkens the corners and leaves the center, neutral filters export like no filter.
- Found and fixed a feature 004 bug: a project made only of photos failed to export ("no audio or video track to output"); photo media items now carry their image duration. A regression test exports a photo with and without a filter.
- Editor view model: panel-state builders moved to `EditorStateBuilders.kt` to keep the class under the size limit.
Not done: T018 (device checks, designer review).
