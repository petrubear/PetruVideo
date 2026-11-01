# Data Model: Stickers and Shapes

## Sticker (catalog, not stored)
`Sticker(glyph, label, category, color: Int?)`; `color` is the default colour for shapes, null for emoji.

## Stored form
A `TextClip` with `text = glyph`, `sizePermille = 160`, `durationMs = 3000` (limited to the project end), centered. No new field or table.
