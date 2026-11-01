# Data Model: Auto Captions

## TextClip (extended)
`caption: Boolean = false`. True for clips made by Auto captions; kept when the user edits them; false for typed text.

## CaptionPhrase and CaptionLine (not stored)
`CaptionPhrase(clipId, sourceStartMs, sourceEndMs, startMs, endMs)`: where a spoken phrase is in a clip's source and on the timeline. `CaptionLine(startMs, endMs, text)`: what the command adds.

## Caption style
x 500, y 880 permille of the frame, size 50, white text, background box on (black, 60%), centered.

## Persistence
Room v16: `text_clip.caption INTEGER NOT NULL DEFAULT 0`.

## Invariants
At most 200 caption clips are added per run; captions have the length of their phrase (at least `TEXT_MIN_MS`).
