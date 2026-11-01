# Data Model: Audio Tools Part 2

All values live in `core:model` as immutable data classes on both `VideoClip` and `AudioClip`. Defaults are neutral (sound unchanged).

## Fade
- `inMs` (0 .. 30 000, default 0), `outMs` (same). Stored in timeline time. Set-time rule: `inMs + outMs <= clip timeline length`, each at most half the length; the value being set is the one limited. Render-time rule: if a clip later gets shorter (trim, speed), effective fades are reduced to fit; stored values are kept.

## NoiseReduction
- `enabled` (default false), `strength`: Low | Medium | High (default Medium).

## Equalizer
- `preset`: Flat | VoiceBoost | BassBoost | TrebleBoost | Warm | Telephone | Custom (default Flat).
- `lowDb`, `midDb`, `highDb`: -12 .. +12 (default 0). `preset` is recomputed from the numbers, as for reverb. Flat means no processing.

## VoiceFx
- `echo`: `enabled`, `delayMs` 50 .. 1000 (default 250), `repeats` 1 .. 5 (default 3).
- `robot`: `enabled`, `intensity` 0 .. 100 (default 60).
- `pitch`: `enabled`, `semitones` -12 .. +12 (default +4; 0 changes nothing).
- `bass`: `enabled`, `amount` 0 .. 100 (default 50).
Each effect keeps its settings when off.

## Commands (pure, undoable, one step per gesture)
`SetFade(clipId, inMs, outMs)`, `SetNoiseReduction(clipId, value)`, `SetEqualizer(clipId, value)`, `ApplyEqPreset(clipId, preset)`, `SetVoiceFx(clipId, value)`. Rejected for clips without sound (photo, video with detached or no audio).

## Rules in existing commands
- Split: first half keeps fade in only, second half keeps fade out only; all other values copied to both.
- Duplicate, detach audio: copy all values. Replace audio: keeps all values.
- Trim and speed change: re-limit effective fades at render time (stored values kept).
- Invariants: every number in range; `inMs + outMs <= timeline length` for new settings.

## Persistence
Room schema 5 adds the flat columns listed in plan.md to `video_clip` and `audio_clip`, defaults neutral; `AutoMigration(4, 5)`; `schemas/5.json`.

## Preview/export mapping
`BuildSpec` stays; `CompositionBuilder.soundEffects` builds `[VoiceFxProcessor, ReverbAudioProcessor, GainAudioProcessor(percent, fade)]` per clip with sound. Live handles: `voiceFx[clipId]`, `gains[clipId]` (volume and fade).
