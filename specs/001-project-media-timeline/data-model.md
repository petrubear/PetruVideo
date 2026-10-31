# Data Model: Project Creation, Media Import, and Basic Timeline with Trim

All times are integer milliseconds. The engine types live in `core:model` with no Android dependencies. Persistence maps them to Room tables (see `contracts/project-storage.md`).

## Project

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Stable identity |
| name | text | 1 to 80 chars; default "Untitled N" |
| createdAt, modifiedAt | timestamp | `modifiedAt` updated on every saved edit |
| status | enum `Draft`, `Exported` | `Exported` is set by the later export feature |
| format | `ProjectFormat?` | Null until the first clip is added |
| thumbnailRef | text? | Cache key of the first frame of the first clip on the timeline |
| schemaVersion | int | For migrations |

**ProjectFormat**: `width`, `height` (px), `fps` (24, 30, or 60). Set once from the first clip (research 4); never recalculated, even if that clip is later removed.

**Relationships**: one Project has many MediaItems and exactly one Timeline.

## MediaItem

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| projectId | UUID | Owner |
| kind | enum `Video`, `Photo`, `Audio` | |
| uri | text | Content URI of the original; never copied |
| displayName | text | |
| durationMs | long? | Null for photos |
| width, height | int? | Display size after rotation; null for audio |
| frameRate | float? | Video only |
| hasAudio | boolean | Video only; false produces no waveform |
| fingerprint | `{sizeBytes, lastModified, durationMs, mimeType}` | For missing/relink detection |
| availability | enum `Available`, `Missing` | Recomputed when a project opens |

**Rules**: importing the same file twice creates two MediaItems (same uri, different ids). MediaItems are created only when an item is added to the timeline (there is no separate library); a MediaItem that no clip references any more is removed on the next save. The editor's Media panel and the project detail list the MediaItems currently referenced by clips.

## Timeline

| Field | Type | Notes |
|-------|------|-------|
| projectId | UUID | |
| videoLane | list of VideoClip | Gapless and sequential; start positions are derived from order |
| audioLane | list of AudioClip | Positioned; gaps allowed; ordered by `timelineStartMs` |
| playheadMs | long | Persisted so reopening restores position |

**Derived**: total duration is the greater of the video lane length and the end of the last audio clip; the transport row's total time shows this value.

## VideoClip

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| mediaItemId | UUID | Video or Photo |
| sourceStartMs, sourceEndMs | long | The trim; for photos, start is 0 and end is the chosen duration |
| attachedAudio | boolean | True while the video's sound is part of this clip; false after Detach or if the source has no sound |

**Validation**: `0 <= sourceStartMs`, `sourceEndMs <= media duration` (videos), and `sourceEndMs - sourceStartMs >= 100`. Photo duration between 500 and 60000, changed by dragging the trim handles (which can lengthen as well as shorten photo clips).

## AudioClip

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| mediaItemId | UUID | Video (detached sound) or Audio file |
| sourceStartMs, sourceEndMs | long | Its own trim |
| timelineStartMs | long | Position on the audio lane |
| muted | boolean | |
| origin | enum `Detached`, `Added` | Informational; drives the "Detached" label |

**Validation**: same minimum length as video clips; audio clips never overlap each other: an add or move that would overlap snaps to the next free position and returns a `Snapped` notice. An audio clip may overlap a video clip's attached sound; both play together.

## Selection (editor state, not persisted)

`kind` (`VideoClip`, `AudioClip`, none), `clipId`.

## ImportSelection (Add media state, not persisted)

Ordered list of `(MediaItemSource, orderIndex)`, current tab (Videos, Photos, Audio), current album, and totals (count, total duration).

## EditHistory (in memory)

`undoStack`, `redoStack` of `Timeline` snapshots with command labels; capacity 100. New edits clear the redo stack.

## ArrangementInput and Arrangement (UI)

- `ArrangementInput`: window width class, window height class, fold state (flat, half-opened, none), fold orientation, hinge bounds and whether it separates.
- `Arrangement`: `Compact`, `Expanded`, `Tabletop`, plus the pane and hinge-avoidance rules. A pure function of `ArrangementInput`.

## State transitions

- **Project**: `Draft` -> `Exported` (set by the export feature); back to `Draft` when edited after export.
- **MediaItem availability**: `Available` <-> `Missing` on project open and after Relink.
- **VideoClip.attachedAudio**: `true` -> `false` on Detach (undoable); never set back to `true` except by undo.
- **Format**: `null` -> set on first clip; terminal.

## Derived rules summary

- Detach(videoClip): requires `attachedAudio` and a source with sound; creates `AudioClip(origin = Detached, mediaItemId = same, sourceStart/End = same, timelineStartMs = clip's current start)`, sets `attachedAudio = false`.
- Split at playhead: clip becomes two clips with contiguous source ranges; `attachedAudio` preserved on both.
- Duplicate: copies the clip including `attachedAudio`; a detached audio clip is not copied.
- Delete video clip: removes its attached audio with it; a detached audio clip remains.
