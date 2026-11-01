# Research: Reverse and Freeze Frame

## 1. Freeze: the still and the command
- **Decision**: the editor extracts the frame at source time `sourceStart + offset x speed / 1000` with `MediaMetadataRetriever.getFrameAtTime(OPTION_CLOSEST)` at full size, saves it as JPEG (quality 95) under `freezes/`, probes it as a photo `MediaItem`, and sends `FreezeFrame(clipId, atMs, lengthMs, stillItem, newIds)`.
- **Command**: cuts the clip at `atMs` like `Split` (first part loses its transition out, the last part keeps it) and inserts a `VideoClip` of the still (`sourceStart 0`, `sourceEnd = length`, no attached sound, speed 1) between the parts. At the very start or end it inserts before or after without an empty part. The still copies the clip's transform, filter, and effect; with keyframes it gets one keyframe at 0 holding `poseAt(offset)`. Lengths 500..10 000 ms. One undo step.
- **Why**: reuses photo clips, so preview, export, trimming, transitions, keyframes, and persistence already work.
- **Rejected**: a freeze flag that repeats a frame in the player (needs a new render path); extracting the frame with the GL pipeline (the retriever is exact enough: the closest frame to the requested time).

## 2. Reverse: lossless remux of an all-intra copy
- **Decision**: (a) Transformer exports the clip's trimmed range to a temporary MP4 with H.264 at a high constant bitrate and `iFrameInterval = 0` so every frame is a sync frame and no frame depends on another; (b) `MediaExtractor` reads that file's video samples, the list of samples is reversed, and `MediaMuxer` writes them with new ascending presentation times (original frame times mirrored: `t' = total - t`); (c) the audio of the trimmed range is decoded to PCM, reversed frame by frame, and encoded to AAC, then muxed into the same file.
- **Why**: Media3 has no reverse; decoding and re-encoding every frame on our own needs a GL pipeline. With every frame a sync frame the compressed samples can be copied backwards without decoding. The cost is a larger temporary file, which the 30 second limit and a free-space check bound.
- **To confirm in the first reverse task**: the encoder honours `iFrameInterval = 0` as all sync frames on the emulator and typical devices; if it does not, fall back to setting a very short interval and checking each sample's sync flag, refusing with a message when frames are not independent.
- **Rejected**: GL frame buffer reversal (large memory per group of pictures); playing a clip backwards with negative speed (not supported by the player).

## 3. Mapping ranges
A reversed file covers the source range `[rs, re]` that was prepared. A clip that is reversed and trimmed to `[s, e]` inside it reads the file from `re - e` to `re - s`. `ReverseStore.fileFor(media, s, e)` returns any stored file whose range covers `[s, e]`, else null (the preparer then renders one for exactly `[s, e]`). Keys use the media fingerprint, so a relinked file gets a new copy.

## 4. Preparation flow
- Tapping Reverse starts `ReverseRenderer` in the background with progress (0..100) and Cancel; the clip changes only when it finishes (`SetReversed(clipId, true)`), so a failure or cancel leaves the project untouched.
- On open or load, a reversed clip whose file is missing is prepared again in the background and plays forwards until it is ready (a note says it is being prepared).
- Export waits for every needed file (rendering missing ones first) before building.
- Cleanup: after a project is deleted or a clip is unreversed or deleted, files not used by any reversed clip of any project are removed (same sweep idea as voice-overs).

## 5. Edit rules for a reversed clip
- Split at `t`: both parts stay reversed; because the picture runs backwards, the later source part plays first. The command mirrors the source cut so each part keeps the matching source range.
- Trim start/end on the timeline trims the backwards playback, so the source end/start moves instead (mirrored).
- Speed, volume, fade, audio tools, keyframes, transform, filter, effect, and transitions work on the backwards clip as on any clip (they act on the timeline order).
- Duplicate and project duplicate copy the flag; relink resets nothing but triggers re-preparation.

## 6. Persistence
Room 13 to 14 auto migration: `video_clip.reversed INTEGER NOT NULL DEFAULT 0`. Stills are photo media items already stored by the project.

## 7. Editor
A `Freeze` clip tool opens a panel with a length slider (0.5 to 10 s, default 2 s) and an Insert button. A `Reverse` clip tool toggles reverse (button shows On/Off); starting it shows a progress bar with Cancel above the tool bar. Reverse is shown unavailable for photos ("Photos cannot be reversed"), missing media, and clips over 30 s ("Too long to reverse, trim or split it first").
