# Research: Effects, Audio Tab, and Templates

## 1. How transitions are rendered
- **Decision**: overlap A's tail with B's head. Clips alternate between two video sequences (A, B) with gaps, and a `TransitionLayout` compositor blends them per frame from the time inside the transition window; outside windows the active input is drawn alone.
- **Why**: Media3 sequences are strictly back to back, and the 010 spike showed the compositor handles several sequence inputs with per-input alpha and transform. A transition is then alpha, offset, and scale per input over a window.
- **Consequence**: the later clip starts T earlier, so the project shortens by T per transition. `videoLengthMs` subtracts the overlaps; clip start times are derived, not stored.
- **Rejected**: fading to black inside a single sequence (cannot show two clips at once); using source handles beyond the trim (not always present, photos have none).
- **Risk, spike first (T0)**: same ordering and gap-frame lessons as 010 (hide 16x16 gap frames by alpha, ended input holds its last frame so add tail gaps).

## 2. Transition kinds
Dissolve (alpha), Fade through black (A to black in the first half, B in from black in the second), Slide (B pushes A left), Cover (B slides in over A, which stays; the compositor has no mask, so a true wipe is not offered), Zoom (A scales up while fading to B). Strength control is the length (0.2 to 2.0 s, step 0.1), limited to half of the shorter neighbour.
- **Audio**: during a transition A's sound fades out and B's fades in over the same window using the existing gain processor curve. No new audio code.

## 3. Clip effects
Blur (box blur, radius from strength), Pixelate (block size from strength), Mirror (horizontal mirror strength 0 to 100 maps to mirrored fraction of frame), Shake (time-based offset, amplitude from strength). Implemented as `GlShaderProgram` effects with a live strength value, like `LiveRgbMatrix`. Order per clip: filter, effect, then presentation.
- **Reduced quality**: preview skips Blur and Shake only if the frame budget is exceeded (PreviewQuality flag `clipEffects`), export never skips.

## 4. Music
`MediaKind.Audio` and `AudioClip` already exist (the audio lane holds detached video audio today). Add `AudioOrigin.Music`. Picker: `OpenDocument` with `audio/*`. Duration and readability come from `MediaMetadataRetriever`; unreadable or zero-length files are rejected with a message. Mixed in preview and export by the existing audio sequence.

## 5. Templates
Six code-defined templates, no assets to license: for example Quick Cuts (6 slots, 1.5 s, Vivid), Memories (4 slots, 3 s, Faded plus dissolves), Travel (5 slots, 2.5 s, Warm, slides), Black and White Story (4 slots, 3 s, B&W, fade through black), Pop (6 slots, 1 s, Vivid, zoom), Slow Fade (3 slots, 4 s, Vintage, dissolves). Each has a title text style. Previews are drawn in Compose from the definition (colored blocks with slot lengths), so no video files ship.
- **Fill rules** (FR-014): items in order, videos trimmed to `min(slot, video length)`, photos use the slot length, extra items dropped with a message, unused slots dropped, transitions kept only between filled neighbours.

## 6. Persistence
Room 9 to 10, auto migration: `trn_kind`, `trn_length_ms` and `fxe_kind`, `fxe_strength` nullable columns on `video_clip`. Music reuses `audio_clip`; the `origin` column gains a value.
