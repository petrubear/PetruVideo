# Feature Specification: Effects and Transitions Pack

**Feature Branch**: `035-effects-transitions-pack`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "Let's add more effects and transitions, but we don't need hundreds, just the top ten most popular and useful."

## Scope note

The editor has 9 video effects (Blur, Pixelate, Mirror, Shake, Glitch, RGB split, Glow, VHS, Grain) and 5 transitions (Dissolve, Fade to black, Slide, Cover, Zoom). This feature adds ten new effects and ten new transitions, chosen because they are the ones people reach for most in short videos. They sit in the same places as the existing ones, work the same way, are seen live in the preview, are saved with the project and are rendered by export exactly as seen. Nothing that exists today is changed, renamed, reordered or removed: the existing nine effects and five transitions look and behave exactly as before, and the new ones are added after them. Transitions that cut the picture into shapes or change its pixels (wipes, iris, glitch, pixelate, swirl, blur dissolve, white flash) are not possible with the current transition engine and are not included. Not included: particle effects such as rain or snow, effects that need other frames of the clip (trails, echoes), 3D transitions (cube, page curl), effect or transition marketplaces, and anything downloaded.

## Clarifications

### Session 2026-11-05

- Q: How many new items? → A: Ten new effects and ten new transitions; the existing nine effects and five transitions stay exactly as they are.
- Q: Keep the proposed effects or choose again by popularity? → A: Choose again: the ten most popular effects the app is missing. Kaleidoscope is replaced by Cartoon.
- Note: the transition list was reworked after checking the transition engine (see Assumptions).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Use a new effect on a clip (Priority: P1)

A user selects a clip, opens Effects, sees the ten new effects beside the nine they know, picks "Zoom pulse" and sees it in the preview. They move the strength control and see it change.

**Why this priority**: The effects are the main value of the feature.

**Independent Test**: Open Effects on a clip, apply each new effect in turn, and check the preview shows a clearly visible change that grows with strength and disappears at 0.

**Acceptance Scenarios**:

1. **Given** a clip is selected, **When** the user opens Effects, **Then** the list shows the existing nine effects and the ten new ones, each with a name.
2. **Given** a new effect is applied, **When** the user changes its strength from 0 to 100, **Then** the preview changes smoothly within 100 ms of the drag, with no visible change at 0.
3. **Given** an effect is applied, **When** the user removes it, **Then** the clip looks as before.
4. **Given** a clip has an existing effect, **When** the user adds a new one, **Then** both apply together in the same order in preview and export, as stacked effects already do.

---

### User Story 2 - Use a new transition between two clips (Priority: P1)

A user selects the joint between two clips, opens Transitions, picks "Whip pan" and a length. The preview keeps cutting between the clips, as it does for every transition today; the export shows the transition.

**Why this priority**: Transitions are the other half of the request.

**Independent Test**: Apply each new transition to a joint between two clips at 0.5 s and export; check in the exported video that it starts on the first clip, ends on the second and has no jump at either end.

**Acceptance Scenarios**:

1. **Given** a joint between two clips, **When** the user opens Transitions, **Then** the list shows the existing five and the ten new ones.
2. **Given** a transition is chosen, **When** the user sets its length within the allowed range, **Then** the joint shows it as selected with that length, and the export plays it over that length.
3. **Given** a transition longer than the clips allow, **When** it is set, **Then** it is limited with the existing short message, as today.
4. **Given** a transition is removed, **When** the user plays across the joint, **Then** the cut is plain again.

---

### User Story 3 - What you see is what you export (Priority: P1)

The exported video shows the same effects and transitions as the preview.

**Why this priority**: A look that changes on export would be a bug.

**Independent Test**: Export a project that uses several new effects and transitions and compare it with the preview frame by frame at the effect and joint times.

**Acceptance Scenarios**:

1. **Given** a project with new effects and transitions, **When** it is exported, **Then** the file shows the same looks at the same times.
2. **Given** the project is saved and reopened, **When** the user opens the clips and joints, **Then** the same effects, strengths, transitions and lengths are shown.

---

### Edge Cases

- A project made before this feature opens and exports with no change.
- A project saved with a new effect and opened on a build that does not know it: the effect is ignored and the clip plays unchanged, not a crash.
- Photos and overlay clips can use the new effects like the existing ones; transitions apply only between clips on the main video lane, as today.
- Very short clips: a transition is limited to what the two clips allow.
- Effects that change brightness quickly (Strobe, Flash) show a short notice each time they are applied, because rapid flashing can be uncomfortable; they never exceed three flashes per second at full strength.
- On a device that cannot keep up, the preview lowers its quality and says so, as for existing effects; export is never lowered.
- Names and descriptions appear in English and Spanish.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Effects list MUST offer these ten new effects, each with a strength from 0 to 100 and no change at 0:
  1. **Zoom pulse**: the picture swells and settles back in a repeating beat.
  2. **Flash**: short bright white flashes that fade out.
  3. **Light leak**: warm colored light washes in and out over the picture.
  4. **Letterbox**: black bars at top and bottom for a cinema look; strength sets bar height.
  5. **Zoom blur**: a radial blur that streaks outward from the center.
  6. **Fisheye**: a bulging wide-lens distortion.
  7. **Ripple**: a water-like wave moving through the picture.
  8. **Strobe**: the picture blinks on and off at a steady pace.
  9. **Old film**: warm faded color, flicker and light vertical scratches.
  10. **Cartoon**: flat, poster-like colors with dark outlines, like a drawn comic.
