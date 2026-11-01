# Data Model: Keyframes

## Keyframe
| Field | Type | Rule |
|---|---|---|
| timeMs | Long | 0 to the clip's duration, relative to its start; unique within a clip; at least one frame (33 ms) from a neighbour |
| xPermille, yPermille | Int | 0 to 1000 |
| sizePermille | Int | overlay: width share 50 to 1000; text: within the text clip's size limits |
| opacityPercent | Int | 0 to 100 |
| ease | Boolean | false = Linear, true = Ease, for the stretch after this keyframe |

## Pose
`Pose(xPermille, yPermille, sizePermille, opacityPercent)`: the shown values at one moment, as Ints rounded from the blend.

## OverlayClip and TextClip (extended)
`keyframes: List<Keyframe> = emptyList()`, sorted by time, at most 50. `poseAt(relMs)` returns the plain values when the list is empty.

## Persistence
Room v11 table `keyframe(id auto, projectId, ownerId, timeMs, x, y, size, opacity, ease)`, foreign key to `project` with cascade, index `(projectId, ownerId)`. Owner is an overlay or text clip id; there is no foreign key to the clips because the two lanes use two tables.

## Invariants
Keyframes sorted and unique in time; inside the clip; values in range; no keyframes on a clip of any other lane.
