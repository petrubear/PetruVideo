# Quickstart: Clip Transforms

1. Unit: `gradle --offline testDebugUnitTest` (transform math, crop rules, commands, edits).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios: a left/right split picture rotated, flipped, cropped, and all three (export frames); a crop with a preset shape; a transformed clip with a keyframed push-in, a filter, and a transition; split, speed change, relink to a different shape.
5. Deferred until the user is back: fold and tabletop checks, a feel check on the phone, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (two new), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 267 tests; the two failures of the first run (a stale generated factory in the app suite, and a 40 dp crop screen item) were fixed and their suites rerun green.
- Export frames confirm turn, mirror, crop, turn then crop, and keyframes on the turned picture.
- Open: fold and tabletop checks, a feel check of the crop screen on the phone, designer review (T012).
