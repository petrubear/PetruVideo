# Quickstart: Canvas Background

1. Unit: `gradle --offline testDebugUnitTest`.
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: `gradle --offline --no-build-cache connectedDebugAndroidTest --continue` (rerun KSP with `--rerun-tasks` if the app suite reports a missing view model factory).
4. Deferred until the user is back: how the blur looks and runs on the phone, fold and tabletop checks, designer review.

## Results

Full connected suite on the emulator: 303 tests, 0 failures, 0 errors, 3 skipped (speech recognizer). Static gate (unit, Paparazzi, detekt, lint) green.
