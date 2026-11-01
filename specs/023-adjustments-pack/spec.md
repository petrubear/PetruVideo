# Feature Specification: Adjustments Pack

**Feature Branch**: `023-adjustments-pack`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Adjustments pack: add Exposure, Gamma, Highlights, Shadows, Tint, Hue and Sharpen to the clip adjustments next to Brightness, Contrast, Saturation, Warmth and Vignette. They show live in the preview, are saved with the project, follow undo as one step, copy with Apply to all, split and duplicate, and are exported as in the preview."

## Scope note

Seven new per-clip adjustment sliders on the existing Filters tool (video and photo clips on the main track). Not included: curves, LUT files, per-colour (HSL) control, grain, and adjustments for overlay clips.

## Clarifications

### Session 2026-11-01

- Q: Where are they? → A: In the existing Adjust section of the Filters panel, grouped as Light (Brightness, Contrast, Exposure, Gamma, Highlights, Shadows) and Colour (Saturation, Warmth, Tint, Hue) and Detail (Sharpen, Vignette).
- Q: Ranges? → A: Exposure, Gamma, Highlights, Shadows, Tint and Hue are -100 to +100 with 0 as no change; Sharpen is 0 to 100.
- Q: What does Reset do? → A: Puts all adjustments, old and new, back to 0 and keeps the look.
- Q: Do they follow Apply to all and Remove filter? → A: Yes, they are part of the clip's filter.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fix a dark or flat clip (Priority: P1)

A user's clip is underexposed with crushed shadows. They open Filters, Adjust, raise Exposure and Shadows and lower Highlights, and the picture opens up in the preview as they drag.

**Why this priority**: Light correction is the most common need after brightness and contrast.

**Independent Test**: Add a dark clip, raise Exposure and Shadows, see the picture brighten in the preview, export, and compare the mean brightness with the preview frame.

**Acceptance Scenarios**:

1. **Given** a clip, **When** the user opens Filters then Adjust, **Then** the sliders Brightness, Contrast, Exposure, Gamma, Highlights, Shadows, Saturation, Warmth, Tint, Hue, Sharpen and Vignette are offered, each starting at its neutral value.
2. **Given** a slider, **When** it moves, **Then** the preview shows the result live.
3. **Given** Exposure above 0, **When** shown, **Then** the whole picture is brighter, evenly in stops; below 0 it is darker.
4. **Given** Gamma above 0, **When** shown, **Then** midtones are brighter while black and white points stay.
5. **Given** Shadows above 0 or Highlights below 0, **When** shown, **Then** dark areas lift or bright areas come down without changing the other end much.
6. **Given** any committed change, **When** made, **Then** it is one undo step.

---

### User Story 2 - Colour and detail (Priority: P2)

The user shifts Tint towards green or magenta, rotates Hue, and adds Sharpen to soft footage.

**Why this priority**: Completes the common grading controls.

**Independent Test**: Raise Tint, export, and see the mean green and magenta channels move in opposite directions; raise Sharpen on a soft edge and see the edge contrast rise.

**Acceptance Scenarios**:

1. **Given** Tint above 0, **When** shown, **Then** the picture shifts towards magenta; below 0 towards green.
2. **Given** Hue at +100 or -100, **When** shown, **Then** colours are rotated by up to half a turn and greys stay grey.
3. **Given** Sharpen above 0, **When** shown, **Then** edges look crisper; at 0 nothing changes.

---

### User Story 3 - Kept, saved, exported (Priority: P2)

The adjustments are saved with the project, kept by duplicate, split, Apply to all, and project duplicate, and exported as in the preview, together with the look, effects, transitions, and keyframes.

**Independent Test**: Set all seven, close and reopen, apply to all, export, and compare a frame with the preview.

**Acceptance Scenarios**:

1. **Given** adjusted clips, **When** the project is closed and reopened or duplicated, **Then** all values are unchanged.
2. **Given** an adjusted clip, **When** it is split or duplicated, **Then** each part keeps the values.
3. **Given** an export, **When** a frame is compared with the preview at the same time, **Then** they match within a small visual difference.
4. **Given** all adjustments at their neutral values, **When** exported, **Then** the output is identical to before this feature.

---

### Edge Cases

- Projects saved before this feature open with all seven at 0.
- Extreme values never produce NaN or black frames; results are clamped to the displayable range.
- Photos behave like videos.
- Reset puts every adjustment at neutral; Remove filter clears the look and all adjustments.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each clip's filter MUST carry Exposure, Gamma, Highlights, Shadows, Tint, Hue (each -100 to +100, default 0) and Sharpen (0 to 100, default 0).
- **FR-002**: The values MUST show live in the preview and in the export, in this order: the existing colour transform, then the new adjustments, then the vignette.
- **FR-003**: Exposure MUST scale linear light by up to plus or minus 2 stops; Gamma MUST bend midtones by up to a factor of 2 either way; Highlights and Shadows MUST act mainly on the bright and dark ends; Tint MUST move green-magenta; Hue MUST rotate hue by up to 180 degrees; Sharpen MUST raise edge contrast.
- **FR-004**: A filter with every adjustment at neutral MUST look and export exactly as before this feature.
- **FR-005**: The values MUST be saved, copied by duplicate and split, applied by Apply to all, reset by Reset, and cleared by Remove filter.
- **FR-006**: Each committed change MUST be one undo step.
- **FR-007**: The Adjust section MUST stay usable in all layouts, meet touch-target and contrast rules, and have accessible labels for each slider.

### Key Entities *(include if feature involves data)*

- **Clip filter**: The clip's look and now twelve adjustments (five existing, seven new).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can raise exposure of a clip in 3 taps or fewer after selecting it (Filters, Adjust, drag).
- **SC-002**: In an export, Exposure +50 raises the mean brightness of a mid-grey frame by about one stop within a small tolerance; Hue at 0 and Sharpen at 0 change nothing.
- **SC-003**: With all adjustments neutral, an exported frame is identical to the export before this feature.
- **SC-004**: 100% of values survive close and reopen, undo, redo, split, duplicate, Apply to all, and project duplicate in automated tests.

## Assumptions

- All seven run in one extra shader pass; Sharpen uses a small unsharp mask over the four neighbouring pixels.
- Exposure works in linear light (approximate gamma 2.2); Highlights and Shadows are luminance-weighted curves.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
