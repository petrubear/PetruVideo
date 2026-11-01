# Quickstart: Overlay (Picture in Picture)

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline connectedDebugAndroidTest
```

## Results so far

Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, goldens, detekt, lint and every connected suite pass. Export tests check an overlay's window, place and size, two overlays with their own settings, and a video overlay; preview tests play video and photo overlays to the end with live changes. Not done: T007 (device checks, designer review).
