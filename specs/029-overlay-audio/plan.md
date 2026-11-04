# Implementation Plan: Overlay Audio

**Branch**: `029-overlay-audio` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/029-overlay-audio/spec.md`

## Summary

Give a picture-in-picture clip the sound abilities of a base-lane video clip. Today `CompositionBuilder.overlaySequences` removes the overlay's audio and `OverlayClip` has no sound fields. The plan:

1. **Model**: `OverlayClip` gets `attachedAudio`, `muted`, `volumePercent`, `reverb`, `fx` (the same types `VideoClip` uses). New overlays start with sound on; rows already in the database keep it off (column default `0`).
2. **Build**: each sounding overlay adds one extra audio-only sequence (a gap up to its start, then the clip's sound with the same sound effects chain as a lane clip). The video-only overlay sequence stays as it is, so the video input order, `OverlayLayout` and `TransitionPlan` indexes do not change. The preview and the export use the same builder.
3. **Commands**: `SetVolume`, `SetMuted`, the `AudioFxCommand` family and `SetReverb*` accept an overlay id; `DetachAudio` and `SplitVoice` accept an overlay id and make audio-lane clips at the overlay's start. Split, duplicate, and delete already copy the whole overlay, so sound travels with it.
4. **UI**: the overlay's panel shows the same sound controls as a lane clip (volume, mute, effects, detach, split voice).
5. **Preview fallback** (clarified): the preview first tries all overlay sounds; if the multi-input playback cannot run reliably it keeps the picture going, plays the base sound only, and shows a notice. The export always has every overlay sound.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Jetpack Compose

**Primary Dependencies**: Media3 1.11.1 (Transformer, CompositionPlayer), existing `GainAudioProcessor`, `ReverbAudioProcessor`, `VoiceFxProcessor`, Room, Hilt

**Storage**: Room, `overlay_clip` table: new columns `attachedAudio`, `muted`, `volumePercent`, `reverbJson`/reverb fields, `fx` fields; schema 22 to 23 with a migration and defaults that keep old overlays silent

**Testing**: JVM unit tests for the model and commands (test first); `EditorViewModel` tests; Room migration test and repository round trip (instrumented); Paparazzi for the overlay panel; export check on the phone (sound present, level and timing)

**Target Platform**: Android 8.0+ (minSdk 26), phone, foldable, tablet

**Project Type**: mobile-app (multi-module: `core:model`, `core:data`, `core:media`, `core:export`, `feature:editor`)

**Performance Goals**: no extra video decoder per overlay (sound is a separate audio-only input); preview picture never frozen by overlay sound; export mix has no clipping

**Constraints**: offline; off the main thread; the 3-layer audio-lane limit is unchanged (overlay sounds are mixed on their own sequences until detached)

**Scale/Scope**: typically 1 to 4 sounding overlays; one extra audio sequence per sounding overlay

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check |
|---|---|
| I. Native Android, Kotlin-first | Pass: Media3 and existing Kotlin modules. |
| II. Non-destructive editing | Pass: sound settings live in the project model; sources untouched; every change is a `Command`, undoable; schema is versioned with a migration. |
| III. Real-time preview performance | Pass with a stated fallback: audio-only inputs add no video decoder, so the video cost is unchanged. A recorded phone check is required (tasks). Where the preview cannot play every overlay sound, it keeps the picture and says so (FR-012). |
| IV. Incremental delivery | Pass: P1 stories (mix, volume, mute) ship alone; effects, detach and split follow as P2 and P3. |
| V. Test-first on core logic | Pass: command and model tests are written first; export audio presence is checked automatically. |
| VI. Clean architecture | Pass: commands in `core:model` (no Android), builder in `core:media`, UI state through `EditorViewModel`. |
| VII. Privacy and resources | Pass: all on device; no new permission. |
| VIII. Adaptive and foldable-first | Pass: the controls reuse the overlay panel and the lane clip sound controls, which already adapt; verified at compact, medium, expanded and tabletop, and across fold and unfold. |

No violations; Complexity Tracking is empty. Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/029-overlay-audio/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md             # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/model/src/main/kotlin/com/petruvideo/core/model/
├── Overlay.kt                         # OverlayClip: sound fields
└── engine/
    ├── OverlayCommands.kt             # AddOverlay sets sound on
    ├── SpeedVolumeCommands.kt         # SetVolume accepts an overlay id
    ├── AudioClipCommands.kt           # SetMuted, DetachAudio accept an overlay id
    ├── AudioFxCommands.kt             # AudioFxCommand accepts an overlay id
    ├── AudioToolCommands.kt           # SetReverb* accept an overlay id
    └── VoiceSplitCommands.kt          # SplitVoice accepts an overlay id
core/data/src/main/kotlin/com/petruvideo/core/data/db/
├── Entities.kt, Mappers.kt            # new columns and mapping
└── PetruDatabase.kt                   # version 23, migration 22 to 23
core/media/src/main/kotlin/com/petruvideo/core/media/
├── composition/CompositionBuilder.kt  # overlay audio sequences
└── preview/Media3PreviewEngine.kt     # live gain, reverb, fx for overlay ids; fallback and notice
feature/editor/src/main/kotlin/com/petruvideo/feature/editor/
├── EditorViewModel.kt, EditorContract.kt   # events for overlay sound, notice
└── ui/OverlayUi.kt                    # sound controls in the overlay panel
```

**Structure Decision**: extend the existing modules in place; no new module. Tests sit next to the code they cover (`core/model/src/test`, `core/data/src/androidTest`, `feature/editor/src/test`).

## Complexity Tracking

No violations.
