# Quickstart: Speed Ramps

1. Unit: `gradle --offline testDebugUnitTest` (steps, length, mappings, windowing, presets, commands).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios: every preset (length of the export within 100 ms of the editor length); split and trim of a ramped clip (speed at tested moments unchanged); reverse; keyframes and a transition on a ramped clip; sound pitch of a steady tone across a ramp.
5. Deferred until the user is back: fold and tabletop checks, a feel check of the graph on the phone, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (one new), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 285 tests, 0 failures (2 skipped, including the real-microphone test).
- Export length of Bullet and Montage matches the editor within 150 ms.
- Open: pitch measurement across a ramp, fold and tabletop checks, a feel check of the graph on the phone, designer review (T012).

## Gaps closed (2026-11-01)

- The sound of a ramped clip keeps its pitch: an export test compares the zero-crossing rate of the Bullet ramp with the plain export (within 40%), where an unprotected 0.1x slowdown would drop it by 90%.
- The project list shows the real length (video, audio, text, overlays, speed curves, transitions): the length is saved with the project (Room v17) instead of being estimated in the list query. Older projects show the estimate until they are saved once.
- Ducking follows where the sound of a reversed or speed-curved video clip is actually heard.
