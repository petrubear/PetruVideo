# Feature Specification: Keyframes for Video Clips

**Feature Branch**: `014-video-keyframes`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Keyframes for video clips: the same keyframe animation that overlay and text clips have (position, size, opacity, with easing) becomes available on main-track video clips, so a clip can zoom in, pan, or fade over time. The Keyframes panel, diamonds on the timeline, and preview gestures work on a selected video clip like on overlays; keyframes are saved with the project, follow trims, splits and speed changes, play in the preview, and are exported with the video."

## Scope note

Video clips on the main track can change their zoom, pan, and opacity over time, with the same keyframes users already know from overlay and text clips. A video clip starts out filling the frame; a keyframe stores how zoomed in it is, which part of it is shown, and how opaque it is at one moment. Between two keyframes the values change smoothly. Not included: shrinking a video clip below the frame, rotation, curves beyond linear and ease, keyframes on audio clips, and copying keyframes between clips.

## Clarifications

### Session 2026-11-01

- Q: What do place, size, and opacity mean for a video clip? → A: Size is the zoom (100% fills the frame as today, up to 400%). Place is the pan: which part of the zoomed clip is in view; it cannot move the clip so far that the frame shows empty space. Opacity fades the clip toward the dark project background.
- Q: Do these keyframes replace the existing clip filter, effect, and transition? → A: No. Filter, effect, and transition into the next clip keep working; keyframes change the clip's framing and opacity on top of them.
- Q: Are keyframe times in clip time or source time? → A: Clip time on the timeline (after speed), so a speed change rescales keyframe times in proportion and the animation keeps its shape.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Zoom and pan a video clip (Priority: P1)

A user selects a video clip, moves the playhead inside it, and taps Add keyframe. A keyframe is stored with the clip's current zoom, pan, and opacity. They move the playhead later, pinch to zoom in and drag to pan on the preview, and a second keyframe is made from the change. Playing shows the clip slowly push in and drift between the two.

**Why this priority**: A slow push-in or pan on footage is the most requested use of keyframes and the core of the feature.

**Independent Test**: Import a clip, keyframe it at its start at full frame, keyframe it 3 s later at 200% zoom on one side, play across it and see the push-in; export and compare frames.

**Acceptance Scenarios**:

1. **Given** a selected video clip with no keyframes, **When** the user taps Add keyframe at the playhead, **Then** one keyframe appears at that time holding the clip's current values and the clip looks the same.
2. **Given** a video clip with keyframes, **When** the user pinches, drags, or moves the opacity slider with the playhead on a keyframe, **Then** that keyframe's values change.
3. **Given** a video clip with keyframes, **When** the user changes a value with the playhead between keyframes, **Then** a keyframe is made at the playhead holding the shown values with the change applied.
4. **Given** a video clip with no keyframes, **When** the user pinches or drags on the preview, **Then** a keyframe is made at the playhead with the change, and the clip keeps its full-frame look before that moment; from that moment on the new values hold, as with any last keyframe.
5. **Given** two keyframes, **When** the project plays or the playhead moves between them, **Then** zoom, pan, and opacity are the in-between values for that moment.
6. **Given** the playhead before the first or after the last keyframe, **When** the preview shows the clip, **Then** it shows the first or last keyframe's values.
7. **Given** any keyframe, **When** the pan or zoom would leave empty space in the frame, **Then** the values are limited so the clip always covers the frame.
8. **Given** one gesture (a drag, a pinch, or a slider release), **When** it ends, **Then** it is one undo step.

---

### User Story 2 - Edit video keyframes on the timeline (Priority: P2)

A user sees each keyframe as a diamond on the selected video clip in the timeline. They tap a diamond to select it and the playhead jumps to it, drag it to a new time inside the clip, copy it to the playhead, delete it, and set Linear or Ease for the stretch after it.

**Why this priority**: Timing and feel make the move look good, but a basic animation works without it.

**Independent Test**: Make three keyframes on a video clip, move the middle one later, set it to Ease, delete the last one, undo each step.

**Acceptance Scenarios**:

1. **Given** a video clip with keyframes, **When** it is selected, **Then** a diamond shows at each keyframe's time on the clip, at least 48 dp to tap, without hiding the clip's trim handles.
2. **Given** a selected keyframe, **When** the user drags it, **Then** its time follows inside the clip, it cannot pass a neighbour, and it snaps to the playhead within 8 dp.
3. **Given** a selected keyframe, **When** the user taps Copy here, **Then** a copy with the same values is made at the playhead; if a keyframe is already there it is replaced.
4. **Given** a selected keyframe, **When** the user taps Delete, **Then** it is removed; with the last one removed the clip goes back to full frame and full opacity.
5. **Given** a selected keyframe, **When** the user picks Linear or Ease, **Then** the motion in the stretch after it changes and the preview shows it.

---

### User Story 3 - Kept through edits, saved, and exported (Priority: P3)

Keyframes stay right when the video clip is split, duplicated, moved, trimmed, deleted, or has its speed changed, are saved with the project, and appear in an export exactly as in the preview, including across transitions. The tools work in the unfolded and tabletop layouts.

**Why this priority**: Persistence, timeline operations, and export rules from earlier features apply to this clip type.

**Independent Test**: Animate a video clip, split it in the middle, trim, change its speed, save and reopen, unfold and half-fold, export and compare a frame at the middle with the preview.

**Acceptance Scenarios**:

