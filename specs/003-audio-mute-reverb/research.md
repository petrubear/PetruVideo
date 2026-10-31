# Research: Audio Tools - Mute and Reverb

## 1. Reverb implementation

- **Decision**: A custom reverb in pure Kotlin (`ReverbDsp`): Freeverb-style with 8 parallel damped comb filters feeding 4 series all-pass filters per channel, plus a pre-delay line and a dry/wet mix. Wrapped in a Media3 `BaseAudioProcessor` (16-bit PCM, stereo or mono, any sample rate) like the 002 gain processor.
- **Rationale**: Media3 1.11 has no reverb processor. Android's `PresetReverb` and `EnvironmentalReverb` attach to an audio session, not to one clip in a composition, and cannot be tuned per clip. A small DSP class is testable on the JVM and gives the five required settings directly.
- **Alternatives considered**: Platform effects (wrong scope, per session); a native library (new dependency, build complexity); convolution with impulse responses (large assets, no live settings).

## 2. Mapping the settings

- **Decision**: Room size (0 to 100) scales comb delay lengths (about 0.6x to 1.4x of the base lengths); Decay time (0.2 to 10 s) sets comb feedback so the tail falls 60 dB in that time (`g = 10^(-3 * delay / (decay * sampleRate))`); Damping (0 to 100) sets the low-pass in each comb's feedback; Pre-delay (0 to 200 ms) is a delay line before the combs; Mix (0 to 100) is the wet fraction with equal-power crossfade.
- **Rationale**: Physical meaning for each control; decay in seconds matches what users expect.
- **Alternatives considered**: Freeverb's raw "room" and "damp" numbers only (no decay in seconds, spec asks for seconds).

## 3. Live changes

- **Decision**: Parameters live in a small immutable `Reverb` value; the processor reads a volatile reference each block and smooths changes over about 20 ms (ramped mix, feedback, and damping). Changing room size changes delay lengths, which would click, so room size applies with a short crossfade between two filter sets only on commit; during the drag room size updates the feedback and damping only and applies delay lengths on release.
- **Rationale**: Meets the 100 ms target without clicks; delay length changes are the only costly case.
- **Alternatives considered**: Rebuilding the composition per drag step (stutters, misses 100 ms).

## 4. Tail at clip end

- **Decision**: The processor does not flush a tail at end of stream, so the output ends with the input and the tail is cut (clarified). State resets on `flush` (seek, new item).
- **Rationale**: Matches FR-008 and keeps clips independent.

## 5. Mute for video clips

- **Decision**: `muted` on `VideoClip`; the preview treats a muted clip like a clip with `attachedAudio = false` for sound, keeping the flag and settings. Detach copies the mute to the new audio clip. Duplicate and Split keep it.
- **Rationale**: Reuses the existing remove-audio path; no new playback code.

## 6. Persistence

- **Decision**: Room schema 3 adds `muted` to `video_clip` and seven reverb columns to both clip tables with defaults (disabled, preset "default", room 50, decay 1500 ms, damping 50, pre-delay 20 ms, mix 30); auto migration from 2, a test from 2, exported schema committed.
- **Rationale**: Additive and safe for existing projects.

## 7. UI

- **Decision**: Reuse the 002 sheet pattern: a Reverb panel above the clip tool bar with an on/off switch, a row of preset chips, Mix and Decay sliders, and a "More" expander with Room size, Damping, Pre-delay; Reset returns to the preset. Inspector shows all five. The clip tool bar gets Reverb (and Mute for video clips); the group order is Volume, Mute, Reverb.
- **Alternatives considered**: A full-screen effects screen (rejected in 002 for the same reason: timeline and preview must stay visible).

## 8. Reference devices

- **Decision**: Samsung Fold SM-F976B is primary; a mid-range phone is needed for the 5-clip CPU check.
