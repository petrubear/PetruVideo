# Implementation Plan: Voice Split

## Summary

A new `VoiceSplitService` in `core/media/voicesplit`: decode the clip's trimmed source range to 44.1 kHz stereo float, run the bundled Open-Unmix UMX-HQ vocals ONNX model through ONNX Runtime Android over overlapping chunks, apply the ratio mask on the mixture's STFT phase, inverse-STFT to get Voice, subtract from the mixture to get Backing, write both as WAV into `filesDir/voicesplit/` keyed by the source fingerprint and range. A pure command `SplitVoice` replaces Detach's effect with two audio clips. The editor adds a Split voice clip tool, a progress card with Cancel, and a message on failure.

## Design

- **Model**: `core/media/src/main/assets/voicesplit/umx-hq-vocals.onnx` (17,820,856 bytes, SHA-256 `3d05709f...6ccb`, MIT). Input `float32[1,2,2049,frames]` magnitudes, output the same shape. Licence note in `licenses/`.
- **Runtime**: `com.microsoft.onnxruntime:onnxruntime-android`.
- **DSP** (`Stft.kt`, pure Kotlin, unit tested): n_fft 4096, hop 1024, periodic Hann, centred with reflect padding; inverse with overlap-add and window-square normalisation.
- **Chunking**: 400 frames per run with 40 frames of context on each side, so the LSTM sees context and the seams are inside the discarded margin.
- **Audio in/out**: decode with MediaExtractor and MediaCodec to PCM, resample to 44.1 kHz (linear), mono duplicated to stereo; output WAV 16-bit.
- **Interface**: `VoiceSplitter { suspend fun split(media, startMs, endMs, onProgress): SplitFiles? }` so the model can change; `NoVoiceSplitter` default in view model tests.
- **Store**: `VoiceSplitStore` keyed by `(fingerprint, startMs, endMs)`; swept with unreferenced files at app start and project delete, like reversed copies.
- **Command** (`core/model`): `SplitVoice(clipId, voice: MediaItem, backing: MediaItem)` adds two `AudioClip`s (origin Detached) at the clip's start, same length and speed, sets `attachedAudio` false; one undo step; refuses reversed or curved clips.
- **Editor**: `ClipTool.SplitVoice`, local state `voiceSplit` (progress, cancel), `VoiceSplitProgress` card, disabled reasons, message on failure.

## Constitution Check

No violations: pure command in the engine, offline, accessible controls.
