# Research: Filters and Color Adjustments

1. **Rendering**: one `RgbMatrix` per clip combining look and adjustments; vignette via a bitmap overlay. Alternatives: LUT files (assets and tuning), shader effect (more code), Media3 `Brightness`/`Contrast`/`HslAdjustment` chain (several passes, no look blending).
2. **Matrix order**: look blended by intensity first, then saturation, contrast (around mid gray), brightness (offset), warmth (red up, blue down). The order is fixed and the same in preview and export.
3. **Live changes**: `LiveRgbMatrix` reads a volatile matrix each frame; a paused preview is refreshed by a debounced seek to the current position. A filter going between neutral and active changes the effect chain and rebuilds.
4. **Looks**: Vivid (saturation 1.35, contrast 1.1), Warm (red 1.08, blue 0.92, small lift), Cool (red 0.92, blue 1.08), Black and white (luma, saturation 0 with contrast 1.05), Vintage (sepia-leaning matrix, lifted blacks, contrast 0.9), Faded (contrast 0.85, lift 0.06, saturation 0.85). Starting values tuned by eye.
5. **Tiles**: the same matrix converted to an Android `ColorMatrix` on the clip's thumbnail.
6. **Apply to all**: one command setting the filter on every video and photo clip.
7. **Persistence**: flat columns with neutral defaults and auto migration.
