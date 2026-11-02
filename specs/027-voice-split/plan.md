# Implementation Plan: Voice Split

**Branch**: `027-voice-split` | **Spec**: [spec.md](spec.md)

## Summary

Two parts. (1) **Layered audio**: audio-lane clips may overlap, up to 3 at any moment, shown on parallel rows and mixed in preview and export (needed so Voice and Backing play together, and it also allows music under a voice-over). (2) **Split voice**: a `VoiceSplitService` decodes the clip's trimmed source range to 44.1 kHz stereo, runs the bundled Open-Unmix UMX-HQ vocals ONNX model over overlapping chunks, applies the ratio mask on the mixture's STFT phase, inverse-STFTs to get Voice, subtracts it from the mixture to get Backing, and writes both as WAV files. A pure command `SplitVoice` adds the two audio clips at the clip's start.

## Technical Context

Kotlin 2.4, Media3 1.11.1, Room 2.8 (no schema change), Hilt, Compose; new dependency `com.microsoft.onnxruntime:onnxruntime-android` (arm64 and x86_64) and a 17 MB model asset. Engine pattern unchanged: immutable `Timeline`, pure `Command`s, `TimelineInvariants`.

## Part 1: Layered audio

- **Rule** (`core/model/engine/AudioPlacement.kt`): `MAX_AUDIO_LAYERS = 3`. `layersAt(lane, start, end, ignoreId)` is the largest number of clips overlapping at once inside `[start, end)`. `fits(lane, start, length, ignoreId)` is true when adding a clip there keeps every moment at 3 or fewer. `nextFreeStart` returns the earliest start at or after the desired one where `fits`, so a clip is only moved when all 3 layers are full there (before this change it moved whenever any clip overlapped).
- **Callers that change meaning**: `DetachAudio`, `MoveAudioClip`, `DuplicateAudio`, `AddAudioClips`, `AddVoiceOver` (rejects only when no layer is free), `TrimCommands.overlapsOthers` (becomes "would exceed 3 layers"), and `SpeedVolumeCommands` (the neighbour limit becomes the same check).
- **Invariant** (`TimelineInvariants`): replace "audio clips overlap" with "more than 3 audio clips at once".
- **Builder** (`CompositionBuilder.audioSequence` becomes `audioSequences`): sort by start, give each clip the first row whose last clip ended, build one audio-only sequence per row (gaps as now). Media3 mixes parallel audio sequences; the main video's own sound is already mixed with the audio lane's sequence this way, so no new graph is needed. Gains, reverb, voice-effect handles, and ducking keep working because they are keyed by clip id.
- **Ducking** (`DuckingPlanner`, `DuckingAnalyzer`): already treat other audio clips as triggers by time overlap, so they need no change; add a test with two overlapping clips.
- **Lane view** (`TimelineView`, `AudioLaneView`): rows by the same greedy assignment (shared pure function `AudioRows.of(lane)` in `core/model`, tested), lane height grows with the row count; clip select, move, and trim keep working per row.

## Part 2: Split voice

- **Model**: `core/media/src/main/assets/voicesplit/umx-hq-vocals.onnx`, 17,820,856 bytes, SHA-256 `3d05709ff7197bbd4a33aee759e4e82002acb491f41c94770227ab57507b6ccb`, MIT. Input `float32[1,2,2049,frames]` magnitudes, output the same shape. The licence and credit go in `licenses/` and the About text. The file size and checksum are verified by a test.
- **DSP** (`Stft.kt`, pure Kotlin, JVM tested): n_fft 4096, hop 1024, periodic Hann, centred with reflect padding; inverse by overlap-add with window-square normalisation. Radix-2 real FFT.
- **Chunking**: 400 frames per run with 40 frames of context on each side that are dropped, so LSTM edge effects stay out of the result.
- **Audio in and out**: MediaExtractor and MediaCodec decode to PCM, linear resample to 44.1 kHz, mono duplicated to stereo; output 16-bit WAV (`WavWriter`), 44.1 kHz stereo.
- **Interface**: `VoiceSplitter.split(media, startMs, endMs, onProgress): SplitFiles?` (suspend, cancellable), with `OnnxVoiceSplitter` and a no-op default for view model tests.
- **Store**: `VoiceSplitStore` in `filesDir/voicesplit/`, keyed by `(fingerprint, startMs, endMs)`; unreferenced files are swept at app start and when a project is deleted, like reversed copies; the two results are ordinary `MediaItem`s probed as audio.
- **Command** (`core/model/engine/VoiceSplitCommands.kt`): `SplitVoice(clipId, voice: MediaItem, backing: MediaItem)` adds two `AudioClip`s (origin Detached; names "Voice" and "Backing" come from the media display names) at the clip's start with its length and speed and the clip's volume, sets `attachedAudio = false` and `muted = false` like Detach, one undo step; rejects reversed clips and clips on a speed curve, clips without attached sound, and the case where a third layer would be exceeded.
- **Editor**: `ClipTool.SplitVoice` beside Detach with disabled reasons; local state `voiceSplit` (clip id and the clip's trim, speed, and start when it began, progress, job); a `VoiceSplitProgress` card with Cancel; the view model cancels the job when the watched clip's trim, speed, or position changes or it is deleted (a watcher on the timeline), and says why; a failure shows a message.

## Risks

- Real-voice quality is judged on a phone with a real recording; the interface lets a different model replace this one.
- ONNX Runtime on the x86_64 emulator may be slower than on the phone; the emulator test uses a short synthetic clip.
- Memory for a long clip: results are streamed to disk chunk by chunk, not held whole.

## Constitution Check

No violations: pure engine commands, offline (the model is bundled; no network permission added), accessible controls, existing panel and lane slots in all layouts. Post-design re-check: the layered audio change keeps the "one immutable timeline, pure commands" rule.