1. **Given** a video clip with keyframes, **When** it is split at the playhead, **Then** the first part keeps the keyframes before the cut plus one at its end holding the value at the cut, and the second keeps those after plus one at its start, so the movement continues without a jump.
2. **Given** a video clip with keyframes, **When** it is duplicated, **Then** the copy has the same keyframes at the same times relative to the clip.
3. **Given** a video clip with keyframes, **When** it is trimmed, **Then** keyframes outside the new range are removed and the value at each new edge is kept by a keyframe there.
4. **Given** a video clip with keyframes, **When** its speed changes, **Then** keyframe times scale with the clip's new length and the animation keeps its shape.
5. **Given** a video clip with keyframes, **When** clips before it are deleted, reordered, or the clip is moved in the lane, **Then** the keyframes stay with the clip.
6. **Given** a video clip with keyframes and a transition into the next clip, **When** the transition plays, **Then** the animated framing is used for the outgoing and incoming clips throughout.
7. **Given** a project with keyframes on video clips, **When** it is closed and reopened, **Then** they are unchanged.
8. **Given** an exported video, **When** a frame inside an animation is compared with the preview at the same time, **Then** zoom, pan, and opacity match within a small visual difference.

---

### Edge Cases

- A keyframe time is relative to the clip's start on the timeline, so moving the clip keeps the animation.
- Two keyframes cannot share a time; a new one at an occupied time changes that keyframe. Keyframes closer than one frame apart are not allowed.
- A very short clip (under 0.5 s) still allows one keyframe, and two at its ends.
- A clip with no keyframes keeps today's full-frame look and exports exactly as before.
- Opacity below 100% shows the dark project background (or the neighbouring clip during a transition); it does not reveal other lanes differently from overlays.
- Clips with different shapes (portrait inside a landscape project) are zoomed and panned relative to how they fit the frame today.
- Reduced preview quality does not affect keyframes; the animation plays the same.
- Relink, remove-missing, project duplicate, and templates keep keyframes with their clips.
- A frame-limit message appears when the 50 keyframe limit is reached.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Video clips MUST be able to hold an ordered list of keyframes, each with a time inside the clip, a zoom (100% to 400%), a pan, an opacity, and a motion choice (Linear or Ease).
- **FR-002**: Users MUST be able to add a keyframe at the playhead of a selected video clip, holding the values the clip shows at that moment.
- **FR-003**: Changing zoom, pan, or opacity of a video clip MUST change the keyframe at the playhead, or create one there when none exists, so no change is ever silently ignored. Creating the first keyframe MUST NOT change how the clip looks before the playhead.
- **FR-003a**: A video clip MUST hold at most 50 keyframes; adding one past the limit is refused with a short message.
- **FR-004**: Between two keyframes the shown values MUST change continuously following the first's motion choice; outside the keyframes the nearest keyframe's values MUST hold.
- **FR-005**: A video clip with no keyframes MUST look and export as it does today.
- **FR-006**: The shown pan and zoom MUST always keep the clip covering the whole frame; values that would not are limited automatically, in the keyframes, the preview, and the export.
- **FR-007**: The preview MUST show the animated values live while playing, scrubbing, and while the user drags.
- **FR-008**: Users MUST be able to select, move, copy to the playhead, and delete a keyframe, and set its motion choice; every change MUST be one undo step.
- **FR-009**: A keyframe MUST stay inside its clip, MUST NOT cross a neighbour, and no two keyframes MAY share a time.
- **FR-010**: The timeline MUST show a diamond for each keyframe on the selected video clip with touch targets of at least 48 dp, without blocking trimming or selecting the clip.
- **FR-011**: Split, duplicate, trim, speed change, move, delete, relink, templates, and project duplicate MUST keep keyframes consistent as in User Story 3.
- **FR-012**: Keyframes MUST work together with the clip's filter, effect, and transition into the next clip.
- **FR-013**: Keyframes MUST be saved with the project and survive close, reopen, and process death.
- **FR-014**: Export MUST render the animated zoom, pan, and opacity as the preview shows them.
- **FR-015**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Keyframe**: A moment inside a clip with the clip's zoom, pan, and opacity at that moment, and the motion choice for the stretch after it. Video clips now hold them as overlay and text clips do.
- **Animated video clip**: A video clip with one or more keyframes; its framing and opacity at any time come from its keyframes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can make a push-in on a video clip (two keyframes) in under 30 seconds.
- **SC-002**: The preview keeps its normal responsiveness with an animated clip across a transition; the playhead never stalls longer than 0.5 s.
- **SC-003**: An exported frame inside an animation matches the preview frame at the same time within a small visual difference in an automated comparison.
- **SC-004**: 100% of keyframes survive close and reopen, split, duplicate, trim, speed change, undo, and redo in automated tests.
- **SC-005**: Playing a two-keyframe push-in shows no sudden jump: between consecutive frames the zoom changes by no more than a small share of the full change.
- **SC-006**: In no tested case does the frame show empty space because of zoom or pan.
- **SC-007**: Every keyframe tool is reachable in 3 taps or fewer from a selected video clip.

## Assumptions

- Keyframes reuse the existing keyframe rules, limits, panel, and gestures from overlay and text clips; only the clip type and the meaning of the values are new.
- Pan and zoom are applied to the clip as framed today, before the clip's filter and effect are applied.
- Ease means a smooth start and end; no other curves in this feature.
- The values between keyframes are computed the same way in the preview and in export, from the clip's own timeline.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
