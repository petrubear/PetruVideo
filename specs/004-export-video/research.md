# Research: Export Video

## 1. One composition for preview and export

- **Decision**: Move the composition construction out of `Media3PreviewEngine` into `CompositionBuilder` in `core:media` and parameterize it: for preview, scaled size and capped preview frame rate with live gain and reverb processors; for export, the chosen output size, frame rate, and processors with fixed settings (no live updates, no pressure bypass of reverb).
- **Rationale**: Speed, volume, mute, reverb, photos, gaps, and the fitting of clips to the frame already live there; reusing them makes FR-008 true by construction.
- **Alternatives considered**: Duplicate builder for export (drifts); render the preview player offscreen (slow, not supported for files).

## 2. Rendering

- **Decision**: Media3 `Transformer` with the `Composition`, `setVideoMimeType(H264)`, `setAudioMimeType(AAC)`, and a `DefaultEncoderFactory` with `VideoEncoderSettings` (bitrate from the table, constant bitrate mode, 2 s keyframe interval). Frame rate by a frame-drop effect on the composition's effects when below the source rate; resolution by `Presentation` to the output short side with aspect ratio kept (bars as in the preview). Progress by polling `getProgress` once per second on the main looper; cancel by `cancel()` then deleting the temporary file.
- **Rationale**: Native, hardware accelerated, handles speed and audio processors, supports cancel.
- **Alternatives considered**: `MediaCodec` plus OpenGL by hand (far more work); FFmpeg (licence, size).

## 3. Bitrate table and size estimate

- **Decision**: Recommended video bitrate at 30 fps: 720p 6 Mbit/s, 1080p 12 (as in the artboard), 2K 20, 4K 45. Frame rate scales it by `(fps / 30)^0.75`. Smaller is 0.6x, Higher is 1.5x. Audio is AAC 128 kbit/s stereo. Estimated size is `(video + audio) bitrate x duration / 8`, constant bitrate so the estimate holds within 15%. Values are tuned by measurement and kept in `BitrateTable`.
- **Rationale**: Matches the artboard (12 Mbit/s at 1080p) and common export tables; constant bitrate makes SC-003 achievable.
- **Alternatives considered**: Variable bitrate with constant quality (smaller files, unpredictable size).

## 4. Options the device and project allow

- **Decision**: `EncoderCapabilities` asks `EncoderUtil`/`MediaCodecList` for the H.264 encoder's supported sizes and frame rates (hardware preferred). Caps from the project (clarified): maximum resolution = largest source clip resolution used (photos by pixel size), maximum frame rate = highest source frame rate used (photos at the project rate). An option is available only if it is within both the cap and the device ability; a short reason is given otherwise ("above your footage", "not supported by this phone").
- **Rationale**: Honest options with reasons; no fake detail.

## 5. Background work

- **Decision**: A foreground service (`foregroundServiceType="mediaProcessing"` on API 35 and up) owned by the app's `ExportCoordinator`. It starts only from the Save to gallery tap, posts a progress notification with a Cancel action, and stops when the job ends. The coordinator is a process singleton holding the project snapshot, the job state flow, and the Transformer. Process death means no resume (clarified): `ExportRecovery` marks a leftover `Rendering` job as failed ("interrupted") on the next start and removes cache leftovers.
- **Rationale**: Keeps the render alive in the background and gives the user control from the notification.
- **Alternatives considered**: WorkManager expedited work (shorter limits, harder progress and cancel); a plain background thread (killed quickly).

## 6. Saving to the gallery

- **Decision**: Render to `cacheDir/exports/<jobId>.mp4`; on success insert into `MediaStore.Video` with `RELATIVE_PATH = Movies/PetruVideo`, `IS_PENDING = 1`, copy the bytes, set `IS_PENDING = 0`, delete the cache file. On cancel or failure delete the pending row. On API 26 to 28 write to the public Movies directory with the legacy permission and scan the file.
- **Rationale**: No broad media access on current Android versions (FR-017) and no partial file visible in the gallery (FR-016).

## 3a. Encoder limits

- **Decision**: The bitrate from the table is clamped to the encoder's supported range (`EncoderUtil.getSupportedBitrateRange`); the Bitrate row and the estimate use the clamped value. HDR sources are tone-mapped to SDR by the composition (`Composition` with SDR output) and the screen shows a notice.
- **Rationale**: Prevents encoder failures at 4K on weaker hardware and keeps the estimate honest.

## 5a. Pressure

- **Decision**: Transformer cannot pause. Under pressure the render thread priority is lowered; if the pressure monitor reaches Critical the job stops with `Failed(Pressure)` and Retry is offered.

## 7. Storage and preconditions

- **Decision**: Before starting, require free space of at least `estimatedSize x 1.2` plus 50 MB (cache copy plus the saved file are not both kept; peak is two copies, so require `2 x estimate` when the cache and gallery share a volume). Block export for missing media with a message naming the file.
- **Rationale**: Prevents the common failure of running out of space mid-render.

## 8. Persistence

- **Decision**: Room v4 adds `export_settings(projectId, resolution, frameRate, quality)` and `export_job(projectId, jobId, state, reason, settings, startedAt, resultUri)` (latest per project). Auto migration; test from v3.
- **Rationale**: Remembered choices (FR-007) and the interrupted-export notice (FR-019).

## 9. UI

- **Decision**: One `ExportScreen` following the artboard: preview with duration, three segmented controls, format, bitrate, and estimate rows, a progress card, and the Save to gallery button; after success a result row with Open and Share. Layouts: single column on compact; two columns (preview left, settings right) on expanded; preview above the hinge and settings below in tabletop. State lives in `ExportViewModel` observing the coordinator, so fold and rotation never touch the render.
- **Alternatives considered**: A bottom sheet over the editor (no room for the preview and the progress card).

## 10. Reference devices

- **Decision**: Samsung Fold SM-F976B primary; a mid-range phone for the 4x-length render check at 1080p and the 4K support check.
