# Feature Specification: Filters and Color Adjustments

**Feature Branch**: `007-filters-color`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Filters and color adjustments for video and photo clips: a Filters tab with preset looks (Vivid, Warm, Cool, Black and white, Vintage, Faded) with an intensity slider, and manual adjustments (brightness, contrast, saturation, warmth, vignette) per clip. Live in the preview, kept by split and duplicate, saved with the project, and rendered by export exactly as in the preview."

## Scope note

The Filters tab of the main tool bar, disabled until now, becomes active. A filter look and five manual adjustments apply to one video or photo clip at a time. Not included: filters on text, audio, or the whole project at once, custom user looks or importable look files, keyframed (changing over time) adjustments, masks and selective color, and curves.

## Clarifications

### Session 2026-11-01

- Q: Does the Filters tab offer Apply to all? → A: Yes, one tap, one undo step.
- Q: How are looks previewed in the list? → A: On the selected clip's frame at the playhead, with each look applied.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Apply a look to a clip (Priority: P1)

A user selects a video or photo clip, opens Filters, taps a look such as Warm, and sees the clip with that look at once in the preview. A slider sets how strong the look is, and tapping None removes it.

**Why this priority**: One-tap looks are the fastest way to change how a clip feels and the core of this feature.

**Independent Test**: Select a clip, tap Vintage, see the change in the preview, drag Intensity to 50% and see it halve, tap None and see the original, undo and redo each step.

**Acceptance Scenarios**:

1. **Given** a selected video or photo clip, **When** the user opens Filters, **Then** they see None plus Vivid, Warm, Cool, Black and white, Vintage, and Faded, each with a small preview, and None selected by default.
2. **Given** a look is tapped, **When** it applies, **Then** the preview shows it within 100 ms (while playing and while paused) and the intensity is 100%.
3. **Given** a look is applied, **When** the user drags Intensity from 0% to 100%, **Then** the look fades in smoothly and 0% looks the same as None.
4. **Given** a look is applied, **When** the user taps another look or None, **Then** the look is replaced and the manual adjustments stay.
5. **Given** a selected clip without picture (an audio clip or a text clip), **When** it is selected, **Then** the Filters tool is not offered for it.

---

### User Story 2 - Adjust the colors by hand (Priority: P1)

A user opens Adjust and moves five sliders: Brightness, Contrast, Saturation, Warmth, and Vignette. Each shows its value and the preview changes while dragging.

**Why this priority**: Looks are a start; matching clips shot at different times needs manual control.

**Independent Test**: Raise Brightness and Saturation, lower Warmth, add Vignette, see each live, reset all, save and reopen to find the same values, export and see the same look.

**Acceptance Scenarios**:

1. **Given** a selected clip, **When** the user opens Adjust, **Then** they see Brightness, Contrast, Saturation, Warmth (all from -100 to +100, default 0) and Vignette (0 to 100, default 0), each with its number.
2. **Given** playback is running, **When** the user drags a slider, **Then** the change is seen within 100 ms and playback does not pause.
3. **Given** all adjustments at their defaults and no look, **When** the clip plays, **Then** it looks the same as without any filter.
4. **Given** values were changed, **When** the user taps Reset, **Then** all five return to their defaults and the look is kept; a second Reset action, Remove all, clears the look as well.
5. **Given** a slider drag, **When** the user releases, **Then** it is one edit and one undo step.

---

### User Story 3 - Copy a look to other clips (Priority: P2)

A user wants the same look on every clip of a scene. They apply the current clip's filter and adjustments to all video and photo clips with one tap, or duplicate a clip and find the same look on the copy.

**Why this priority**: Matching a whole sequence is a very common need and saves many taps, but a single clip already works.

**Independent Test**: Style one clip, tap Apply to all, see every video and photo clip with the same look, undo once and see all return.

**Acceptance Scenarios**:

1. **Given** a clip with a look or adjustments, **When** the user taps Apply to all, **Then** every video and photo clip gets the same look, intensity, and adjustments, and one undo step restores all.
2. **Given** a clip with a filter, **When** it is duplicated, **Then** the copy has the same filter; **When** it is split, **Then** both halves have it.
3. **Given** Apply to all, **When** the project has a single clip, **Then** the action is dimmed.

---

### User Story 4 - Filters in the other layouts, saved, exported (Priority: P3)

The Filters panel works in the unfolded editor (inspector) and in tabletop (below the hinge). Looks and adjustments are saved with the project, and the exported video matches the preview.

**Why this priority**: Layout, persistence, and export rules from earlier features apply to every tool.

**Independent Test**: Adjust a clip, save, force-close, reopen; unfold and use the inspector; half-fold and use the panel; export and compare a frame to the preview.

**Acceptance Scenarios**:

1. **Given** the unfolded editor, **When** a clip is selected, **Then** the inspector hosts the look list and the five sliders with the same effects as the phone panel.
2. **Given** tabletop posture, **When** the panel is open, **Then** it sits on the controls side of the hinge and nothing interactive is under the hinge.
3. **Given** an open panel, **When** the device is folded, unfolded, or rotated, **Then** it stays open with the same values.
4. **Given** a project with filters, **When** it is exported, **Then** each clip shows the same look as in the preview, at any export size and frame rate.
5. **Given** a clip with a filter on the timeline, **When** it is shown, **Then** a small "Fl" badge appears with the other clip badges.

