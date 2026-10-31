# Data Model: Export Video

## ExportSettings (per project)

| Field | Type | Rule |
|-------|------|------|
| resolution | Resolution (R720, R1080, R2K, R4K) | short side 720, 1080, 1440, 2160 |
| frameRate | FrameRate (F24, F30, F50, F60) | |
| quality | Quality (Smaller, Recommended, Higher) | |

Defaults: the largest resolution not above 1080p and not above the project's source cap; the frame rate nearest to the project's that is within the source cap; Recommended. A saved choice outside the current caps (the footage changed) is lowered to the nearest allowed value.

## ExportCaps (derived, not stored)

| Field | Rule |
|-------|------|
| maxResolution | largest short side among video clips in use, photos by pixel size; at least 720 |
| maxFrameRate | highest frame rate among video clips in use, photos at the project rate; at least 24 |
| device | encoder abilities from `EncoderCapabilities` |

An option is allowed when within both the caps and the device abilities; otherwise it carries a reason (`AboveFootage`, `NotSupported`).

## Bitrate and size (derived)

`videoBitrate(resolution, frameRate, quality)` from the table in research 3; `audioBitrate = 128 kbit/s`; `estimatedBytes = (video + audio) x durationSeconds / 8`.

## ExportJob (in memory, latest per project persisted)

| Field | Type | Rule |
|-------|------|------|
| id | String | unique per run |
| projectId | String | |
| settings | ExportSettings | copied at start |
| snapshot | Timeline | copied at start; not persisted (no resume) |
| state | ExportState | see below |
| startedAt | Long | |

### ExportState

`Waiting` -> `Rendering(progress 0..100)` -> `Finished(uri, bytes, durationMs)`; `Rendering` -> `Cancelled`; `Waiting` or `Rendering` -> `Failed(reason)`.

Failure reasons: `MissingMedia(name)`, `NotEnoughStorage(needed)`, `EncoderError`, `Interrupted`, `Pressure`, `SaveFailed`.

## Invariants

1. At most one job is `Waiting` or `Rendering`.
2. A `Finished` job has a MediaStore row published (not pending); any other terminal state has none and no cache file.
3. The settings of a job never change after start.
4. Estimated size uses the same table as the encoder bitrate.

## Persistence

Room version 4 (auto migration from 3): tables `export_settings` and `export_job` (see research 8). The `Interrupted` rule: on app start a persisted job in `Waiting` or `Rendering` becomes `Failed(Interrupted)`.
