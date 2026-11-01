# Feature Specification: Keyframes

**Feature Branch**: `012-keyframes`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Keyframes: animate a clip's position, scale, and opacity over time. On a selected overlay or text clip the user adds keyframes at the playhead, changes values at each keyframe by dragging on the preview or with sliders, and the clip moves smoothly between keyframes with a choice of linear or ease. Keyframes show as diamonds on the clip in the timeline, can be moved, copied, and deleted, are live in the preview, saved with the project, kept by split and duplicate, and rendered by export exactly as in the preview."

## Scope note

Overlay clips and text clips can change their place, size, and opacity over time. A keyframe stores those three values at one moment of the clip; between two keyframes the values change smoothly. Not included: keyframes on video-lane clips, audio volume keyframes, rotation, curves beyond linear and ease, and copying keyframes between clips.

## Clarifications

### Session 2026-11-01

- Q: Which values can be animated? → A: Place (x and y together), size, and opacity. Text size is its size. Each keyframe holds all three.
- Q: How does a clip with keyframes relate to its plain values? → A: With no keyframes the clip uses its plain values as today. With one or more keyframes the keyframes drive the clip; before the first keyframe and after the last, the nearest keyframe's values hold.
- Q: Which motion choices exist? → A: Linear and Ease, set per keyframe for the stretch that follows it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Animate an overlay or text clip (Priority: P1)

A user selects an overlay or text clip, moves the playhead inside it, and taps Add keyframe. A keyframe is stored at that moment with the clip's current place, size, and opacity. They move the playhead later, change the place or size on the preview or opacity with the slider, and a second keyframe is made from the change. Playing shows the clip move and fade smoothly between the two.

**Why this priority**: This is the core of the feature.

**Independent Test**: Add an overlay, keyframe it at its start in the upper right, keyframe it 2 s later in the lower left at half opacity, play across it and see it travel; export and compare frames.

**Acceptance Scenarios**:

1. **Given** a selected clip with no keyframes, **When** the user taps Add keyframe at the playhead, **Then** one keyframe appears at that time holding the clip's current values and the clip looks the same.
2. **Given** a clip with keyframes, **When** the user changes place, size, or opacity at a playhead that sits on a keyframe, **Then** that keyframe's values change.
3. **Given** a clip with keyframes, **When** the user changes a value with the playhead between keyframes, **Then** a keyframe is made at the playhead holding the shown values with the change applied.
4. **Given** two keyframes, **When** the project plays or the playhead moves between them, **Then** the clip's place, size, and opacity are the in-between values for that moment.
5. **Given** the playhead before the first or after the last keyframe, **When** the preview shows the clip, **Then** it shows the first or last keyframe's values.
6. **Given** one gesture (a drag, a pinch, or a slider release), **When** it ends, **Then** it is one undo step.

---

### User Story 2 - Edit keyframes on the timeline (Priority: P2)

A user sees each keyframe as a diamond on the selected clip in the timeline. They tap a diamond to select it and the playhead jumps to it, drag it to a new time inside the clip, duplicate it to the playhead, or delete it. Each keyframe has a Linear or Ease choice for the stretch after it.

**Why this priority**: Changing timing and feel is needed to make animations good, but a basic animation already works without it.

**Independent Test**: Make three keyframes, move the middle one later, set it to Ease, delete the last one, undo each step.

**Acceptance Scenarios**:

1. **Given** a clip with keyframes, **When** it is selected, **Then** a diamond shows at each keyframe's time and the diamonds are at least 48 dp to tap.
2. **Given** a selected keyframe, **When** the user drags it, **Then** its time follows inside the clip, it cannot pass a neighbour keyframe, and it snaps to the playhead within 8 dp.
3. **Given** a selected keyframe, **When** the user taps Copy here, **Then** a copy with the same values is made at the playhead; if a keyframe is already there it is replaced.
4. **Given** a selected keyframe, **When** the user taps Delete, **Then** it is removed; with the last one removed the clip goes back to its plain values.
5. **Given** a selected keyframe, **When** the user picks Linear or Ease, **Then** the motion in the stretch after it changes and the preview shows it.

---

### User Story 3 - Kept, saved, and exported (Priority: P3)

Keyframes stay right when the clip is split, duplicated, moved, or trimmed, are saved with the project, and appear in an export exactly as in the preview. The tools work in the unfolded and tabletop layouts.

**Why this priority**: Persistence, timeline operations, and export rules from earlier features apply.

**Independent Test**: Animate a text clip, split it in the middle, trim, save and reopen, unfold and half-fold, export and compare a frame at the middle with the preview.

