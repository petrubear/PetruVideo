# Implementation Plan: Audio Tools Part 2 - Fades, Noise Reduction, EQ, Voice Effects

**Branch**: `005-audio-effects-part2` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-audio-effects-part2/spec.md`

## Summary

Add Fade, Noise reduction, Equalizer and four stackable voice effects (Echo, Robot, Pitch, Bass) to video clips with sound and audio clips. Technical approach: extend the pure-Kotlin engine with five small values on both clip types and their commands; add columns with a Room v5 auto migration; implement the sound in pure Kotlin DSP classes (testable on the JVM) wrapped by one new Media3 `AudioProcessor` (`VoiceFxProcessor`: noise reduction, EQ, bass, pitch, robot, echo) placed before the existing reverb processor, and extend the existing gain processor with fade ramps. The shared `CompositionBuilder` from 004 feeds preview and export, so export renders the same sound. The tool bar gets one Audio entry opening a tool list; each tool gets a panel built from the 003 reverb panel pattern.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17 toolchain (unchanged)

**Primary Dependencies**: Media3 1.11 (CompositionPlayer, Transformer, AudioProcessor), Room 2.8, Compose BOM 2026.09, Hilt, Paparazzi. No new libraries.

**Storage**: Room schema 4 to 5: on `video_clip` and `audio_clip`, columns `fadeInMs`, `fadeOutMs`, `nrEnabled`, `nrStrength`, `eqPreset`, `eqLow`, `eqMid`, `eqHigh`, `echoEnabled`, `echoDelayMs`, `echoRepeats`, `robotEnabled`, `robotIntensity`, `pitchEnabled`, `pitchSemitones`, `bassEnabled`, `bassAmount`, all with neutral defaults; auto migration plus a test. Originals untouched.

**Testing**: JVM tests for commands, presets, clamping and each DSP stage (neutral settings equal the input, frequency response of EQ and bass, pitch ratio by zero-crossing or FFT peak, echo tap timing, noise level drop on synthetic noise plus tone, no clipping, no discontinuity on live changes); Room migration test; view model tests; Compose and accessibility checks (incl. 2x font); Paparazzi goldens; emulator playback and export tests (preview and export match); Fold and mid-range phone measurements of CPU and fps with 5 clips using several tools.

**Target Platform**: Android, minSdk 26. Primary foldable: Samsung Fold SM-F976B.

**Project Type**: mobile-app (multi-module Gradle build)

**Performance Goals**: change heard within 100 ms with no gap; 30 fps at 1080p with up to 5 clips using several tools; no audio dropouts; UI under 100 ms; pipeline latency added by the new stages at most about 26 ms (noise reduction 11 ms, pitch 15 ms), so audio stays in sync (A/V within 40 ms).

**Constraints**: offline; no activity restart on fold or rotation, open panel survives; at most 5 seconds of work lost; one undo step per gesture; tails cut at clip end; export renders in the same order and with the same code as the preview.

**Scale/Scope**: 7 audio tools in one list, 5 new panels (Fade, Noise reduction, Equalizer, Voice effects with four sections), 6 EQ presets, badges, inspector sections; touches `core:model`, `core:data`, `core:media`, `feature:editor`, `core:designsystem` (only if a control is missing).

## Constitution Check

| Principle | Gate | Status |
|-----------|------|--------|
| I. Native Android, Kotlin-First | Custom Media3 AudioProcessors in Kotlin, no new frameworks | PASS |
| II. Non-Destructive Editing | All tools are clip settings with commands and undo; sources untouched | PASS |
| III. Real-Time Preview Performance | DSP on the audio thread with fixed buffers, no per-block allocation, live parameters; measured on the Fold and a mid-range phone; pressure monitor bypasses heavy stages with a notice | PASS (verify, risk R1) |
| IV. Incremental Delivery | Independent slice on top of 003; automation, curve choice, stacking order control excluded | PASS |
| V. Test-First on Core Logic | Commands, presets and DSP written test-first on the JVM; export checks | PASS |
| VI. Clean Architecture and Modularity | Engine Android-free; DSP pure Kotlin in `core:media`; panel state in the view model | PASS |
| VII. Privacy, Safety, Resources | On-device only; heavy stages can be bypassed under critical pressure | PASS |
| VIII. Adaptive and Foldable-First UI | Audio list and panels work in compact, medium, expanded, tabletop; panel survives fold and rotation | PASS |

No violations. Post-design re-check: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/005-audio-effects-part2/
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
├── Models.kt                      # fade, noise, eq, voice on both clips
├── AudioFx.kt                     # Fade, NoiseReduction, Equalizer, EqPreset, VoiceFx (Echo, Robot, Pitch, Bass), limits
└── engine/
    ├── AudioFxCommands.kt         # SetFade, SetNoiseReduction, SetEqualizer, ApplyEqPreset, SetVoiceFx
    ├── SplitDuplicateDelete.kt, AudioClipCommands.kt, SpeedVolumeCommands.kt, trim commands   # fade rules, copy settings
    └── TimelineInvariants.kt      # ranges

core/data/.../db/                  # Entities, Mappers, PetruDatabase v5 (auto migration), schemas/5.json

core/media/.../preview/ and dsp/
├── dsp/NoiseReductionDsp.kt       # spectral gate, pure Kotlin
├── dsp/EqualizerDsp.kt            # three biquads
├── dsp/PitchShiftDsp.kt           # granular two-head pitch shifter
├── dsp/EchoDsp.kt, RobotDsp.kt, BassDsp.kt
├── VoiceFxProcessor.kt            # Media3 processor: chain of the stages above, live parameters, latency compensation
├── GainAudioProcessor.kt          # adds fade in/out ramps
├── composition/CompositionBuilder.kt   # chain order [VoiceFx, Reverb, Gain+Fade], bypass flags
├── pressure/PreviewQuality.kt     # voiceFx flag
└── PreviewEngine.kt               # setClipFxLive(clipId, fx), setClipFadeLive

feature/editor/.../feature/editor/
├── EditorContract.kt, EditorViewModel.kt   # Audio entry, tool list, panel state, events
└── ui/
    ├── AudioToolList.kt           # list of the seven tools (sheet)
    ├── FadePanel.kt, NoiseReductionPanel.kt, EqualizerPanel.kt, VoiceEffectsPanel.kt
    ├── ClipBadge.kt               # badges with collapse into a count
    ├── ClipToolBar.kt             # one Audio button replaces Volume, Mute and Reverb
    ├── expanded/InspectorPanel.kt # sections for all tools
    └── tabletop/TabletopEditorLayout.kt

tests: matching src/test and src/androidTest in each module
```

