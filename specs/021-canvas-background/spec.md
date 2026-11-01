# Feature Specification: Canvas Background

**Feature Branch**: `021-canvas-background`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Canvas background: when a clip does not fill the project frame (a portrait clip in a landscape project, a square photo, a clip zoomed out), the empty bars are black today. The user can choose what fills them for the whole project: Black (default), a solid colour from a palette, or a blurred, enlarged copy of the clip itself (Blur) at three strengths. The choice shows live in the preview, is saved with the project, is exported exactly as in the preview, is one undo step, and works with transitions, transforms, filters, effects, and keyframes."

## Scope note

A project-wide setting for what is shown behind clips that do not cover the frame. Three choices: Black, Colour (from a palette of 12 and a custom picker), and Blur (a blurred enlarged copy of the clip's own picture, at Light, Medium, or Strong). Not included: per-clip backgrounds, images or gradients as background, and changing the project's frame shape (that is the format).

## Clarifications

### Session 2026-11-01

- Q: Where is the setting? → A: On the Effects tab with no clip selected there is a Background button opening a panel; it applies to the whole project.
- Q: What does the background do when a clip fills the frame? → A: Nothing is visible and nothing changes; the export of such a project is identical to before.
- Q: What does Blur show during a transition? → A: Each clip brings its own blurred copy; during a crossfade the two blend like the clips themselves.
- Q: Does it show when a keyframe zooms a clip out? → A: Keyframe zoom stays at 100% or more (feature 014), so keyframes never reveal the background by themselves; a transform that changes the clip's shape (rotate, crop) does.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Blur or colour behind a clip that does not fill the frame (Priority: P1)

A user has a portrait clip in a landscape project. They open the Effects tab, tap Background, and choose Blur. The black bars at the sides are replaced by a soft blurred enlargement of the clip itself. They can instead pick a colour. The preview shows the change at once.

**Why this priority**: This is the whole feature and a very common need.

**Independent Test**: Add a portrait photo to a landscape project, choose Blur, see blurred sides in the preview, export, and compare a corner pixel with the picture's average colour; choose a colour and compare the corner with it; choose Black and see black.

**Acceptance Scenarios**:

1. **Given** a project, **When** the user opens Effects with no clip selected and taps Background, **Then** a panel offers Black, Colour, and Blur with a selected mark on the current one.
2. **Given** a clip that leaves bars, **When** the user picks Colour and a swatch, **Then** the bars show that colour in the preview.
3. **Given** a clip that leaves bars, **When** the user picks Blur and a strength, **Then** the bars show a blurred enlarged copy of the clip's own picture, softer at Strong.
4. **Given** any change, **When** it is made, **Then** it is one undo step.
5. **Given** Black (the default), **When** a project is exported, **Then** it is the same as before this feature.
6. **Given** a clip that fills the frame, **When** any background is chosen, **Then** the picture does not change.

---

### User Story 2 - Kept, saved, exported, combined (Priority: P2)

The choice is saved with the project, kept by project duplicate, and appears in an export exactly as in the preview. It works with transitions, transforms, filters, effects, and keyframes, and in the unfolded and tabletop layouts.

**Why this priority**: Persistence, export parity, and combination with earlier features apply.

**Independent Test**: Choose Blur, add a dissolve transition between two portrait clips, apply a filter, save and reopen, export, compare a frame in the transition with the preview.

**Acceptance Scenarios**:

1. **Given** a chosen background, **When** the project is closed and reopened or duplicated, **Then** it is unchanged.
2. **Given** a clip with a filter or an effect, **When** the background is Blur, **Then** the blurred copy has the same filter and effect as the clip.
3. **Given** a transition between two clips, **When** the background is Colour or Blur, **Then** the transition shows the backgrounds of both clips blending.
4. **Given** an export, **When** a frame is compared with the preview at the same time, **Then** they match within a small visual difference.

---

### Edge Cases

- Photos behave like videos; a project of only clips that fill the frame shows no difference.
- Text and overlay layers sit on top of the background as on top of the picture.
- A transform that crops or rotates a clip uses the transformed picture for Blur.
- Reduced preview quality may lower the blur's smoothness but not its look or colours.
- Undo and redo restore the exact earlier choice.
- The setting is per project, not per clip, and not a global preference.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each project MUST have a background setting: Black (default), Colour (an ARGB colour), or Blur with a strength Light, Medium, or Strong.
- **FR-002**: The setting MUST decide what is shown in the area of the frame a clip does not cover, in the preview and in the export.
- **FR-003**: Colour MUST fill the area with the chosen colour; Blur MUST fill it with an enlarged, blurred copy of the clip's own picture (after transform, filter, and effect).
- **FR-004**: A clip that fills the frame, and any project with Black, MUST look and export exactly as before.
- **FR-005**: Changing the setting MUST be one undo step and show in the preview within one second.
- **FR-006**: The setting MUST be saved with the project, kept by project duplicate, and work with transitions, transforms, filters, effects, and keyframes.
- **FR-007**: The Background panel MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels, including each swatch.

### Key Entities *(include if feature involves data)*

- **Canvas background**: The project's choice of what fills the frame behind clips: Black, Colour, or Blur with a strength.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can choose a background in 3 taps or fewer from the Effects tab.
- **SC-002**: In an export, the bar area of a portrait photo in a landscape project is the chosen colour within a small tolerance for Colour, and differs from black by the picture's own colours for Blur.
- **SC-003**: With Black, an exported frame is identical to the export before this feature in an automated comparison.
- **SC-004**: 100% of choices survive close and reopen, undo, redo, and project duplicate in automated tests.

## Assumptions

- Blur strengths are fixed, light, medium and strong (about 2%, 4% and 8% of the frame height as the blur radius).
- The palette has 12 colours plus a custom colour picker (reusing the app's colour picker).
- The background is drawn by the same step that fits the clip into the frame, so nothing else in the project changes when it is Black.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
