# Feature Specification: Text Layer - Titles and Captions

**Feature Branch**: `006-text-layer`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Text on the timeline: add titles and captions as a text layer over the video. Add text at the playhead, edit the words, choose font, size, color, alignment, background and outline, position it on the preview by dragging, set its start and duration on the timeline, and apply a simple in and out animation. Live in the preview, respected by export."

## Scope note

This feature adds a third lane to the timeline, the Text lane, holding text clips that sit over the video. The Text tab of the main tool bar, disabled until now, becomes active. Export (004) renders the text exactly as the preview shows it. Not included: text templates and presets packs, auto captions from speech, keyframe animation, per-character styling, and curved or 3D text.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a title and edit its words (Priority: P1)

A user opens the Text tab, taps Add text, and a text clip with the placeholder "Your text" appears in the Text lane at the playhead, five seconds long, and as a line in the middle of the preview. They tap it, type their own words with the keyboard, and see them in the preview at once.

**Why this priority**: A title with the user's words is the smallest useful text feature.

**Independent Test**: Add a clip, add text at 3 s, type "Day one", see it in the preview and the Text lane at 3 s for 5 s, play across it and see it appear and disappear, undo twice and see it gone.

**Acceptance Scenarios**:

1. **Given** a project with a video, **When** the user taps Add text, **Then** a text clip appears at the playhead for 5 s (shorter if the project ends sooner, not less than 0.5 s), is selected, and its edit panel opens with the keyboard ready.
2. **Given** a selected text clip, **When** the user types, **Then** the preview updates within 100 ms, multi-line text is allowed, and an empty text keeps the clip but shows nothing.
3. **Given** the playhead is outside the text clip's time, **When** the user plays, **Then** the text shows only between its start and end.
4. **Given** the project has no video yet, **When** the user taps Add text, **Then** the text can still be added and the project length follows the text clip.

---

### User Story 2 - Style the text (Priority: P1)

A user picks a font from a short list, sets the size, the text color, the alignment, a background box (color and opacity) and an outline (color and width), and sees every change live.

**Why this priority**: Unstyled text is rarely usable; legibility over varied footage needs background and outline.

**Independent Test**: Style a title with a bold font, 64 size, yellow, centered, a dark box at 60% and a black outline; reopen the project and find it identical; export and see the same look.

**Acceptance Scenarios**:

1. **Given** a selected text clip, **When** the user opens Style, **Then** they see Font (at least 6 fonts: Sans, Serif, Mono, Display, Handwritten, Bold), Size, Color, Alignment (left, center, right), Background (on/off, color, opacity) and Outline (on/off, color, width).
2. **Given** the style panel is open, **When** the user changes any option, **Then** the preview shows it within 100 ms, and one gesture is one undo step.
3. **Given** text wider than the frame, **When** it is shown, **Then** it wraps inside the frame with a safe margin and is never cut off at the frame edge.
4. **Given** a style change, **When** the user taps Reset style, **Then** the style returns to the defaults (Sans, 48, white, center, no background, no outline) and the words stay.

---

### User Story 3 - Position the text on the preview (Priority: P1)

A user drags the text on the preview to move it, uses a pinch or a size control to scale it, and a guide shows when it is centered. The position is kept as a share of the frame so it looks the same in any export size.

**Why this priority**: Captions at the bottom and titles in the middle are the main layouts.

**Independent Test**: Drag the text to the bottom third, see center guides while dragging near the middle, export at 720p and at 1080p and see it at the same relative place.

**Acceptance Scenarios**:

1. **Given** a selected text clip, **When** the user drags it on the preview, **Then** it follows the finger, snaps to the horizontal and vertical center with a guide line within 8 dp, and stays inside the frame.
2. **Given** a selected text clip, **When** the user pinches on the preview, **Then** the size changes live between the minimum and maximum size.
3. **Given** two text clips overlap in time, **When** one is selected, **Then** only the selected one is moved by dragging in its area, and the other is not changed.
4. **Given** the project format changes size at export, **When** the video is exported, **Then** the text keeps its relative position and relative size.

---

### User Story 4 - Time the text on the timeline (Priority: P2)

A user sees the text clips in a Text lane under the video, drags a clip to change its start, trims its ends to change its duration, splits it at the playhead, duplicates and deletes it. Text clips may overlap in time, using separate rows.