---

### Edge Cases

- Black and white with Saturation adjusted: the saturation slider has no visible effect on the look at full intensity; at lower intensity it changes the remaining color.
- Very extreme combinations (Brightness +100, Contrast +100): highlights clip to white without color artifacts and the video does not flicker.
- HDR sources: the filters apply on the standard-range picture used in the preview and export.
- Photos: filters apply the same way; a photo clip's look is constant for its whole length.
- Missing source media: the settings stay editable and are kept.
- Many filtered clips in a row on a mid-range device: preview stays at 30 fps or the app lowers preview quality and says so.
- A clip with a filter that is shorter after a trim: the filter is unchanged.
- Force-close right after a change: looks and adjustments come back as set, with at most 5 seconds of work lost.

## Requirements *(mandatory)*

### Functional Requirements

**Looks**

- **FR-001**: The Filters tab MUST be active and, with a video or photo clip selected, offer Filters (looks) and Adjust.
- **FR-002**: Users MUST be able to choose one look per clip from None, Vivid, Warm, Cool, Black and white, Vintage, and Faded, default None.
- **FR-003**: Each look MUST have an intensity from 0% to 100% (default 100%); 0% MUST equal None.
- **FR-004**: The look list MUST show a small preview of each look on the selected clip's frame at the playhead.

**Adjustments**

- **FR-005**: Users MUST be able to set Brightness, Contrast, Saturation, and Warmth from -100 to +100 and Vignette from 0 to 100, all defaulting to 0, each with its value shown as a number.
- **FR-006**: With every adjustment at its default and no look, the clip's picture MUST be unchanged.
- **FR-007**: Reset MUST return the five adjustments to their defaults and keep the look; Remove all MUST clear the look and the adjustments.

**Live preview and editing**

- **FR-008**: A look, intensity, or adjustment change MUST be seen in the preview within 100 ms, while playing and while paused, and playback MUST NOT pause for it.
- **FR-009**: Every change MUST be undoable and redoable as one step per gesture (one tap or one slider drag).
- **FR-010**: Apply to all MUST copy the selected clip's look, intensity, and adjustments to every video and photo clip as one undo step; it MUST be dimmed when the project has one picture clip.
- **FR-011**: Split, duplicate, trim, speed change, and move MUST keep a clip's filter settings (both halves of a split get them).
- **FR-012**: The Filters tool MUST NOT be offered for audio and text clips.

**Persistence and export**

- **FR-013**: Looks, intensities, and adjustments MUST be saved with the project, restored on reopening, and covered by the same autosave guarantee (at most 5 seconds lost).
- **FR-014**: Export MUST render each clip with the same look and adjustments as the preview, at any export size and frame rate.
- **FR-015**: Source media files MUST NOT be modified.

**Design and layout**

- **FR-016**: The Filters panels MUST follow the existing visual language and work in compact, medium, expanded (inspector from 840 dp), and tabletop arrangements, keep an open panel and its values through fold, unfold, and rotation, and keep nothing interactive under the hinge.
- **FR-017**: Controls MUST have accessible labels, touch targets of at least 48 dp, scalable text, and values readable by a screen reader.
- **FR-018**: A clip with a look or any adjustment other than its default MUST show a compact "Fl" badge on the timeline.

### Key Entities

- **Filter look**: one of Vivid, Warm, Cool, Black and white, Vintage, Faded, or None, with an intensity (0 to 100%).
- **Color adjustments**: brightness, contrast, saturation, warmth (each -100 to +100), and vignette (0 to 100).
- **Clip** (video or photo, from 001 to 006): gains a filter look and color adjustments.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can apply a look to a clip within 10 seconds of selecting it, on their first attempt.
- **SC-002**: A look, intensity, or slider change is seen within 100 ms and playback never pauses for it.
- **SC-003**: With filters on every clip of a project (up to 10 clips) and up to 3 video layers, preview holds 30 frames per second at 1080p on a mid-range device, or the app lowers preview quality and says so.
- **SC-004**: 100% of filter changes can be undone and redone back to the identical project.
- **SC-005**: After force-closing the app right after a change, 100% of projects reopen with the same filters (at most 5 seconds lost).
- **SC-006**: With every look off and every adjustment at default, exported frames are identical to the unfiltered export within encoder noise in 100% of tested cases; with a filter on, the average color of an exported frame matches the preview's within 3% of the full range.
- **SC-007**: On each affected screen and arrangement, a design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.
- **SC-008**: Source media files are byte-identical before and after these edits in 100% of tested cases.

## Assumptions

- Features 001 to 006 are in place and reused: the editing engine, undo, autosave, the clip tool bar, inspector, tabletop layout, the shared composition builder, and export.
- The designer's reference shows a Filters tab but no panels for it; the controls follow the existing visual language and are reviewed with the designer, with deviations recorded.
- Looks are built-in; their exact strengths are tuned by eye during implementation.
- Filters apply to the clip's picture only, in a fixed order: look, then adjustments, then vignette.
- Apply to all copies settings once; later changes to one clip do not follow the others.
