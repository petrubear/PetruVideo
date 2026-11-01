# Quickstart: Auto Captions

1. Unit: `gradle --offline testDebugUnitTest` (planner, command, service with a fake recognizer, view model).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios (fake recognizer): phrases at the right times with trim and speed; Replace removes only auto captions; cancel leaves nothing; skipped clips are noted; unavailable states show their reason.
5. Deferred until the user is back: real recognition on the phone (Android 13+ with an on-device language), fold and tabletop checks, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (three new, one updated for the new button), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 291 tests, 0 failures after one test was made independent of the phone (the emulator reported speech recognition as available, so the check no longer assumes it is not).
- Recognition itself is only tested with a fake recognizer; the real recognizer was not run on real speech. The emulator reports an installed language, so a real end-to-end check with generated speech may be possible there later.
- Open: real recognition on your phone, fold and tabletop checks, designer review (T011).
