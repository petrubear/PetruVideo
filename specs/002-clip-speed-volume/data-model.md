# Data Model: Clip Speed and Volume

Extends the 001 model. Only the additions and rules are listed.

## VideoClip (changed)

| Field | Type | Rule |
|-------|------|------|
| speedMilli | Int | 250 to 4000 (1000 = 1x); default 1000; always 1000 for photos |
| volumePercent | Int | 0 to 200; default 100; meaningful only when `attachedAudio` is true |

Derived: `sourceLengthMs = sourceEndMs - sourceStartMs`; `lengthMs = round(sourceLengthMs * 1000 / speedMilli)` (timeline length). For photos `lengthMs = sourceLengthMs` (no speed).

## AudioClip (changed)

| Field | Type | Rule |
|-------|------|------|
| speedMilli | Int | 250 to 4000; default 1000 |
| volumePercent | Int | 0 to 200; default 100; independent of `muted` |

Derived: same `sourceLengthMs` and `lengthMs`; `timelineEndMs = timelineStartMs + lengthMs`.

## Invariants (added to TimelineInvariants)

1. Every speed is within 250 to 4000 and every volume within 0 to 200; photos have speed 1000.
2. Every clip's `lengthMs` is at least 100 ms (photos 500 ms), computed from the derived rule.
3. Audio clips never overlap (already in 001; now checked with derived lengths).
4. Total duration equals the sum of video clip `lengthMs`.

## Commands (new)

| Command | Effect | Notices |
|---------|--------|---------|
| SetSpeed(clipId, speedMilli) | Sets speed on a video or audio clip; later video clips move; audio keeps its start | `SpeedLimited(applied)` when clamped by the range, the minimum length, or the next audio clip; Rejected for photos or unknown clips |
| SetVolume(clipId, percent) | Sets volume on a clip that has sound | Rejected for a video clip without attached sound, photos, or unknown clips |

## Changed commands

| Command | Change |
|---------|--------|
| TrimStart / TrimEnd | Take timeline-time positions; convert to source time with the clip's speed; limits use derived lengths |
| Split | Cut position converted to source time; both halves keep speed and volume |
| Duplicate | Copy keeps speed and volume |
| DetachAudio | New audio clip copies the video clip's speed and volume; the video's volume stays stored but unused |
| ReplaceAudio | Keeps speed and volume; the replacement's length follows the existing rule |
| MoveAudioClip | Unchanged (uses derived lengths) |

## Persistence

Room version 2: `video_clip` and `audio_clip` each gain `speedMilli INTEGER NOT NULL DEFAULT 1000` and `volumePercent INTEGER NOT NULL DEFAULT 100`. Migration 1 to 2 adds the columns; no data rewrite is needed.

## State transitions

- Speed or volume gesture: sheet drag preview (UI state only) then one command on release, one undo step.
- Detach: the video's volume becomes inactive; undo restores it together with the removal of the audio clip.
