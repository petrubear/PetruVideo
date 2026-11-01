# Data Model: Speed Ramps

## SpeedPoint and SpeedCurve
| Field | Type | Rule |
|---|---|---|
| positionMs | Long | 0 for the first point, the clip's trimmed source length for the last, strictly increasing, at least 100 ms apart |
| speedMilli | Int | 100 to 10 000 (0.1x to 10x) |

2 to 12 points. Derived and cached: `steps`, `timelineMs`.

## VideoClip (extended)
`speedCurve: SpeedCurve? = null`. `lengthMs` follows the curve. Invariant: `speedCurve.sourceLengthMs == sourceLengthMs`.

## Persistence
Room v15: `video_clip.speedCurve TEXT NULL`.

## Invariants
Photos and freeze stills have no curve; the timeline length of a clip with a curve is between the minimum clip length and 10 minutes.
