# Quickstart: Exports List

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline :core:data:connectedDebugAndroidTest :core:export:connectedDebugAndroidTest :feature:export:connectedDebugAndroidTest
```

## Results so far

Emulator Pixel_7_Pro_API_34, 2026-11-01: JVM tests, goldens, detekt, lint and all connected suites pass. A real export adds exactly one history record with the saved video's location; failed, cancelled and unsaved exports add none. Not done: T008 (device checks, designer review).
