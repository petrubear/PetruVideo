# Feature Specification: Speed Ramps

**Feature Branch**: `018-speed-ramps`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Speed ramps: on a selected video clip with sound or without, the user can change the clip's speed smoothly over its length instead of using one constant speed. A Speed curve tool offers presets (Montage, Hero time, Bullet time, Jump, Flash in, Flash out) and a Custom graph where the user adds, drags and removes points (speed from 0.1x to 10x along the clip) with a live readout of the resulting clip length. The picture and attached sound follow the curve (sound keeps its pitch), the clip's timeline length changes to match, and Reset returns to the constant speed. It shows live in the preview, is saved with the project, exported exactly as in the preview, and works with trim, split, duplicate, relink, keyframes, transform, reverse, freeze, transitions and the clip's audio tools; each change is one undo step."

## Scope note

A video clip on the main track can play at a speed that changes along the clip: for example slow, then fast, then slow again. The speed is set by a curve with points; between points the speed changes smoothly. The curve replaces the clip's constant speed while it is on. Not included: speed curves for audio-lane clips, overlays, or photos; frame blending or optical flow for slow motion (the picture shows the nearest frames); curves that change the order of frames.

## Clarifications

### Session 2026-11-01

- Q: Along what is the curve drawn? → A: Along the clip as it plays, from its start to its end. A point sits at a moment of the clip (in the clip's own source time) and has a speed from 0.1x to 10x.
- Q: How does the curve relate to the constant speed? → A: The curve replaces it. While a curve is on, the constant Speed control is dimmed with the reason "This clip uses a speed curve". Reset returns the clip to constant speed 1x.
- Q: What happens to a curve when the clip is trimmed or split? → A: Each result keeps the part of the curve that lies in its part of the clip, with a point added at each new edge holding the speed there, so the speed at every moment stays the same.
- Q: Are presets fixed? → A: Yes, six named shapes; picking one replaces the curve and the user can then edit it as Custom.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Use a speed curve preset (Priority: P1)

A user selects a clip, opens Speed curve, and taps a preset such as Hero time. The clip slows down in the middle and runs fast at the ends. The preview shows it at once, the clip's length on the timeline changes to match, and Reset goes back to 1x.

**Why this priority**: Presets give the effect in one tap and are what most people use.

**Independent Test**: Select a 6 second clip, pick Hero time, see the length change and the picture slow in the middle; export and compare the length and a few frames; Reset and see 1x.

**Acceptance Scenarios**:

1. **Given** a selected clip, **When** the user opens Speed curve, **Then** a graph shows the clip's speed along its length and six presets are offered.
2. **Given** a preset, **When** the user taps it, **Then** the curve is set, the clip's timeline length is recalculated, and the preview plays with that speed from the next moment.
3. **Given** a clip with a curve, **When** the user taps Reset, **Then** the curve is removed, the speed is 1x, and the clip's length returns to its trimmed length.
4. **Given** one tap on a preset or Reset, **When** it is made, **Then** it is one undo step.
5. **Given** a curve, **When** the clip has attached sound, **Then** the sound follows the speed and keeps its pitch.

---

### User Story 2 - Edit the curve by hand (Priority: P2)

A user taps Custom and sees the curve as a graph with points. They drag a point up or down to change its speed (0.1x to 10x) and left or right to move it along the clip, tap the graph to add a point, and remove a point. A readout shows the resulting clip length while they drag.

**Why this priority**: Manual editing is needed for control beyond presets.

**Independent Test**: Start from a preset, add a point, drag it to 0.2x, see the length readout grow, release, and see the change apply; remove the point.

**Acceptance Scenarios**:

1. **Given** the Custom graph, **When** the user drags a point, **Then** the readout shows the new clip length live and the change applies when the finger lifts, as one undo step.
2. **Given** the graph, **When** the user taps an empty place, **Then** a point is added there holding the curve's speed at that moment.
3. **Given** a selected point, **When** the user removes it, **Then** the curve goes on without it; at least two points always remain (the two ends).
4. **Given** a curve, **When** a point would make the clip shorter than the minimum clip length or longer than 10 minutes, **Then** the move is limited to the allowed range with a short message.
5. **Given** any point, **When** it is dragged, **Then** it cannot cross a neighbour and its speed stays between 0.1x and 10x.
6. **Given** a point, **When** it is edited with the on-screen buttons (nudge left, right, faster, slower, delete), **Then** the same result is reached without dragging.

---

### User Story 3 - Kept through edits, saved, exported (Priority: P3)

The curve stays right when the clip is trimmed, split, duplicated, relinked, reversed, or has a freeze made in it; works with keyframes, transform, transitions, and the audio tools; is saved with the project; and is exported exactly as in the preview. The tools work in the unfolded and tabletop layouts.

**Why this priority**: Persistence, edit rules, and combination with earlier features apply.

**Independent Test**: Give a clip a curve, split it, trim the second part, reverse it, add a keyframe push-in and a transition, save and reopen, unfold, export, and compare the length and frames.

**Acceptance Scenarios**:

1. **Given** a clip with a curve, **When** it is split at the playhead, **Then** each part keeps the part of the curve in its part of the clip and plays at the same speed at every moment as before.
2. **Given** a clip with a curve, **When** it is trimmed, **Then** the curve outside the new range is removed and the speed at each new edge is kept; the clip's length follows.
3. **Given** a clip with a curve, **When** it is duplicated, relinked, or its project is duplicated, **Then** the curve is kept.
4. **Given** a clip with a curve, **When** it is reversed, **Then** the clip plays backwards with the speed following the curve along the played clip.
5. **Given** a clip with a curve, keyframes, a transform, a filter, an effect, or a transition, **When** the project plays, **Then** all work together; keyframe times follow the clip's new timeline length.
6. **Given** a freeze made in a clip with a curve, **When** it is inserted, **Then** the still shows the frame at the playhead and the parts keep their curves.
7. **Given** an exported video, **When** its length and a frame at several times are compared with the preview, **Then** they match within a small difference.

---

### Edge Cases

- The constant Speed control is dimmed while a curve is on; Reset restores it at 1x.
- A curve over a very short clip (under 1 second) still allows the presets; the minimum clip length limits the extremes.
- Slowing a clip to 0.1x stretches 1 second into 10; frames are shown for longer, not invented.
- Speeding to 10x skips frames evenly; sound at high speed is allowed but may sound thin.
- A curve cannot be put on a photo or a freeze still; the tool is unavailable with the reason "Photos have no speed".
- Changing a clip's length through a curve changes the start of everything after it on the timeline, as any speed change does; transitions are limited to half the new shorter neighbour length.
- Overlay, text, and audio-lane clips do not move with the clip; the user keeps control of them as with a constant speed change.
- Keyframes keep their times relative to the clip's start on the timeline and are limited to the new length.
- Missing media: the tool is unavailable with a short reason.
- Reduced preview quality does not change the speed.
- Undo and redo restore the exact earlier curve.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A main-track video clip MUST be able to hold a speed curve of 2 to 12 points, each at a moment of the clip and a speed from 0.1x to 10x, with the speed changing smoothly between points.
- **FR-002**: The clip's timeline length MUST be the time the trimmed source takes at the curve's speed, and everything after it MUST move accordingly.
- **FR-003**: Six presets (Montage, Hero time, Bullet time, Jump, Flash in, Flash out) MUST each set a curve in one step; Reset MUST remove the curve and restore constant speed 1x in one step.
- **FR-004**: Users MUST be able to add, move (time and speed), nudge, and remove points; the two end points cannot be removed, points cannot cross, and every change MUST be one undo step.
- **FR-005**: A change that would make the clip shorter than the minimum clip length or longer than 10 minutes MUST be limited to the allowed range with a short message.
- **FR-006**: The graph MUST show the clip's resulting length while a point is dragged.
- **FR-007**: The picture and the attached sound MUST follow the curve; the sound MUST keep its pitch.
- **FR-008**: The preview MUST play the curve; export MUST produce the same length and the same frames at the same times within a small difference.
- **FR-009**: Split, trim, duplicate, move, delete, relink, project duplicate, reverse, freeze, and undo/redo MUST keep the curve consistent as in User Story 3.
- **FR-010**: The curve MUST work with the clip's keyframes, transform, filter, effect, transitions, volume, fade, and audio tools.
- **FR-011**: The curve MUST be saved with the project and survive close, reopen, and process death.
- **FR-012**: The constant Speed control MUST be unavailable while a curve is on, with the reason; the tool MUST be unavailable for photos and missing media.
- **FR-013**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels, including the graph and its points.

### Key Entities *(include if feature involves data)*

- **Speed curve**: An ordered list of points on a clip, each with a moment in the clip's source time and a speed; replaces the clip's constant speed.
- **Curve preset**: A named shape of a speed curve scaled to the clip's length.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can apply a preset and see the result in under 10 seconds from selecting the clip.
- **SC-002**: The clip's length shown in the editor and the length of the exported video differ by less than 100 ms for every preset.
- **SC-003**: The exported frame at any moment shows the source frame the curve says (the nearest source frame, within one frame) in an automated check.
- **SC-004**: The sound of a ramped clip keeps its pitch: the dominant frequency of a steady tone changes by less than 3% across a ramp.
- **SC-005**: 100% of curves survive close and reopen, split, trim, duplicate, reverse, undo, and redo in automated tests, with the speed at every tested moment unchanged by a split or trim.
- **SC-006**: Dragging a point keeps the graph and readout responsive: the readout updates within 100 ms of each move.
- **SC-007**: Every curve control is reachable in 3 taps or fewer from a selected clip.

## Assumptions

- Presets are defined as shapes over the clip's length and scaled to the clip: Montage (fast, slow, fast), Hero time (fast, slow in the middle, fast), Bullet time (very slow in the middle), Jump (normal then a sudden slow-down), Flash in (very slow start then normal), Flash out (normal then a very slow end).
- The preview and export play the curve in short constant-speed steps (about 100 ms) that approximate the smooth curve, so a ramp is smooth to the eye and ear.
- Slow motion repeats nearest frames; no frame blending in this feature.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
