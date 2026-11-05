# Implementation Plan: Audio Compressor

**Branch**: `034-audio-compressor` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Summary

Add a `Compressor` to a clip's `AudioFx`, process it in the existing per-clip `VoiceFxChain` (so preview and export use the same code), store it in the existing `fx_` columns, change it through a new `SetCompressor` command, and show it as a new tool in the Audio panel and the unfolded inspector.

## Constitution Check

No new module or dependency, local only, tests first on core logic (Principle V), same undo path as other audio tools. PASS.

## Design

- **Model** (`core/model/AudioFx.kt`): `CompressorPreset` (Gentle, Voice, Podcast, Punchy, Custom) and `Compressor(enabled, preset, thresholdDb, ratioTenths, attackMs, releaseMs, levelDb)`. Strength is derived: `strength = 50 * (-threshold / 40) + 50 * ((ratio - 1) / 7)`, each part clamped to 0..1, and `withStrength(s)` sets threshold `-0.4 s` dB and ratio `1 + 0.07 s`; so one set of values, and the shortcut round trips within 1 point. `normalized()` clamps ranges and recomputes the preset by matching the four named ones. Defaults equal the Voice preset with `enabled = false`.
- **Engine** (`core/model/engine/AudioFxCommands.kt`): `SetCompressor` like `SetEqualizer`; works for video, overlay and audio clips through `AudioFxCommand`.
- **Storage**: seven new defaulted columns in `AudioFxColumns`, Room schema 24 with `AutoMigration(23, 24)`; mappers read the preset by name (unknown means Custom).
- **DSP** (`core/media/dsp/CompressorDsp.kt`): feed-forward compressor, detection on the loudest channel in dB, 6 dB soft knee, attack/release smoothing of the gain in dB, manual level as smoothed linear makeup, output through the existing soft limiter. In `VoiceFxChain` between the equalizer and bass; `active` includes it. It is not a "heavy" stage, so pressure never bypasses it.
- **Editor**: `AudioToolKind.Compressor`, a row in the Audio list, `CompressorControls` (switch, preset chips, Strength, Level, collapsed Advanced with Threshold, Ratio, Attack, Release, Reset), a section in the inspector, a "Cp" badge, `commitFx` and `resetFx` cases.
- **Strings**: editor and designsystem, English and Spanish.

## Tests

Model (strength round trip, presets, ranges, default), command, mappers, DSP (gap reduction at least 40%, strength 0 is a pass-through, neutral level, no clipping), chain order, screenshots of the controls, migration test.
