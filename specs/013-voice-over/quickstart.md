# Quickstart: Voice-over Recording

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline connectedDebugAndroidTest
```

The emulator must run with audio enabled (no `-no-audio`) for the microphone tests.

## Scenarios
1. Allow the microphone, record 5 s from the 2 s mark over a video: the clip starts at 2 s and plays in the preview.
2. Cancel during the count-in: nothing added.
3. Deny the permission: message; blocked: Open settings.
4. Put the app in the background while recording: the clip keeps what was recorded.
5. Kill the process while recording and reopen the project: the take is recovered.
6. Record up to the next audio clip: it stops there with a message.
7. Trim, fade and apply a voice effect to the clip; export and hear it.

Performance check (Principle III): count dropped preview frames while recording on the emulator, and on device when the user is back.

Deferred: Fold and device checks (real microphone quality, echo with the speaker), designer review.

## Results (2026-11-01)

- Unit tests, Paparazzi goldens, detekt and lint pass.
- Connected suites on the Pixel 7 Pro API 34 emulator: 252 tests, 0 failures (2 skipped, among them the real-microphone test, because the emulator crashes when it opens the microphone).
- Run connected tests with `--no-build-cache`; if the app suite reports "Cannot create an instance of class ...EditorViewModel", rerun KSP with `--rerun-tasks`.
- Device-only checks (microphone quality, echo, latency) stay open as T013.
