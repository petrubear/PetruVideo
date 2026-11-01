# Data Model: Chroma Key

**ChromaKey** (inside OverlayClip): `enabled` (default false), `color` (ARGB int, default GREEN), `strength` 0..100 (default 40), `smoothness` 0..100 (default 20), `spill` 0..100 (default 50).

Stored as five columns of `overlay_clip` (Room schema 19). Rows from before default to off.
