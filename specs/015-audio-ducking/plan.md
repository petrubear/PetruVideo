# Implementation Plan: Audio Ducking

**Branch**: `015-audio-ducking` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Each `AudioClip` gets a `Ducking(enabled, amountPercent, smoothness)` setting. A pure planner in `core:model` (`DuckingPlanner`) turns the project's trigger sounds into a gain envelope for each ducked clip: it reads the loudness of every trigger (video clips with sound and non-ducked audio-lane clips) from the waveform peaks that already exist (20 per second, cached per media), maps them onto the timeline through trim, position, speed and volume, finds when sound is audible, looks ahead by the attack time, and smooths the gain with attack and release times from the Smoothness setting. The envelope (one float per 20 ms) is applied by the clip's `GainAudioProcessor` on top of volume and fade, in preview (computed in the background after each load, then pushed to the processor) and in export (computed before the composition is built). Room v12 adds three columns to `audio_clip` by auto migration. The Audio panel gets a Ducking section; the audio clip on the timeline gets a badge.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Media3 1.11.1 (`BaseAudioProcessor`, existing `GainAudioProcessor`), Room 2.8 (auto migration 11 to 12), Compose BOM 2026.09, Hilt
**Storage**: Room v12, three columns on `audio_clip`: `duck_enabled`, `duck_amount`, `duck_smoothness`
**Testing**: JUnit4 + kotest (planner: levels, trim, speed, volume, mute, self and two-ducked rules, look-ahead, smoothing, no jumps), `GainAudioProcessor` envelope tests, androidTest (export level measurement in voice and pause sections, migration, round trip, preview analyzer), Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: planning is O(clips x steps), well under 50 ms for a 10 minute project with peaks cached; analysis never blocks playback; the first plan for new media waits for the peak decode, which already runs for waveforms
**Constraints**: offline; detekt limits (line 120, complexity 15, method 60); `EditorViewModel` is large, ducking handlers go through `AudioFxCommands`-style commands and a new small file
**Scale/Scope**: about 14 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Audio processor and existing waveform decoder. |
| II Non-destructive | PASS. Ducking is model data with undo; sources untouched. |
| III Real-time preview | PASS. The envelope is precomputed; the processor does one lookup per frame. Analysis runs off the main thread. |
| IV Incremental slices | PASS. Builds on 004, 008 and 013 only. |
| V Test-first on engine | PASS. The planner is pure and tested first. |
| VI Clean architecture | PASS. Planner and commands in `core:model`, processor and analyzer in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions; all analysis is on device. |
| VIII Adaptive and foldable-first | PASS by design. Reuses the Audio panel and inspector section. Device and fold checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/015-audio-ducking/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Ducking.kt                      Ducking, DuckSmoothness, DuckEnvelope, constants
core/model/.../DuckingPlanner.kt               pure planner: triggers -> envelope for one clip
core/model/.../Models.kt                       AudioClip.ducking
core/model/.../engine/DuckingCommands.kt       SetDucking (enabled, amount, smoothness)
core/model/.../engine/SplitDuplicateDelete.kt, AudioClipCommands.kt   copies keep ducking (data class copy)
core/data/.../db                               columns, mappers, migration 11 to 12
core/media/.../preview/GainAudioProcessor.kt   envelope input
core/media/.../ducking/DuckingAnalyzer.kt      peaks -> planner -> envelopes
core/media/.../composition/CompositionBuilder.kt   passes envelopes to the processors
core/media/.../preview/Media3PreviewEngine.kt  computes and pushes envelopes after load
core/export/.../TransformerRunner.kt           computes envelopes before building
feature/editor/.../ui/AudioPanel.kt            Ducking section (switch, Amount, Smoothness, note)
feature/editor/.../ui/AudioLaneView.kt         badge
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.
