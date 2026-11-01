# Research: Audio Ducking

## 1. Where the trigger loudness comes from
- **Decision**: the waveform peaks (`WaveformPeaksGenerator`, 20 per second, values 0..1 of full scale, cached on disk by media fingerprint). A trigger is audible at a moment when `peak x volume% / 100 >= 0.04` (about -28 dBFS).
- **Why**: already decoded and cached for the waveform display, so ducking adds no new decoding path; 50 ms resolution is finer than the smoothing times.
- **Rejected**: measuring in the audio processors at play time (a processor sees only its own clip, not the others, and export would differ from preview); a fixed "clip is present" rule (music would stay low through pauses, against the spec).

## 2. The planner
Pure function `DuckingPlanner.envelopeFor(timeline, clipId, peaks)` returns a `DuckEnvelope(stepMs = 20, gains: FloatArray)` for the clip's length.
1. For each 20 ms step of the clip, time `t` = clip start + step. A trigger is every video clip with attached sound that is not muted and has volume above 0, and every audio-lane clip that is not the target, not muted, volume above 0, and whose own Ducking is off.
2. For a trigger covering `t`, the source time is `sourceStart + (t - triggerStart) x speed`; its level is `peaks[source x 20 / 1000] x volume / 100` (the fade of a trigger is ignored; a fade-in ramp is short).
3. A step is active when any trigger level reaches the threshold.
4. Look-ahead: the active flag is read `attack` ms ahead, so the clip is already low when speech starts.
5. Target gain is `1 - 0.9 x amount / 100` while active, else 1. The gain follows the target with a one-pole filter (time constant attack going down, release going up), starting at the target of step 0 so a trigger at the very start lowers from the first moment.
- **Smoothness**: Quick = attack 60 ms, release 250 ms; Medium = 150 ms, 600 ms (default); Slow = 300 ms, 1200 ms (time constants; the clip is back within 10% of normal about 2.3 times the release constant after the sound stops, 1.4 s at the default).
- **Maximum reduction** 90% (about -20 dB) so 100% is a quiet background, never silence (FR edge case).
- **No jumps** (SC-004): a one-pole filter on a 20 ms grid with linear interpolation per sample changes the gain by well under 3 dB within any 5 ms even in the Quick case (worst step about 6 dB per 20 ms); the spec limit is 3 dB per 5 ms.

## 3. Applying it
`GainAudioProcessor` already tracks the clip position (also after a seek). It gets `@Volatile envelope: DuckEnvelope?`; the frame gain is multiplied by the envelope value interpolated at the current position. The envelope applies after volume and fade, so it never raises the level (values never exceed 1).

## 4. Preview flow
`Media3PreviewEngine.load` builds the composition (no envelope yet), then launches one background job per ducked clip: `DuckingAnalyzer.envelope(timeline, clipId)` loads the peaks (cached or decoded off the main thread), runs the planner, and calls `gains[clipId]?.setEnvelope(...)`. A later load cancels pending jobs. Every committed change (Ducking, Amount, Smoothness, any trigger edit) reloads, so ducking follows within the analysis time (planner under 50 ms with cached peaks).

## 5. Export flow
`TransformerRunner` computes the envelopes for all ducked clips with the same analyzer before building, and passes them to `CompositionBuilder`, which hands each to its gain processor. Preview and export use the same planner, so the levels agree.

## 6. Edits
Ducking lives on the clip (`AudioClip.ducking`), so split, duplicate, trim, move, and speed copy it through `copy(...)`. The envelope is recomputed from the new arrangement each load, so it always follows the sound that is now over the clip. Deleting or muting a trigger changes the plan on the next load.

## 7. Persistence
Room v12 auto migration: `duck_enabled` (default 0), `duck_amount` (default 60), `duck_smoothness` (default `Medium`) on `audio_clip`. Project duplicate and relink copy the clip, so settings follow. Templates carry none.

## 8. Editor
Audio panel gets a Ducking section for audio-lane clips: switch, Amount slider (0 to 100), Smoothness segmented control (Quick, Medium, Slow), and a note "Nothing is playing over this clip, so nothing is lowered" computed from the same trigger rule (any trigger overlapping the clip in time). The audio clip shows a small badge when on, with a content description "Ducking on, 60%". Video and photo clips do not offer the control (FR-012).
