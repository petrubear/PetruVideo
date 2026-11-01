# Quickstart: Audio Ducking

1. Unit: `gradle --offline testDebugUnitTest` (planner, processor, commands).
2. Static: `gradle --offline verifyPaparazziDebug detekt lintDebug`.
3. Emulator: start the AVD as in earlier features, then `gradle --offline --no-build-cache connectedDebugAndroidTest --continue`. If the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
4. Scenarios: music under a voice-over with a pause (export level difference within 2 dB of the chosen amount); trigger at the very start; two ducked clips; muted trigger; split and speed change of the music; fade and volume together.
5. Deferred until the user is back: fold and tabletop checks, a listening check on the phone, designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens (one new), detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 260 tests, 0 failures (2 skipped, including the real-microphone test).
- Export: music under a quieter sound is more than 65% lower than alone and comes back afterwards; the same project without ducking is unchanged.
- The preview applies the same planner after each load; it has no dedicated device-level test beyond the shared export path.
- Open: fold and tabletop checks, a listening check on the phone, designer review (T014).
