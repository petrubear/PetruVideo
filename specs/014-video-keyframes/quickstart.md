# Quickstart: Keyframes for Video Clips

1. Unit: `gradle --offline testDebugUnitTest` (pose, clamp, commands, edit math).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios: push-in (full frame to 200% over 3 s) plays and exports; a fade to 0% opacity; split in the middle keeps the motion; speed 2x halves keyframe times; transition between two animated clips.
5. Deferred until the user is back: fold and tabletop checks, designer review, a feel check on the phone.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (one new), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 256 tests, 0 failures (2 skipped, including the real-microphone test).
- Export frames match the pose function for zoom, pan and fade, and an animated clip still dissolves into the next clip.
- Open: fold and tabletop checks, a feel check on the phone, designer review (T015).