**Why this priority**: Timing is part of making captions, but a default placement already works.

**Independent Test**: Add three text clips, make two overlap, trim and move them, split one at the playhead, delete one, undo each step.

**Acceptance Scenarios**:

1. **Given** the Text lane, **When** a clip is dragged, **Then** its start changes, it never goes below 0, and the project length extends when it ends after the video.
2. **Given** a selected text clip, **When** its handles are dragged, **Then** its duration changes in a range from 0.2 s up to the maximum of 10 minutes, with one undo step per drag.
3. **Given** overlapping text clips, **When** the lane is shown, **Then** they are drawn on separate rows and all remain selectable.
4. **Given** a selected clip, **When** Split, Duplicate, or Delete is used, **Then** it behaves as for video clips: split keeps the text and style on both halves, duplicate is placed right after the original.

---

### User Story 5 - Animate in and out (Priority: P2)

A user chooses an in animation (None, Fade, Slide up, Pop) and an out animation (None, Fade, Slide down, Pop) and its length (0.2 s to 1.5 s), and sees it in the preview and in the export.

**Why this priority**: Motion makes titles feel finished, but static text is already useful.

**Independent Test**: Apply Fade in and Slide down out of 0.5 s, play the clip and see both, export and see the same.

**Acceptance Scenarios**:

1. **Given** a selected text clip, **When** the user opens Animation, **Then** they see In and Out choices with a length for each, all None by default.
2. **Given** an animation is set, **When** the clip plays, **Then** the in animation runs over the first part of the clip and the out over the last part, and both are limited to half of the clip's duration.
3. **Given** a clip trimmed shorter than its animations, **When** shown, **Then** the animation lengths are limited without changing the stored choice.

---

### User Story 6 - Text in the other layouts, saved and exported (Priority: P3)

The Text tool works in the unfolded editor (inspector) and in tabletop (panel below the hinge), text is saved with the project, and the exported video shows the text as in the preview.

**Why this priority**: Layout rules and persistence from earlier features apply to every tool.

**Independent Test**: Style text, save, force-close, reopen; unfold and use the inspector; half-fold and use the panel; export and compare one frame to the preview.

**Acceptance Scenarios**:

1. **Given** the unfolded editor, **When** a text clip is selected, **Then** the inspector hosts the words, style and animation controls with the same effects as the phone panels.
2. **Given** tabletop posture, **When** the text panel is open, **Then** it sits on the controls side of the hinge and nothing interactive is under the hinge.
3. **Given** an open text panel, **When** the device is folded, unfolded, or rotated, **Then** it stays open with the same words, cursor, and values.
4. **Given** a project with text, **When** it is exported, **Then** the text appears at the same relative position, size, style, timing, and animation as in the preview.

---

### Edge Cases

- Very long text: it wraps and shrinks nothing automatically; if it does not fit the frame height, it is clipped at the safe margin and a hint says "Text does not fit"; the user can reduce the size.
- Emoji and non-Latin scripts (accents, CJK, Arabic, Hebrew): they display with the device's fallback fonts; right-to-left text aligns correctly.
- Text on a project without video or with gaps: the text still plays and exports over black.
- A text clip beyond the end of the video lengthens the project; deleting it shortens it again.
- Splitting at a position inside an animation: both halves keep the choices; the out animation of the first half and the in animation of the second half are set to None.
- A missing font on the device: the fallback font is used with the same size and the choice is kept.
- Many text clips visible at once (up to 10) on a mid-range device: the preview stays at 30 fps or the app lowers preview quality and says so.
- Force-close right after an edit: the words and style come back as set, with at most 5 seconds of work lost.
- Typing while playing: playback keeps running; the keyboard never covers the text being edited in the preview (the preview shifts or shrinks).

## Requirements *(mandatory)*

### Functional Requirements

**Adding and editing**

- **FR-001**: Users MUST be able to add a text clip at the playhead, 5 s long by default (at least 0.5 s), with the placeholder "Your text", selected with its edit panel open.
- **FR-002**: Users MUST be able to edit the words, including several lines; the preview MUST update within 100 ms of each keystroke.
- **FR-003**: The Text tab of the main tool bar MUST become active and offer Add text; selecting a text clip MUST show a clip tool bar with Edit, Style, Animation, Split, Duplicate, Delete.

**Style**

