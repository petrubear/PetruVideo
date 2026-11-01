# Implementation Plan: Voice-over Recording

**Branch**: `013-voice-over` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

A singleton `VoiceRecorder` (core:media) records the microphone to a streamable AAC (ADTS) file in the app's own storage and reports a live level. The editor view model runs the flow as a small state machine: permission, 3 second count-in, recording (preview plays from the playhead), stop. The result is a normal audio-lane clip made by a pure `AddVoiceOver` command (at the exact start time, no snapping), so every audio tool, saving, preview and export already work. A one-line journal beside each recording lets an interrupted or lost take be recovered as a clip. No database change.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Android `MediaRecorder` (AAC, ADTS container), Compose BOM 2026.09, Media3 1.11.1 (plays the clip), Hilt
**Storage**: files under `filesDir/voiceovers/<recordingId>.aac` plus `pending.json`; media items keep the file URI; no Room change
**Testing**: JUnit4 + kotest (command, limit math, permission logic, view model state machine with a fake recorder), Paparazzi goldens, androidTest (real recording on the emulator with the permission granted, export with a voice-over, accessibility)
**Target Platform**: Android minSdk 26 (`MediaRecorder` ADTS output exists since API 16)
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: Principle III: level polling every 50 ms off the main thread work, no frame drops in the preview while recording
**Constraints**: offline; microphone permission just in time; recording ends when the app leaves the foreground; detekt limits (line 120, complexity 15, method 60)
**Scale/Scope**: one recording at a time, up to 30 minutes; about 12 new files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin | PASS. `MediaRecorder`, no extra library. |
| II Non-destructive | PASS. The recording is a new file; the project only references it; undo removes the clip. |
| III Real-time preview | PASS with measure. Playback and recording run together; frame-drop check recorded in the quickstart. |
| IV Incremental slices | PASS. Builds on 001 and 005 audio clips only. |
| V Test-first on engine | PASS. The command, limit math and state machine are pure and tested first. |
| VI Clean architecture | PASS. Recorder interface and fake in tests; UI state in the view model. |
| VII Privacy and safety | PASS. Permission asked at first use with a reason; the file stays in app storage and is deleted with the project; low storage and interruptions are handled. |
| VIII Adaptive and foldable-first | PASS by design. The recording bar sits in the bottom bar area in every layout; the take survives rotation and fold changes because the recorder is a singleton owned outside the activity. Device and fold checks stay deferred. |

Post-design re-check: PASS. No violations, so no Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/013-voice-over/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../VoiceOver.kt                  limits, VoiceOverLimit math (next clip, 30 min, storage)
core/model/.../engine/AddVoiceOver.kt        command: exact start, no overlap, minimum 0.5 s
core/media/.../voiceover/VoiceRecorder.kt    interface + MediaRecorder implementation, level, journal, store
core/media/.../voiceover/VoiceOverStore.kt   files, journal, recovery list, orphan sweep
core/data/.../repo/ProjectRepository.kt      allMediaUris() for the sweep
app/.../di/VoiceOverModule.kt                singleton recorder and store
app/src/main/AndroidManifest.xml             RECORD_AUDIO
feature/editor/.../VoiceOverFlow.kt          permission launcher and settings hand-off
feature/editor/.../MicAccess.kt              pure permission-state logic
feature/editor/.../EditorViewModel.kt, EditorContract.kt   events, state, flow, recovery
feature/editor/.../ui/RecordingBar.kt        count-in, meter, timer, Stop, Cancel
feature/editor/.../ui/MainToolBar.kt         Record voice-over on the Audio tab bar
```

**Structure Decision**: extend existing modules; no new module.

## Complexity Tracking

None.

## Spike findings (T001, emulator Pixel_7_Pro_API_34, 2026-11-01)

- A take made with the recorder's settings (AAC, ADTS, 44.1 kHz, mono, 96 kbps) plays in Media3 as an audio clip to its end, with the clip length given by the measured time.
- A take cut off in the middle of a frame still plays its recorded part, so a recording lost with the process is recoverable.
- The length estimated from the file size (`size * 8 / 96000`) is within 250 ms of the real length.
- Opening the microphone crashes the headless emulator on this Mac (the device goes offline), so the real-microphone test is skipped on emulators and the real recorder is a device check; the file format is tested with a synthetic take.
