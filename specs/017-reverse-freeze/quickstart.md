# Quickstart: Reverse and Freeze Frame

1. Unit: `gradle --offline testDebugUnitTest` (freeze and reverse commands, mapping).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios: freeze mid-clip and at both ends; freeze with speed, transform, filter and keyframes; reverse a 3 s clip (frame order, sound order), cancel, turn off, trim and split a reversed clip, export both.
5. Deferred until the user is back: fold and tabletop checks, a timing check of reverse on the phone, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (two new), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 280 tests; the only failures of the full run were two accessibility cases of the reverse progress (a Cancel button measured 46 dp), fixed and rerun green with the accessibility suite.
- Freeze: the exported still equals the frame at the playhead and the video continues after it. Reverse: the renderer and the export show the frames in reverse order and keep the sound.
- Open: fold and tabletop checks, a timing check of reverse on the phone, a listening check of the reversed sound, designer review (T015).
