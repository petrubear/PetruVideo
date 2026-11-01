# Quickstart: Stickers and Shapes

1. Unit: `gradle --offline testDebugUnitTest`.
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: `gradle --offline --no-build-cache connectedDebugAndroidTest --continue` (rerun KSP with `--rerun-tasks` if the app suite reports a missing view model factory).
4. Deferred until the user is back: how the emoji look on the phone, fold and tabletop checks, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (one new; the Text tab bar and captions goldens updated for the new button), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 298 tests, 0 failures (3 skipped: the microphone test and the speech test, which skip on the emulator, and one more).
- Every symbol the phone says it can draw produces visible pixels (`StickerGlyphTest`).
- Open: how the emoji look on your phone, fold and tabletop checks, designer review (T007).
