# Research: Text Layer

## 1. How to draw text in preview and export

**Decision**: One Android `TextRenderer` makes a bitmap per text clip (StaticLayout with the clip's font, size, color, alignment, an optional rounded background box, and an outline drawn as a stroke pass), sized for the output frame. Each bitmap is shown by a Media3 `BitmapOverlay` inside one composition-level `OverlayEffect`. Preview and export use the same builder, so they draw the same pixels.
**Rationale**: Matches the project's rule of one shared builder (004); no second text engine to keep in sync; works for any script because Android lays the text out.
**Alternatives**: Draw text in Compose over the preview and use a different renderer for export (two renderers, drift, SC-006 hard to hold); OpenGL text rendering (large work); burning with FFmpeg drawtext (new dependency).

## 2. Time window and animation

**Decision**: Each overlay returns overlay settings per presentation time: alpha 0 outside the clip's window, and inside the window alpha, vertical offset and scale from a pure `TextAnimator(clip, timeMs)`. The bitmap itself does not change over time. Spike S1 checks that composition-level effects receive composition time in preview and export.
**Rationale**: Cheap per frame, deterministic, JVM-testable.
**Alternatives**: One overlay per time segment (many overlays); rebuild the composition at each boundary (stutter).

**Result of spike S1 (emulator, API 34)**: composition-level `OverlayEffect` receives composition time in export: the title shows only inside its window and at its place (left half for x=250). `BitmapOverlay` re-uploads only when the bitmap instance changes. A project with only text exports (a gap-only video sequence works); text past the end of the video extends the project over black via a trailing gap in the video sequence. For a gap-only sequence the output size is fixed by a `Presentation` effect placed before the overlays. Preview plays projects with animated text, text beyond the video, text only, and live changes while paused and playing. Bug found and fixed: a centered layout wider than the cropped bitmap showed only one letter; the layout is now made again at the widest line's width.

## 3. Position, size, and frame size

**Decision**: Position (center) is stored in permille of the frame width and height, size as permille of the frame height. The renderer wraps to the frame width minus a 5% margin on each side and clamps the box inside the margin. The overlay anchor is the clip center in normalized device coordinates (x*2-1, 1-y*2).
**Rationale**: Same relative layout at 720p, 1080p, 4K (SC-006).
**Alternatives**: Absolute pixels (breaks across export sizes).

## 4. Live editing and a paused preview

**Decision**: Overlays read a volatile snapshot of their clip. A live update replaces the snapshot, marks the bitmap stale, and renders the new bitmap on a background thread; the next frame uses it. Since a paused player draws no new frames, the engine seeks to the current position (debounced) to redraw.
**Rationale**: Meets the 100 ms goal without rebuilding the composition.
**Alternatives**: Rebuild the composition on every keystroke (re-prepare, flicker).

## 5. Interaction on the preview

**Decision**: A transparent Compose layer above the player shows the selection box of the selected text clip and handles drag (move), pinch (size), and tap to select, with center guides at 8 dp. Gesture changes stream as live updates and commit once on release (one undo step).
**Rationale**: Same pattern as the Speed and Reverb sliders; keeps the player surface simple.
**Alternatives**: Gestures inside the GL surface (not available).

## 6. Lane rows

**Decision**: Rows are assigned automatically by a pure function: clips sorted by start, each placed in the first row whose last clip ends at or before its start. Rows are not stored.
**Rationale**: Clarification answer; deterministic for tests.
**Alternatives**: Stored rows (extra state and migration).

## 7. Fonts

**Decision**: Six choices mapped to system families: Sans (sans-serif), Serif (serif), Mono (monospace), Display (sans-serif-black), Handwritten (cursive), Bold (sans-serif bold). Missing families fall back to the default.
**Rationale**: Clarification answer (system fonts only).
**Alternatives**: Bundled fonts (rejected by the user).

## 8. Persistence

**Decision**: A `text_clip` table with flat columns and defaults; the session saves the text lane with the video and audio lanes in the same transaction.
**Rationale**: Same pattern as 002 to 005; auto migration works.
**Alternatives**: JSON column (harder to migrate and test).

## 9. Project length

**Decision**: `Timeline.totalDurationMs` becomes the maximum of video length, audio end, and text end. Export and the preview already use it.
**Rationale**: Spec FR-010; text past the video plays over black.
**Alternatives**: Clamp text to the video length (rejected by the spec).
