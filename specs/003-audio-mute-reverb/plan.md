# Implementation Plan: Audio Tools - Mute and Reverb

**Branch**: `003-audio-mute-reverb` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-audio-mute-reverb/spec.md`

## Summary

Add Mute for video clips, an audio-tools group (Volume, Mute, Reverb), and a Reverb effect with presets and five settings, on video clips with sound and audio clips. Technical approach: extend the pure-Kotlin engine with a `Reverb` value on both clip types plus `muted` on video clips and three commands; add columns with a Room v3 auto migration; implement reverb as a custom Media3 `AudioProcessor` (Schroeder/Freeverb-style comb and all-pass filters, pure Kotlin DSP, parameters changeable live like the gain from 002); add a reverb panel, badge, and inspector section reusing the 002 sheet pattern.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17 toolchain (unchanged)

**Primary Dependencies**: Media3 1.11 (CompositionPlayer, AudioProcessor), Room 2.8, Compose BOM 2026.09, Hilt, Paparazzi. No new libraries.

**Storage**: Room schema 2 to 3: `muted` on `video_clip`; reverb columns (`reverbEnabled`, `reverbPreset`, `reverbRoom`, `reverbDecayMs`, `reverbDamping`, `reverbPreDelayMs`, `reverbMix`) on `video_clip` and `audio_clip`, with defaults; auto migration plus a test. Originals untouched.

**Testing**: JVM tests for commands, presets, and the reverb DSP (impulse response decays, mix 0 equals input, tail ends with the input, no clipping); Room migration test; view model tests; Compose and accessibility checks; Paparazzi goldens; emulator playback tests; Fold and mid-range phone measurements for CPU and fps with 5 reverb clips.

**Target Platform**: Android, minSdk 26, unchanged. Primary foldable: Samsung Fold SM-F976B.

**Project Type**: mobile-app (multi-module Gradle build)

**Performance Goals**: setting change heard within 100 ms with no gap; 30 fps at 1080p with up to 5 reverb clips; no audio dropouts; UI under 100 ms.

**Constraints**: offline; no activity restart on fold or rotation, open panel survives; at most 5 seconds of work lost; one undo step per gesture; tail cut at clip end.

**Scale/Scope**: 1 effect, 5 presets, 5 settings, 1 panel, 1 badge, 1 inspector section; touches `core:model`, `core:data`, `core:media`, `feature:editor`, `core:designsystem` (toggle and icon only if missing).

## Constitution Check

| Principle | Gate | Status |
|-----------|------|--------|
| I. Native Android, Kotlin-First | Custom Media3 AudioProcessor in Kotlin, no new frameworks | PASS |
| II. Non-Destructive Editing | Reverb and mute are clip settings; sources untouched; commands with undo | PASS |
| III. Real-Time Preview Performance | DSP on the audio thread with fixed buffers and no allocation per block; live parameters; measured on the Fold and a mid-range phone | PASS (verify, risk R1) |
| IV. Incremental Delivery | Independent slice; other effects, automation, user presets excluded | PASS |
| V. Test-First on Core Logic | Commands, presets, and DSP written test-first on the JVM; migration test | PASS |
| VI. Clean Architecture and Modularity | Engine Android-free; DSP is a pure Kotlin class in `core:media` wrapped by the processor; panel state in the view model | PASS |
| VII. Privacy, Safety, Resources | On-device only; the pressure monitor can bypass reverb under heavy pressure with a notice | PASS |
| VIII. Adaptive and Foldable-First UI | Panel and inspector work in compact, medium, expanded, tabletop; panel survives fold and rotation | PASS |

No violations. Post-design re-check: PASS (settings are plain ints in `core:model`; DSP stays behind `PreviewEngine`).

## Project Structure

### Documentation (this feature)

```text
specs/003-audio-mute-reverb/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
├── checklists/requirements.md
└── tasks.md              # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/model/src/main/kotlin/.../core/model/
├── Models.kt                      # Reverb, VideoClip.muted, reverb on both clips
├── ReverbPresets.kt               # five presets and Custom matching
└── engine/
    ├── AudioToolCommands.kt       # SetVideoMuted, SetReverbEnabled, SetReverbSettings, ApplyReverbPreset
    ├── SplitDuplicateDelete.kt, AudioClipCommands.kt   # keep settings; detach copies mute and reverb
    └── TimelineInvariants.kt      # ranges

core/data/.../db/                  # Entities, Mappers, PetruDatabase v3 (auto migration), schemas/3.json

core/media/.../preview/
├── ReverbDsp.kt                   # pure Kotlin Freeverb-style reverb (testable on the JVM)
├── ReverbAudioProcessor.kt        # Media3 processor around ReverbDsp, live parameters
├── Media3PreviewEngine.kt         # mute for video clips, reverb processor per item
└── PreviewEngine.kt               # setClipReverbLive(clipId, reverb)

feature/editor/.../feature/editor/
├── EditorContract.kt, EditorViewModel.kt   # tool Reverb, Mute for video, panel state, events
└── ui/
    ├── ReverbPanel.kt             # presets, Mix, Decay, More (3 sliders), Reset
    ├── ClipBadge.kt               # adds the reverb badge
    ├── ClipToolBar.kt             # Reverb button, Mute for video
    ├── expanded/InspectorPanel.kt # Volume, Mute, Reverb sections
    └── tabletop/TabletopEditorLayout.kt

tests: matching src/test and src/androidTest in each module
```

**Structure Decision**: Reuse the multi-module layout. No new modules.

## Risks

- **R1**: Reverb CPU cost on mid-range phones with several clips. Mitigation: fixed-size buffers, no per-block allocation, a spike measuring 5 reverb clips on the Fold and a mid-range phone, and a pressure-monitor bypass.
- **R2**: A custom `AudioProcessor` must handle format changes and flush/seek correctly (stale tail after a seek). Mitigation: reset the filter state on flush; emulator playback and seek tests.
- **R3**: Live parameter changes can click. Mitigation: smooth parameter changes (ramped mix and feedback) in the DSP and test for discontinuities.
- **R4**: Room v3 on top of v2 before v2 ships. Mitigation: additive columns with defaults, auto migration, test from v2.