**Acceptance Scenarios**:

1. **Given** a clip with keyframes, **When** it is split at the playhead, **Then** both parts keep the values of the animation at their own times: the first part keeps the keyframes before the cut plus one at its end holding the value at the cut, and the second keeps those after plus one at its start.
2. **Given** a clip with keyframes, **When** it is duplicated or moved on the timeline, **Then** the keyframes keep their times relative to the clip.
3. **Given** a clip with keyframes, **When** it is trimmed, **Then** keyframes outside the new range are removed and the value at each new edge is kept by a keyframe there.
4. **Given** a project with keyframes, **When** it is closed and reopened, **Then** they are unchanged.
5. **Given** an exported video, **When** a frame inside an animation is compared with the preview at the same time, **Then** place, size, and opacity match within a small visual difference.

---

### Edge Cases

- A keyframe time is relative to the clip's start, so moving the clip keeps the animation.
- Two keyframes cannot share a time; a new one at an occupied time changes that keyframe.
- A clip with an entrance or exit animation on text (fade, slide, pop) combines them: the keyframes set the base values and the entrance and exit animation run on top.
- Keyframes closer than one frame apart are not allowed.
- A very short clip (under 0.5 s) still allows one keyframe and two at the ends.
- Reduced preview quality does not affect keyframes; the animation plays the same.
- Text keyframe sizes stay within the text clip's size limits.
- Overlay place keeps the existing rule that the overlay stays inside the frame at every keyframe.
- Relink, remove-missing, and duplicate project keep keyframes with their clips.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Overlay and text clips MUST be able to hold an ordered list of keyframes, each with a time inside the clip, a place, a size, an opacity, and a motion choice (Linear or Ease).
- **FR-002**: Users MUST be able to add a keyframe at the playhead, holding the values the clip shows at that moment.
- **FR-003**: Changing place, size, or opacity of a clip that has keyframes MUST change the keyframe at the playhead, or create one there when none exists.
- **FR-003a**: Every existing way to change a keyframed clip's place, size, or opacity (preview drag and pinch, sliders, and the placement and opacity commands) MUST edit the keyframe at the playhead as FR-003 says, so no change is ever silently ignored.
- **FR-003b**: A clip MUST hold at most 50 keyframes; adding one past the limit is refused with a short message.
- **FR-004**: Between two keyframes the shown values MUST change continuously from the first to the second following the first's motion choice; outside the keyframes the nearest keyframe's values MUST hold.
- **FR-005**: A clip with no keyframes MUST behave as it does today.
- **FR-006**: The preview MUST show the animated values live while playing, scrubbing, and while the user drags.
- **FR-007**: Users MUST be able to select, move, copy to the playhead, and delete a keyframe, and set its motion choice; every change MUST be one undo step.
- **FR-008**: A keyframe MUST stay inside its clip, MUST NOT cross a neighbour, and no two keyframes MAY share a time.
- **FR-009**: The timeline MUST show a diamond for each keyframe on the selected clip, with touch targets of at least 48 dp.
- **FR-010**: Split, duplicate, move, trim, delete, relink, and project duplicate MUST keep keyframes consistent as in User Story 3.
- **FR-011**: Keyframes MUST be saved with the project and survive close, reopen, and process death.
- **FR-012**: Export MUST render the animated place, size, and opacity as the preview shows them.
- **FR-013**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Keyframe**: A moment inside an overlay or text clip with the clip's place, size, and opacity at that moment, and the motion choice for the stretch after it.
- **Animated clip**: An overlay or text clip with one or more keyframes; its shown values at any time come from its keyframes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can make a two-keyframe move-and-fade animation on an overlay in under 30 seconds.
- **SC-002**: The preview keeps its normal responsiveness with three animated clips on screen; the playhead never stalls longer than 0.5 s.
- **SC-003**: An exported frame inside an animation matches the preview frame at the same time within a small visual difference in an automated comparison.
- **SC-004**: 100% of keyframes survive close and reopen, split, duplicate, trim, undo, and redo in automated tests.
- **SC-005**: Playing a two-keyframe animation shows no sudden jump: the position changes by no more than a small share of the path from one frame to the next.
- **SC-006**: Every keyframe tool is reachable in 3 taps or fewer from a selected clip.

## Assumptions

- Keyframes apply only to overlay and text clips; video-lane clips are a later feature.
- Ease means a smooth start and end; no other curves in this feature.
- The values between keyframes are computed the same way in the preview and in export, from the clip's own timeline.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
