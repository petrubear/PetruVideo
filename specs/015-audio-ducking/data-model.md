# Data Model: Audio Ducking

## Ducking
| Field | Type | Rule |
|---|---|---|
| enabled | Boolean | default false |
| amountPercent | Int | 0 to 100, default 60; 0 changes nothing, 100 lowers by 90% |
| smoothness | DuckSmoothness | Quick, Medium (default), Slow |

## AudioClip (extended)
`ducking: Ducking = Ducking()`. Only audio-lane clips carry it.

## DuckEnvelope
`stepMs = 20`, `gains: FloatArray` with one value in 0.1..1.0 per step covering the clip's timeline length; `gainAt(relMs)` interpolates linearly. Not stored; recomputed on every load and export.

## Persistence
Room v12: `audio_clip.duck_enabled INTEGER NOT NULL DEFAULT 0`, `duck_amount INTEGER NOT NULL DEFAULT 60`, `duck_smoothness TEXT NOT NULL DEFAULT 'Medium'`. Auto migration 11 to 12.

## Invariants
Amount in range; a clip with ducking on is never its own trigger; a ducked clip is never a trigger for another ducked clip.
