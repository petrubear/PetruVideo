# Data Model: Keyframes for Video Clips

## VideoClip (extended)
`keyframes: List<Keyframe> = emptyList()`, sorted by time, at most 50, times 0..clip length (timeline length after speed), at least 33 ms apart. `poseAt(relMs)` returns the neutral pose `Pose(500, 500, 1000, 100)` when empty.

## Pose meaning for video clips
| Field | Meaning | Range |
|---|---|---|
| xPermille, yPermille | point of the clip at the frame center | clamped so the visible window stays inside the clip |
| sizePermille | zoom, 1000 = fills the frame | 1000 to 4000 |
| opacityPercent | picture brightness toward black | 0 to 100 |

## Persistence
Existing `keyframe` table (ownerId = video clip id). No migration. Load/save read and write the video lane too.

## Invariants
Keyframes sorted, unique, inside the clip, values in range and cover-safe; no keyframes on audio clips.
