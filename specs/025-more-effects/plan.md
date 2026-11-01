# Implementation Plan: More Effects

Add five values to `EffectKind` and five fragment shaders to the existing `ClipEffectHandle` pass (`core/media/effects/ClipEffects.kt`): Glitch and VHS use the clip time, Grain uses time and aspect, Glow uses aspect. The existing panel lists `EffectKind.entries`, so tiles appear without UI changes. Kinds are stored by name in Room; no migration. Several effects per clip is deferred (it changes storage).

## Results

Full connected suite on the emulator: 312 tests, 0 failures, 3 skipped.
