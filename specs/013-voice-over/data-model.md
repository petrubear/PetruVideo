# Data Model: Voice-over Recording

No database change.

## Recording (file, not a table)
| Part | Rule |
|---|---|
| File | `filesDir/voiceovers/<recordingId>.aac`, ADTS AAC, mono, 44.1 kHz, 96 kbps |
| Length | measured recording time; recovered files use `size * 8 / 96000` seconds |
| Journal | `filesDir/voiceovers/pending.json` with projectId, startMs, recordingId while a take is open |

## Media item and clip
A `MediaItem(kind = Audio, uri = file URI, durationMs, hasAudio = true)` and an `AudioClip(origin = Added, timelineStartMs = startMs, sourceStartMs = 0, sourceEndMs = durationMs)`. Everything else about the clip is the existing audio clip.

## Recording state (editor, not saved)
`VoiceOverState`: `CountIn(secondsLeft)`, `Recording(elapsedMs, level 0..1, limitMs)`, or none.

## Limits
`maxDurationMs(startMs, audioLane, freeBytes) = min(30 min, nextClipStart - startMs, (freeBytes - 20 MB) * 8000 / 96000)`; below 0.5 s the recording is refused before it starts with the reason.
