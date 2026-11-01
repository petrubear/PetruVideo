# Data Model: Text Layer

## TextClip (in `core:model`, on `Timeline.textLane`)
- `id`, `text` (up to 500 characters; may be empty), `startMs` (>= 0), `durationMs` (200 .. 600 000).
- `xPermille`, `yPermille`: center of the clip as 0..1000 of frame width and height (default 500, 500). The renderer clamps the visible box inside the safe margin.
- `sizePermille`: text size as a share of frame height, 20..300 (default 60 = 6%).
- `style: TextStyle`, `animIn: TextAnimation`, `animOut: TextAnimation`.
- `endMs = startMs + durationMs`.

## TextStyle
- `font`: Sans | Serif | Mono | Display | Handwritten | Bold (default Sans).
- `colorArgb` (default white), `align`: Left | Center | Right (default Center).
- `background`: enabled (default false), `colorArgb` (default black), `opacityPercent` 0..100 (default 60).
- `outline`: enabled (default false), `colorArgb` (default black), `widthPermille` 0..40 of the text size (default 12).

## TextAnimation
- `kind`: None | Fade | SlideUp | SlideDown | Pop (In allows None, Fade, SlideUp, Pop; Out allows None, Fade, SlideDown, Pop), `lengthMs` 200..1500 (default 500).
- Effective lengths are limited so that in + out <= duration; stored values are kept.

## Timeline
- `textLane: List<TextClip>` sorted by `startMs`, may overlap. `totalDurationMs = max(video, audio end, text end)`.
- Rows are derived (`TextRows.assign`), not stored.

## Commands (pure, one undo step per gesture)
`AddText(atMs, newId)`, `SetTextContent(id, text)`, `SetTextStyle(id, style)`, `SetTextPlacement(id, x, y, size)`, `SetTextAnimation(id, in, out)`, `MoveText(id, startMs)`, `TrimText(id, startMs, endMs)`; `Split`, `Duplicate`, `Delete` accept text clip ids (split: text and style on both halves, first half out animation None, second half in animation None).

## Persistence
Room schema 6: table `text_clip(id PK, projectId FK, startMs, durationMs, text, x, y, size, font, color, align, bgEnabled, bgColor, bgOpacity, olEnabled, olColor, olWidth, inKind, inMs, outKind, outMs)`, index `(projectId, startMs)`; `AutoMigration(5, 6)`; `schemas/6.json`.

## Rendering contract
`TextRenderer.render(clip, frameWidth, frameHeight): Bitmap` returns the clip's box (text plus padding) with transparent surround; `TextAnimator.at(clip, timeMs)` returns alpha, offsetY (share of frame height), scale; overlay center = clip center + offset.
