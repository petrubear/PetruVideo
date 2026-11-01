# Research: Audio Tools Part 2

## 1. DSP implementation: pure Kotlin stages in one processor

**Decision**: Write each stage as a pure Kotlin class (JVM-testable) and run noise reduction, EQ, bass, pitch, robot and echo in one new `VoiceFxProcessor`; keep `ReverbAudioProcessor` and extend `GainAudioProcessor`.
**Rationale**: One short-to-float conversion for six stages; reverb and gain code from 003 and 002 stay untouched except the fade ramp; stages skipped when off cost nothing.
**Alternatives**: One processor per stage (six conversions and buffer copies per block); merge everything with reverb and gain (large refactor of working code); native libraries such as SoundTouch or RNNoise (new dependency, NDK, licensing, harder to test).

## 2. Fade: clip time after a seek

**Decision**: The gain processor computes the clip time from the stream position offset that Media3 provides when the processor is flushed, adds the frames it has processed, and applies the fade curve to the gain. The fade is a smooth curve (raised cosine).
**Rationale**: Fades must be right after a seek into the middle of a fade; a plain sample counter restarts at the item start.
**Alternatives**: Counting frames from flush (wrong after seeks); `VolumeScale`/keyframed gain effects (not available for audio in Media3 1.11). Spike S1 verifies the offset; fallback is a Media3 `SpeedChangingAudioProcessor`-style position lookup or splitting each clip into three items (fade in, body, fade out) at build time, which avoids needing the position at the cost of more items.

## 3. Order of speed and effects

**Decision**: Media3 puts speed change before the clip's custom audio effects, so effects see timeline-rate audio and fade lengths and echo delays are in timeline time.
**Rationale**: Matches spec FR-003. Verified by an emulator test with a 2x clip (R3).
**Alternatives**: Convert lengths by speed inside the processors if the order differs.

## 4. Noise reduction

**Decision**: Short-time spectral gating: 1024-sample frames, 50% overlap, the noise floor estimated per frequency bin from a running minimum over about 1.5 s, gain per bin from over-subtraction, with strengths Low, Medium, High = 6, 12, 20 dB maximum reduction and smoothing over time and frequency to limit musical noise.
**Rationale**: Works offline on device, pure Kotlin, one FFT size, steady-noise focus matches the spec assumption.
**Alternatives**: RNNoise or a neural model (stronger but needs a model, NDK, and more CPU); simple noise gate or high-pass (does not clean noise under speech).

## 5. Equalizer

**Decision**: Three biquad filters: low shelf at 200 Hz, peaking at 1 kHz (Q 0.7), high shelf at 4 kHz, gains -12 to +12 dB, coefficients smoothed on change; output soft-limited. Presets: Flat 0/0/0, Voice boost -3/+4/+2, Bass boost +8/0/-1, Treble boost -1/0/+8, Warm +4/+1/-4, Telephone -12/+6/-8 (starting values, tuned by ear).
**Rationale**: Cheap, stable, matches the three-band UI.
**Alternatives**: 5 or 10 bands (more UI; not asked).

## 6. Pitch shift

**Decision**: A granular delay-line pitch shifter with two cross-faded read heads (window about 40 ms) that changes pitch without changing duration; pitch ratio 2^(semitones/12); parameter changes are live.
**Rationale**: Live changes without flush, small latency, pure Kotlin. Media3's Sonic processor only applies a new pitch on flush (a glitch while dragging).
**Alternatives**: Sonic via `SonicAudioProcessor` (good quality, not live); phase vocoder (better quality, more CPU and latency). Quality is checked by ear; if poor, switch to a WSOLA design behind the same class (spike S2).

## 7. Echo, Robot, Bass

**Decision**:
- Echo: multi-tap delay with `repeats` taps at multiples of the delay, each tap 0.55 times the previous; no tail is emitted after the clip ends.
- Robot: ring modulation of the voice at 70 Hz blended with a short comb filter; intensity sets the blend.
- Bass: low shelf at 120 Hz up to +12 dB plus a gentle soft-clip to add harmonics, amount 0% to 100%.
**Rationale**: Recognisable result, tiny CPU, deterministic for tests.
**Alternatives**: vocoder-based robot (heavier); dynamic bass enhancer (more tuning).

## 8. Latency compensation

**Decision**: Stages report their latency (noise reduction one hop, pitch half a window); the processor drops that many leading frames after a flush and emits the remainder at end of stream, so the output length and timing match the input within one block.
**Rationale**: Keeps A/V within 40 ms (SC-007) and tails cut at the clip end.
**Alternatives**: Ignore the latency (up to about 25 ms drift).

## 9. Preview pressure

**Decision**: Add `PreviewQuality.voiceFx`; when quality is Reduced, noise reduction, pitch and echo are bypassed with the existing notice ("Preview quality lowered and some effects paused to keep playing"). Export always renders every stage.
**Rationale**: Same behaviour as reverb (003) and the constitution's performance principle.
**Alternatives**: Drop frames of video instead (hurts the 30 fps target).

## 10. Tool bar

**Decision**: One Audio button on the clip tool bar for clips with sound opens a tool list (sheet, or inspector sections when unfolded); Speed stays in the tool bar. Volume, Mute and Reverb buttons move into the list; tests for them are updated.
**Rationale**: Clarification answer; keeps the cover-screen bar short.
**Alternatives**: Seven buttons (does not fit); Common plus More (two taps for some tools).

## 11. Persistence of stacked voice effects

**Decision**: Four independent values (on/off plus settings each) stored as flat columns with neutral defaults.
**Rationale**: Auto migration works, queries stay simple, no JSON blobs.
**Alternatives**: One JSON column (no migration help, harder to test).
