# Data Model: Filters and Color Adjustments

**ClipFilter** (field `filter` on `VideoClip`, which also covers photos): `look` None | Vivid | Warm | Cool | BlackAndWhite | Vintage | Faded (default None); `intensity` 0..100 (default 100); `brightness`, `contrast`, `saturation`, `warmth` -100..100 (default 0); `vignette` 0..100 (default 0). `isNeutral` when the effective look is None or intensity 0, and all adjustments are 0.

**ColorMath**: `matrix(filter): FloatArray(16)` column-major 4x4 acting on [r g b 1] with offsets in elements 12 to 14; identity when neutral; `vignetteAlpha(filter)`.

**Commands** (pure, one undo step each): `SetFilterLook(clipId, look)` (keeps adjustments, sets intensity 100 when changing look), `SetFilterIntensity(clipId, intensity)`, `SetFilterAdjustments(clipId, b, c, s, w, v)`, `ResetFilterAdjustments(clipId)`, `RemoveFilter(clipId)`, `ApplyFilterToAll(fromClipId)`. Rejected for audio and text clips and for no change.

**Rules**: split copies to both halves, duplicate copies, trim, speed and move keep it.

**Persistence**: Room 7 columns on `video_clip` (names in plan), `AutoMigration(6, 7)`.
