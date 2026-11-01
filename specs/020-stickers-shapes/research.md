# Research: Stickers and Shapes

- **Decision**: stickers and shapes are text layers with one symbol. Emoji draw in colour from the phone's emoji font; shape glyphs (circle U+25CF, square U+25A0, triangle U+25B2, star U+2605, heart U+2665, arrow U+27A4, check U+2714, cross U+2716) draw in the layer's colour. **Why**: it reuses the whole layer system with no new rendering, data, or export path. **Rejected**: vector shapes (a new layer type, renderer, table, and export path for a modest gain); bundled sticker bitmaps (size and licensing).
- **Default look**: size 160 (permille of the frame height, as for titles: about a fifth of the frame with the text renderer's scaling), 3 s, x and y 500, shapes coloured vivid (red, yellow, green, blue by category order) with no box; emoji colour ignored.
- **Glyph check**: `Paint.hasGlyph(symbol)` at gallery open; symbols that fail are hidden (older phones lack newer emoji). The result is cached for the process.
- **Selection after adding**: the new layer is selected with the usual clip tools (Edit, Style, Animation, Keyframes, Split, Duplicate, Delete); the text editor is not opened.