- **FR-004**: Style MUST include at least six fonts, a size range with a minimum and maximum, text color, alignment (left, center, right), background (on/off, color, opacity 0% to 100%) and outline (on/off, color, width).
- **FR-005**: Style changes MUST appear in the preview within 100 ms and be one undo step per gesture; Reset style MUST restore the defaults and keep the words.
- **FR-006**: Text MUST wrap inside the frame with a safe margin and MUST NOT be cut at the frame edge in the preview or the export.

**Position**

- **FR-007**: Users MUST be able to move a text clip by dragging it on the preview and scale it by pinching; the position and size MUST be stored relative to the frame and the text MUST stay inside the frame.
- **FR-008**: Dragging MUST snap to the frame's horizontal and vertical center with a visible guide, and one drag or pinch MUST be one undo step.

**Timeline**

- **FR-009**: The timeline MUST show a Text lane under the video lane; text clips MAY overlap in time and MUST then be drawn on separate rows.
- **FR-010**: Users MUST be able to move, trim (0.2 s minimum), split, duplicate, and delete text clips; each action is undoable; the project length MUST follow the end of the last clip of any lane.
- **FR-011**: Moving or trimming text MUST NOT change the video or audio lanes.

**Animation**

- **FR-012**: Each text clip MUST have an in animation (None, Fade, Slide up, Pop) and an out animation (None, Fade, Slide down, Pop), each with a length from 0.2 s to 1.5 s, all None by default.
- **FR-013**: Animations MUST be limited so that their lengths together never exceed the clip's duration, and MUST play in the preview and in the export.

**Persistence and export**

- **FR-014**: Text clips MUST be saved with the project, restored on reopening, and covered by the same autosave guarantee as other edits (at most 5 seconds lost).
- **FR-015**: Export MUST render text with the same words, wrapping, style, relative position, relative size, timing, and animation as the preview, at any export resolution.
- **FR-016**: Source media files MUST NOT be modified by these tools.

**Design and layout**

- **FR-017**: The Text tools MUST follow the existing visual language and work in compact, medium, expanded (inspector from 840 dp), and tabletop arrangements, keep an open panel and its values through fold, unfold, and rotation, and keep nothing interactive under the hinge.
- **FR-018**: Controls MUST have accessible labels, touch targets of at least 48 dp, scalable text, and values readable by a screen reader; the text clip in the timeline MUST expose its words to a screen reader.

### Key Entities

- **Text clip**: words, start on the timeline, duration, style, position and size relative to the frame, in and out animation.
- **Text style**: font, size, color, alignment, background (on/off, color, opacity), outline (on/off, color, width).
- **Text animation**: a kind and a length, for in and for out.
- **Text lane**: the ordered set of text clips of a project, which may overlap in time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a title with their own words in under 20 seconds from opening the Text tab, on their first attempt.
- **SC-002**: A word, style, position, or timing change is visible in the preview within 100 ms.
- **SC-003**: With up to 10 text clips visible and 3 video layers, preview holds 30 frames per second at 1080p on a mid-range device, or the app lowers preview quality and says so.
- **SC-004**: 100% of text edits can be undone and redone back to the identical project.
- **SC-005**: After force-closing the app right after an edit, 100% of projects reopen with the same text and style (at most 5 seconds of work lost).
- **SC-006**: In an exported file, text appears within 1 frame of its start and end in the preview and covers the same relative area within 1% of the frame size, at 720p, 1080p, and 4K.
- **SC-007**: Text never extends outside the frame (safe margin respected) in 100% of tested sizes, fonts, and lengths.
- **SC-008**: On each affected screen and arrangement, a design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.
- **SC-009**: Source media files are byte-identical before and after these edits in 100% of tested cases.

## Assumptions

- Features 001 to 005 are in place and reused: the editing engine, undo, autosave, the clip tool bar, inspector, tabletop layout, the shared composition builder, and export.
- The designer's reference shows a Text tab but no panels for it; the controls follow the existing visual language and are reviewed with the designer, with deviations recorded.
- Fonts come from the device and a small bundled set; the exact list is chosen during planning.
- Text is a single style per clip (no per-word styling); color pickers offer a fixed palette plus a custom color.
- Animations are simple presets with a length; keyframes and custom curves belong to a later feature.
- Subtitles imported from files and automatic captions are out of scope.
