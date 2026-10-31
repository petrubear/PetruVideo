# Contract: Project Storage (`core:data`)

Room database `petruvideo.db`. Original media is never copied, moved, or modified. Caches are disposable.

## Tables

| Table | Key columns | Notes |
|-------|-------------|-------|
| `project` | `id` PK, `name`, `createdAt`, `modifiedAt`, `status`, `formatWidth`, `formatHeight`, `formatFps` (nullable together), `thumbnailRef`, `playheadMs` | One row per project |
| `media_item` | `id` PK, `projectId` FK (cascade), `kind`, `uri`, `displayName`, `durationMs`, `width`, `height`, `frameRate`, `hasAudio`, `fpSize`, `fpLastModified`, `fpMimeType` | Fingerprint columns prefixed `fp` |
| `video_clip` | `id` PK, `projectId` FK (cascade), `mediaItemId` FK, `orderIndex`, `sourceStartMs`, `sourceEndMs`, `attachedAudio` | Unique (`projectId`, `orderIndex`) |
| `audio_clip` | `id` PK, `projectId` FK (cascade), `mediaItemId` FK, `timelineStartMs`, `sourceStartMs`, `sourceEndMs`, `muted`, `origin` | Index on (`projectId`, `timelineStartMs`) |

Availability is not stored; it is recomputed when a project opens. `media_item` rows are written only when items are added to the timeline, and rows no clip references are deleted on the next save.

## Operations

- `observeProjects(filter, query)`: ordered by `modifiedAt` desc; returns summaries including computed duration and counts.
- `loadProject(id)`: returns project, media items, and timeline.
- `saveTimeline(projectId, timeline, playheadMs)`: replaces clip rows in one transaction and updates `modifiedAt`.
- `duplicateProject(id)`: deep copy with new ids; media items reference the same URIs.
- `deleteProject(id)`: removes rows and cache entries; never touches original media.
- `rename`, `setStatus`.

## Autosave

The editor view model submits the latest timeline to a single-writer channel. The writer waits 1.5 seconds after the last submission, then saves; it also flushes immediately on lifecycle stop. Save status ("saved" or "saving") is exposed for the editor top bar.

## Schema versioning

Database `version` starts at 1. Every change ships with a tested migration; destructive migration is not allowed. `project.schemaVersion` is not used for the database but reserved for exported project bundles in later features.

## Cache files

`cache/thumbs/<fingerprint>/<timeMs>.webp` and `cache/waveforms/<fingerprint>.peaks`. Keyed by media fingerprint; safe to delete at any time; regenerated on demand; trimmed under storage pressure.

## Failure handling

- Low storage on save: surface a warning, keep the last good rows (transactions roll back), retry after the user frees space.
- Process death mid-edit: next open loads the last committed state (loss limited to the debounce window).
