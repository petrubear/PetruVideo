# Research: Overlay Audio

## 1. Where the overlay's sound is built

- **Decision**: add one audio-only sequence per sounding overlay, placed after the main sequences (with the audio-lane rows). The overlay's video-only sequence is unchanged.
- **Rationale**: video input order matters (`OverlayLayout` and `TransitionPlan.of(timeline, overlays.size)` index inputs). Audio sequences already come last, so nothing shifts. An audio-only sequence adds no video decoder, so it should not worsen the known preview freeze. The preview can drop these sequences to fall back without touching the picture.
- **Alternatives**: (a) ask the overlay's own sequence for video and audio tracks: couples sound to the picture inputs, and a gap in a sequence that waits for audio can fail an export (the comment in `videoTrackTypes` records this); (b) reuse audio-lane rows: would take the 3-layer limit and show in the lane.

## 2. Sound effects chain

- **Decision**: reuse `soundEffects(...)` (voice fx, reverb, gain with fades) with the overlay id as the key; the maps `gains`, `reverbs`, `voiceFxs` then drive live preview changes the same way as for lane clips.
- **Rationale**: identical behavior to lane clips (FR-004) with no new processor.
- **Alternatives**: a separate overlay chain: duplicate code.

## 3. Commands

- **Decision**: widen the existing sound commands to find the clip in `overlayLane` after `videoLane` and `audioLane`; add overlay branches to `DetachAudio` and `SplitVoice`.
- **Rationale**: the editor already sends these commands from the clip tools; one id space (UUIDs) means no extra routing. Undo comes for free.
- **Alternatives**: separate `SetOverlayVolume` commands: more types, same logic.

## 4. Database

- **Decision**: add columns to `overlay_clip` with defaults that mean silent (`attachedAudio = 0`, volume 100, mute 0, reverb and fx off), migration 22 to 23.
- **Rationale**: FR-008 and SC-005: old projects export the same. New overlays are created with `attachedAudio = true` by `AddOverlay` when the source has sound.
- **Alternatives**: a JSON column: harder to query and migrate, and the project stores lane clips as columns.

## 5. Detach and Split for an overlay

- **Decision**: the audio clip is created at the overlay's start with its source range; the overlay gets `attachedAudio = false`. Rejected with `LimitReached` when no free layer fits (consistent with `SplitVoice`). Detach uses the snapping rule of `DetachAudio` for lane clips.
- **Rationale**: matches base-lane behavior the user asked for.

## 6. Preview fallback

- **Decision**: build the preview composition with overlay audio; if the preview engine reports playback stalled in multi-input mode (existing stall signal), rebuild without the overlay audio sequences, keep the picture, and set a notice "Overlay sound plays in the export". A phone check decides whether the stall happens at all with audio-only inputs.
- **Rationale**: matches the clarified rule; keeps the picture alive.
- **Alternatives**: never play in preview (rejected in clarify).

## 7. Mix level

- **Decision**: rely on Media3's audio mixer for summing; keep per-source volume at most `VOLUME_MAX_PERCENT`. Check the export for clipping with three loud sources and add a limiter step only if the check shows distortion.
- **Rationale**: avoids building a limiter before it is proven needed (FR-011).