- **FR-002**: The Transitions list MUST offer these ten new transitions, each with the existing length control and limits. Every one moves, turns, resizes and fades the two clips as whole pictures, because that is what the app's transition engine can do:
  1. **Spin**: the first clip turns and shrinks away while the second turns in from the other side.
  2. **Flip**: the first clip narrows to an edge and the second opens out, like a card turning over.
  3. **Push up**: the second clip pushes the first one out of the top of the frame.
  4. **Push down**: the second clip pushes the first one out of the bottom of the frame.
  5. **Push right**: the second clip pushes the first one out to the right (the existing Slide goes left).
  6. **Zoom out**: the first clip shrinks and fades while the second grows into place.
  7. **Whip pan**: a very fast sideways slide that starts and ends sharply.
  8. **Drop**: the first clip falls out of the bottom while tilting, over the second.
  9. **Dip zoom**: the first clip zooms in and fades to black, then the second zooms out of black.
  10. **Tilt slide**: both clips slide sideways while leaning, like a swinging door.
- **FR-000**: The existing nine effects and five transitions MUST NOT change: same names, same order at the start of each list, same look at every strength and length, same saved values. Projects that use them MUST preview and export identically before and after this feature.
- **FR-003**: The new effects MUST work exactly like the existing ones: apply to video, photo and overlay clips, stack with other effects, appear in the same lists, and be removed the same way.
- **FR-004**: The new transitions MUST work exactly like the existing ones: set and removed at a joint, with the same length range, step and limits, kept through trim, split and reorder as today.
- **FR-005**: Preview and export MUST show the same look for every new effect, and each MUST be visible to the eye at strength 50. New transitions MUST behave like the existing five: the export shows the transition at the joint, and the preview shows a hard cut at the same moment, as it does today.
- **FR-006**: All new effects and transitions MUST be saved with the project and shown again on reopen; projects made before this feature MUST open and export unchanged.
- **FR-007**: A project that names an effect or transition this build does not know MUST open, ignore it and keep everything else.
- **FR-008**: Changing an effect strength MUST be seen in the preview within 100 ms without pausing playback; on a device that cannot keep up, the preview lowers its quality and says so.
- **FR-009**: Strobe and Flash MUST NOT flash more than three times per second at any strength, and applying either MUST show a short notice about flashing lights.
- **FR-010**: Every new name and notice MUST exist in English and Spanish and follow the phone's language.
- **FR-011**: Each new item MUST be reachable with a screen reader (name announced, selected state announced) and have touch targets of at least 48 dp.
- **FR-012**: Each change of effect, strength, transition or length MUST be one undo step, as for the existing ones.

### Key Entities

- **Effect kind**: gains ten kinds beside the existing nine; a clip's effect still has a kind and a strength from 0 to 100.
- **Transition kind**: gains ten kinds beside the existing five; a transition still has a kind and a length.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can apply any new effect to a clip in three taps or fewer from the clip's Effects tool.
- **SC-002**: A user can apply any new transition to a joint in three taps or fewer.
- **SC-003**: Each of the 20 new items is visibly different from an unmodified clip when judged by reviewers on a still frame at strength 50 (effects) or at the midpoint of the joint in the exported video (transitions).
- **SC-004**: On a recent phone, playing a 1080p clip with any one new effect or transition keeps the preview at its normal frame rate without lowering quality.
- **SC-005**: For every new effect, a frame of the export matches the preview at the same time with no visible difference; for every new transition, the exported joint starts exactly on the last picture of the first clip and ends exactly on the first picture of the second.
- **SC-006**: Existing projects open with no visible change, and every existing effect and transition renders pixel-for-pixel as before (100% of the existing screenshot tests still pass without re-recording).
- **SC-007**: All new names and notices read correctly in English and Spanish.

## Assumptions

- The ten effects are the most popular ones the app is missing, chosen for popularity in short-form video and for working well on phones, and the ten transitions likewise. Swapping an item changes nothing else in the spec.
- The new items use the same lists, controls and limits as the existing ones, so no new screen is needed beyond longer lists.
- Strength for Letterbox maps to bar height; for Zoom pulse, Strobe and Flash it maps to how strong the swell or flash is, with the pace fixed.
- Transitions stay on the main video lane as today; overlay clips keep their own fade-in and fade-out.
- The transition list was changed from the first draft after checking the engine: wipe, iris, glitch, pixelate, swirl, blur dissolve and white flash cannot be drawn with it. Replacing the engine so they can would be a separate feature.
