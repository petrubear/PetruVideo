# Feature Specification: Text Rotation, Shadow and Spacing

**Feature Branch**: `036-text-rotation-shadow-spacing`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "Text rotation, shadow and letter spacing."

## Scope note

Text clips today have a font, color, alignment, a background box, an outline, animations and keyframes for position, size and opacity. This feature adds four style controls to the same Style tool of a text clip: **Rotation**, **Shadow**, **Letter spacing** and **Line spacing** (the last one comes with letter spacing because both change how a block of text is laid out). They apply to any text clip, including auto captions, are seen live in the preview, are saved with the project and are rendered by export exactly as seen. Not included: rotation as a keyframe, per-letter or per-word styling, curved text, gradient or glow, 3D tilt, and changes to the existing background and outline.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rotate a text (Priority: P1)

A user selects a text clip, opens Style, moves the Rotation control to tilt the title by 12 degrees, and sees it tilted in the preview. They set it back to 0 with one tap.

**Why this priority**: Tilted titles are the most requested missing look.

**Independent Test**: Add a text, rotate it to 15, 90, -45 and 180 degrees, and check the preview, then export and compare.

**Acceptance Scenarios**:

1. **Given** a text clip, **When** the user moves Rotation, **Then** the whole text, with its background box, outline and shadow, turns around its center within 100 ms of the drag.
2. **Given** Rotation is not 0, **When** the user taps Reset on it, **Then** the text is upright again and the other style settings are kept.
3. **Given** the user drags near 0, 90, -90 or 180 degrees, **When** the value is within 3 degrees of one, **Then** it snaps to it so a straight text is easy to get.
4. **Given** a rotated text, **When** it is moved or resized or has an animation or keyframes, **Then** it keeps its angle while it moves.

---

### User Story 2 - Add a shadow so text reads on any picture (Priority: P1)

A user puts white text over a bright sky and can hardly read it. They turn on Shadow, pick the default dark shadow, and the text now stands out.

**Why this priority**: Readability on busy or bright video is the most common text problem.

**Independent Test**: Put white text on a white picture, check it is unreadable, turn on Shadow and check it is readable, then export and compare.

**Acceptance Scenarios**:

1. **Given** a text clip, **When** the user turns Shadow on, **Then** a soft dark shadow appears behind the text with sensible defaults.
2. **Given** Shadow is on, **When** the user changes its color, opacity, softness, distance or direction, **Then** the preview changes within 100 ms.
3. **Given** Shadow is off, **When** the user turns it on again, **Then** the previous settings come back.
4. **Given** a text with a shadow, **When** the background box is on, **Then** the shadow follows the box; **When** it is off, **Then** the shadow follows the letters.

---

### User Story 3 - Space the letters and lines (Priority: P2)

A user spreads the letters of a title wide for a cinematic look and tightens the line spacing of a two-line caption.

**Why this priority**: Spacing is a basic typographic control that makes titles look designed.

**Independent Test**: Set letter spacing to a wide and a tight value and line spacing to tight and loose on a two-line text, and check the preview and export.

**Acceptance Scenarios**:

1. **Given** a text clip, **When** the user changes Letter spacing from tight to wide, **Then** the gaps between letters change in the preview and the text block grows or shrinks around its center.
2. **Given** a text of two or more lines, **When** the user changes Line spacing, **Then** the distance between lines changes.
3. **Given** the defaults, **When** the user does not touch these controls, **Then** the text looks exactly as it does today.

---

### Edge Cases

- A project made before this feature opens and exports with no change: rotation 0, no shadow, spacing at the default.
- A rotated text can extend past the edge of the frame; it is clipped by the frame like any text, with no error.
- Text with the background box on: the box rotates with the text; the shadow is drawn behind the box shape when the box is on, and behind the letters when it is off.
- Very wide letter spacing can make a line longer than the frame; the text wraps as it does today and the user can also lower the size.
- Auto captions use the same controls; changing the style of one caption applies to that caption only, as today.
- Undo and redo treat each change of rotation, shadow, letter spacing or line spacing as one step.
- All new labels appear in English and Spanish.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A text clip MUST have a Rotation from -180 to +180 degrees, default 0, set from its Style tool with a control that shows the value in degrees, a Reset, and snapping within 3 degrees of 0, 90, -90 and 180.
- **FR-002**: Rotation MUST turn the whole text as one piece (letters, background box, outline and shadow) around the center of the text, and MUST be kept while the text is moved, resized, animated or keyframed.
- **FR-003**: A text clip MUST have a Shadow that is off by default, with a switch and these settings: color, opacity (0 to 100%), softness (0 to 100%), distance (0 to 100%) and direction (0 to 360 degrees). Turning it off keeps the settings.
- **FR-004**: The default shadow when first turned on MUST be a soft dark shadow, offset slightly down and to the right, readable on a light picture.
- **FR-005**: A text clip MUST have Letter spacing from -10% to +100% of the text size, default 0, and Line spacing from 70% to 200% of the normal line height, default 100%, each with its value shown and a Reset.
- **FR-006**: With every new setting at its default, a text clip MUST look and export exactly as it does today, and projects made before this feature MUST open and export unchanged.
- **FR-007**: Changes MUST be seen in the preview within 100 ms of the control change and MUST NOT pause playback.
- **FR-008**: Export MUST render rotation, shadow and spacing exactly as seen in the preview.
- **FR-009**: The new settings MUST be saved with the project, kept through trim, split, duplicate and move of the text clip, and copied when the clip is duplicated.
- **FR-010**: Auto caption clips MUST support the new settings like any text clip.
- **FR-011**: Each change of a new setting MUST be one undo step.
- **FR-012**: Controls MUST show current values as numbers with units, work with accessible labels and a screen reader, have touch targets of at least 48 dp, and keep working with large text sizes.
- **FR-013**: All new labels MUST exist in English and Spanish and follow the phone's language.

### Key Entities

- **Text style**: gains rotation (degrees), shadow (on or off, color, opacity, softness, distance, direction), letter spacing (percent of size) and line spacing (percent of line height). One per text clip, stored with the clip's other style settings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can tilt a text to a chosen angle in three taps or fewer after selecting it.
- **SC-002**: A user can turn on a readable shadow in two taps or fewer, with no other change.
- **SC-003**: White text placed over a white picture is unreadable without a shadow and readable with the default shadow, judged by reviewers on a still frame.
- **SC-004**: A change of any new control shows in the preview within 100 ms, and playback never pauses for it.
- **SC-005**: An exported frame matches the preview for rotated, shadowed and spaced text with no visible difference.
- **SC-006**: Existing projects open with no visible change, and all existing text screenshots pass without re-recording.
- **SC-007**: All new labels read correctly in English and Spanish.

## Assumptions

- Rotation is a fixed setting of the clip and is not keyframed in this feature; keyframes keep controlling position, size and opacity.
- Line spacing comes with this feature because it was part of the recommended text style batch and uses the same layout step as letter spacing.
- The Style tool of a text clip already shows its controls in a scrolling panel, so the new controls are added there in the order: Rotation, Shadow, Letter spacing, Line spacing.
- The shadow direction control is a slider in degrees; a visual angle dial is not needed.
