# Macrobenchmarks

Startup and scroll frame timing (T129). Needs a physical device and a release-like build:

```bash
gradle -Pbenchmark :benchmark:connectedBenchmarkAndroidTest
```

- SC-003 (30 fps playback at 1080p): measured with the debug-only `PlaybackStats` (`adb logcat -s PetruPlayback`); results are in `specs/001-project-media-timeline/research.md`.
- SC-004 (20-file import timing): measured by hand in quickstart scenario 2; see `checklists/validation.md`.
- `scrollTimeline` expects a project with 100+ clips under "Continue editing" (the timeline is the first scrollable node in the editor).
