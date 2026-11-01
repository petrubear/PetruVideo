# Implementation Plan: Text Layer - Titles and Captions

**Branch**: `006-text-layer` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-text-layer/spec.md`

## Summary

Add a Text lane of text clips to the timeline, a Text tab with add, edit, style, animation and placement tools, and rendering in preview and export. Technical approach: extend the pure-Kotlin engine with a `TextClip` list on `Timeline` and pure commands (add, content, style, placement, timing, animation, plus split/duplicate/delete on the new lane); add a `text_clip` table with a Room v6 auto migration; render each visible text clip as a Media3 bitmap overlay (`OverlayEffect` on the composition) from one shared Android `TextRenderer` (StaticLayout on a Canvas) so preview and export draw identical pixels at any frame size; animations are overlay-settings changes over time (alpha, offset, scale) with no new bitmap; live edits update mutable overlay state and refresh the current frame; a Compose gesture layer above the preview handles drag, pinch and the selection box.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17 toolchain (unchanged)

**Primary Dependencies**: Media3 1.11 (CompositionPlayer, Transformer, `OverlayEffect`, `BitmapOverlay`, `OverlaySettings`), Room 2.8, Compose BOM 2026.09, Hilt, Paparazzi. No new libraries.

**Storage**: Room schema 5 to 6: new table `text_clip` (id, projectId, text, startMs, durationMs, position x and y in permille of the frame, size in permille of frame height, font, color, alignment, background flag/color/opacity, outline flag/color/width, animation in/out kind and length) with a foreign key to project and an index on `projectId, startMs`; auto migration plus a test. Originals untouched.

**Testing**: JVM tests for commands, style clamping, row assignment, animation math and bounds; Room migration and round-trip; view model tests; Compose and accessibility checks (incl. 2x font); Paparazzi goldens for panels and the text lane; emulator tests of the renderer (bitmap size within the frame, RTL and emoji do not crash), preview with several text clips, and an export test that decodes frames and compares text presence and position to the plan; Fold and mid-range measurements deferred until the user is back.

**Target Platform**: Android, minSdk 26. Primary foldable: Samsung Fold SM-F976B (device checks deferred).

**Project Type**: mobile-app (multi-module Gradle build)

**Performance Goals**: edits visible within 100 ms; 30 fps at 1080p with up to 10 visible text clips and 3 video layers; bitmaps rendered off the render thread and cached; UI under 100 ms.

**Constraints**: offline; no activity restart on fold or rotation, open panel and typed text survive; at most 5 seconds of work lost; one undo step per gesture; text always inside the frame margin; same renderer for preview and export.

**Scale/Scope**: 1 lane, 1 clip type, 6 fonts, 4 in and 4 out animations, 3 panels (Edit, Style, Animation), preview gesture layer, lane view with rows, inspector sections; touches `core:model`, `core:data`, `core:media`, `feature:editor`, `core:designsystem` (color picker, text field styling if missing).

## Constitution Check

| Principle | Gate | Status |
|-----------|------|--------|
| I. Native Android, Kotlin-First | Media3 overlays and Android text layout in Kotlin, no new frameworks | PASS |
| II. Non-Destructive Editing | Text clips are project data with commands and undo; sources untouched | PASS |
| III. Real-Time Preview Performance | Bitmaps cached and rendered off the render thread; animation changes only overlay settings; measured later on device | PASS (verify, risk R2) |
| IV. Incremental Delivery | Independent slice on 001 to 005; templates, auto captions, keyframes excluded | PASS |
| V. Test-First on Core Logic | Commands, bounds, animation math on the JVM; export frame check | PASS |
| VI. Clean Architecture and Modularity | Engine Android-free; renderer in `core:media`; panel state in the view model | PASS |
| VII. Privacy, Safety, Resources | On-device only; no network fonts | PASS |
| VIII. Adaptive and Foldable-First UI | Panels, inspector sections, tabletop placement, state survives fold | PASS |

No violations. Post-design re-check: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/006-text-layer/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/ui-screens.md
├── checklists/requirements.md
└── tasks.md              # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/model/src/main/kotlin/.../core/model/
├── Text.kt                        # TextClip, TextStyle, TextFont, TextAlign, Background, Outline, TextAnimation, limits
├── Models.kt                      # Timeline.textLane, total duration
└── engine/
    ├── TextCommands.kt            # AddText, SetTextContent, SetTextStyle, SetTextPlacement, SetTextAnimation, MoveText, TrimText
    ├── SplitDuplicateDelete.kt    # text lane support
    ├── TextRows.kt                # row assignment for overlapping clips
    └── TimelineInvariants.kt

core/data/.../db/                  # TextClipEntity, Mappers, Daos, PetruDatabase v6 (auto migration), schemas/6.json

core/media/.../
├── text/TextRenderer.kt           # words + style -> Bitmap at a frame size (StaticLayout, outline, box, safe margin)
├── text/TextAnimator.kt           # pure: clip time -> alpha, offset, scale
├── text/TextOverlays.kt           # BitmapOverlay per clip with live state; OverlayEffect for the composition
├── composition/CompositionBuilder.kt  # composition-level OverlayEffect from the text lane
└── preview/Media3PreviewEngine.kt, PreviewEngine.kt   # setTextLive(...), refresh current frame

feature/editor/.../feature/editor/
├── EditorContract.kt, EditorViewModel.kt   # Text tab, ClipSelection.Text, panels, placement events
└── ui/
    ├── TextLaneView.kt            # lane with rows
    ├── TextEditPanel.kt, TextStylePanel.kt, TextAnimationPanel.kt, ColorPicker.kt
    ├── TextPreviewLayer.kt        # selection box, drag, pinch, center guides
    ├── MainToolBar.kt             # Text tab active with Add text
    ├── ClipToolBar.kt             # text clip tools
    ├── expanded/InspectorPanel.kt, tabletop/TabletopEditorLayout.kt

tests: matching src/test and src/androidTest in each module
```

**Structure Decision**: Reuse the multi-module layout. No new modules.

## Risks

- **R1**: Composition-level effects in `CompositionPlayer` may use a different time base or not apply in preview. Mitigation: spike S1 first (emulator): one overlay with a time window in preview and export; fallback is per-item overlays or a single `TextureOverlay` that draws all text clips itself.
- **R2**: Re-uploading bitmaps each frame or per edit is costly. Mitigation: cache bitmaps by (words, style, frame size); return the same instance while unchanged; limit re-render to edits; spike S1 measures upload behaviour.
- **R3**: A paused preview does not redraw after a live edit. Mitigation: refresh the current frame with a seek to the current position after each live update (debounced to one per 50 ms).
- **R4**: Preview and export bitmaps differ with the frame size (rounding of size and wrapping). Mitigation: all sizes are fractions of frame height and width; render at the exact output frame size in both paths; golden test of layout bounds at two sizes.
- **R5**: System fonts differ by device and fallbacks for emoji and other scripts. Mitigation: use system families with fallback; accepted in the spec; tests only check no crash and bounds.
- **R6**: Keyboard and gestures on the preview in tabletop and cover layouts. Mitigation: the edit panel is a normal text field in the panel; the preview shrinks while typing, as for other panels.
- **R7**: Room v6 and a new table in the timeline save path. Mitigation: additive table, auto migration, round-trip test, session saves text with the timeline.
