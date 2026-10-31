# Contract: Editing Engine (`core:model`)

Pure Kotlin, JVM-only. Every operation is a command applied to an immutable `Timeline` and returns a new `Timeline` or a typed failure. The editor view model holds the current timeline and the history; the engine never touches Android, storage, or players.

## Command result

`Applied(newTimeline, label)` or `Rejected(reason)`. Rejections never change state. Reasons: `OutOfRange`, `BelowMinimumLength`, `Overlap`, `NoAttachedAudio`, `NotFound`, `MissingMedia`.

## Commands

| Command | Inputs | Preconditions | Effect |
|---------|--------|---------------|--------|
| `AddVideoClips` | list of (mediaItem, optional trim), insertAtMs | Media available | Inserts into the video lane at the playhead boundary or end; sets project format if unset; sets `attachedAudio` from `hasAudio` |
| `AddAudioClips` | list of audio mediaItems, startMs | Media is audio | Places clips on the audio lane in order from `startMs`; rejects overlaps |
| `TrimStart` / `TrimEnd` | clipId, newSourceMs | Result length >= 100 ms and within source bounds | Updates the trim; for video clips later clips shift automatically |
| `Split` | clipId, atMs | `atMs` strictly inside the clip with both halves >= 100 ms | Replaces the clip with two contiguous clips; attached audio preserved |
| `Duplicate` | clipId | Clip exists | Inserts a copy right after (video) or at the next free position (audio) |
| `Delete` | clipId | Clip exists | Removes the clip; a video clip's attached audio goes with it; detached audio stays |
| `MoveVideoClip` | clipId, newIndex | Valid index | Reorders; attached audio follows; detached audio does not move |
| `MoveAudioClip` | clipId, newStartMs | No overlap | Repositions on the audio lane |
| `DetachAudio` | videoClipId | `attachedAudio` is true | Creates a `Detached` audio clip with the same source range and start; clears `attachedAudio` |
| `ReplaceAudio` | audioClipId, newMediaItem | Media is audio | Keeps `timelineStartMs` and length; trims the new source to fit; if shorter, the clip shortens and the result carries a `LengthChanged` notice |
| `SetMuted` | audioClipId, muted | Clip exists | Toggles mute |
| `SetPhotoDuration` | videoClipId, durationMs | Clip is a photo; 500 to 60000 | Updates duration |
| `RelinkMedia` | mediaItemId, newMediaItem | Compatible kind | Rebinds clips that use the item, clamping trims to the new source length |

## History

- `apply(command)` pushes the previous timeline onto `undoStack` and clears `redoStack`.
- `undo()` / `redo()` swap snapshots; both are no-ops (and report unavailable) on an empty stack.
- Capacity 100; the oldest snapshot is dropped first.
- `canUndo` and `canRedo` drive the dimmed state of the undo and redo buttons.

## Invariants (property-tested)

1. The video lane is gapless and ordered; positions equal the running sum of clip lengths.
2. Every clip length >= 100 ms and trims stay within source bounds.
3. Audio lane clips never overlap.
4. `undo(apply(c, t)) == t` and `redo` restores the applied result for every command.
5. `Split` followed by joining the halves yields the original source range.
6. Total duration equals the sum of the video lane, extended by any audio clip ending later.

## Format derivation

`deriveFormat(firstClipMedia)` returns `ProjectFormat`: aspect ratio from display size, scale so the short side is at most 1080 (never upscaled), frame rate rounded to the nearest of 24/30/60, default 30.