**Structure Decision**: Reuse the multi-module layout. No new modules.

## Processing Order

Per clip: noise reduction, equalizer, bass, pitch, robot, echo, reverb, then fade and volume. Reasons: clean the signal before shaping it; tone before effects; echo and reverb after pitch so repeats are not re-pitched; fade and volume last so they silence tails and repeats. The same order is used by preview and export (one builder).

## Risks

- **R1**: CPU cost of noise reduction (FFT) and pitch shifting with several clips on mid-range phones. Mitigation: fixed buffers, a spike measuring 5 clips with all tools on the Fold and a mid-range phone, a pressure-monitor bypass of the heavy stages with a notice, and stages skipped entirely when off.
- **R2**: Fade position after seek: a processor sees only the stream, so it must know the clip time. Mitigation: use the stream position offset given at flush (research 2); spike on the emulator; fallback is to count samples from the item start and re-derive on seek.
- **R3**: Speed is applied before the clip's effects (research 3); if the order differs, fade lengths and echo timing scale wrongly. Mitigation: a test with a 2x clip checks fade length in timeline time.
- **R4**: Latency added by FFT and pitch stages breaks A/V sync. Mitigation: small frames (11 ms and 15 ms, research 8); measured on the Fold (T053).
- **R5**: Live parameter changes click. Mitigation: smooth coefficient and gain changes; discontinuity test.
- **R6**: Room v5 on top of v4 and many columns. Mitigation: additive columns with defaults, auto migration, test from v4.
- **R7**: Noise reduction can make speech sound watery or remove music. Mitigation: three conservative strengths, a floor on reduction, a panel note, tuning by ear.
