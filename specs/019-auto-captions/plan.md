# Implementation Plan: Auto Captions

**Branch**: `019-auto-captions` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`TextClip` gets `caption: Boolean` (made by this tool). A pure `CaptionPlanner` in `core:model` finds spoken phrases in the eligible clips from the waveform peaks (loudness, 20 per second, cached) and maps them to timeline time through trim, position, and speed. A `CaptionService` in `core:media` cuts each phrase's sound (decoded with the existing sound decoder, mixed to mono, resampled to 16 kHz), gives it to a `SpeechTranscriber`, and returns the lines. The only transcriber is Android's on-device recognizer (`SpeechRecognizer.createOnDeviceSpeechRecognizer`, Android 13+, audio given through the recognizer's audio-source extras), so nothing uses the network; it reports whether the language is installed. The command `AddCaptions` adds all lines as caption-style text clips (and removes earlier `caption` clips when Replace is on) in one undo step. Room v16 adds `caption` to `text_clip`. The editor adds an Auto captions button on the Text tab and a panel with language, Replace, Start, progress, Cancel, and the unavailable explanation with a button to the phone's speech settings.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: `android.speech.SpeechRecognizer` (on-device, API 33), `MediaExtractor`/`MediaCodec` (existing sound decoder), Room 2.8 (auto migration 15 to 16), Compose BOM 2026.09, Hilt
**Storage**: Room v16, `text_clip.caption` INTEGER default 0; no audio is stored (phrases are streamed to the recognizer)
**Testing**: JUnit4 + kotest (planner on synthetic peaks: pauses, merging, splitting long phrases, speed/trim/offset mapping, skipped clips; command: caption style, replace only captions, one undo, limit), androidTest (PCM preparation: decode, mono, 16 kHz, range; availability on the emulator reports unavailable without crashing; migration; round trip), view model tests with a fake transcriber, Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26 (feature available from API 33)
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: phrase planning under 100 ms from cached peaks; recognition runs one phrase at a time in the background with progress
**Constraints**: offline and on device only; detekt limits; `EditorViewModel` is large so caption handlers go in a small file; real recognition cannot be tested on the emulator
**Scale/Scope**: about 20 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Android's own recognizer and decoder. |
| II Non-destructive | PASS. Captions are text clips with undo; sources untouched; no audio is kept. |
| III Real-time preview | PASS. Work is in the background; captions are normal text clips. |
| IV Incremental slices | PASS. Builds on 006 (text) and 015 (peaks). |
| V Test-first on engine | PASS. Planner and command are pure and tested first; the recognizer sits behind an interface with a fake. |
| VI Clean architecture | PASS. Planner and command in `core:model`, service and recognizer in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. On-device only; no network; no new permission (the recognizer reads a stream we give it, not the microphone). |
| VIII Adaptive and foldable-first | PASS by design. Panel reuses the sheet and inspector code. Device checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/019-auto-captions/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Text.kt                         TextClip.caption
core/model/.../Captions.kt                     CaptionPhrase, CaptionLine, CaptionPlanner, caption style constants
core/model/.../engine/CaptionCommands.kt       AddCaptions
core/data/.../db                               column, mapping, migration 15 to 16
core/media/.../captions/SpeechTranscriber.kt   interface, availability, AndroidSpeechTranscriber
core/media/.../captions/PhraseAudio.kt         range to mono 16 kHz PCM
core/media/.../captions/CaptionService.kt      plan, cut, recognise, progress, cancel
feature/editor/.../CaptionsEditing.kt          view model helpers
feature/editor/.../ui/CaptionsPanel.kt         language, Replace, Start, progress, unavailable note
feature/editor/.../ui/MainToolBar.kt           Auto captions button on the Text tab
feature/editor/.../EditorRoute.kt              opens the phone's speech settings
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.
