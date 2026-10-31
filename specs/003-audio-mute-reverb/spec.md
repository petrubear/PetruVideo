# Feature Specification: Audio Tools - Mute and Reverb

**Feature Branch**: `003-audio-mute-reverb`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "lets add audio editing tools, let's start with mute, volume, and the first effect: reverb, add control of the reverb configuration options too"

## Scope note

Volume exists since feature 002, and Mute exists on audio clips since feature 001. This feature brings the audio tools together in one place, extends Mute to the sound of video clips, and adds the first audio effect, Reverb, with adjustable settings.

## Clarifications

### Session 2026-10-31

- Q: Where does the reverb tail end when a clip ends? → A: Cut at the clip end; it never plays over the next clip.
- Q: How does the phone reverb panel show its settings? → A: Presets plus Mix and Decay time visible; a "More" section reveals Room size, Damping, and Pre-delay.
- Q: Can users save their own reverb presets? → A: No; built-in presets only, plus "Custom" for the clip's current values.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add reverb to a clip (Priority: P1)

A user selects a clip that has sound (a video with its sound attached, or an audio clip), taps Reverb, and turns it on. They hear the clip with a room-like echo right away, pick a preset such as Small room, Hall, or Cave, and can undo the change.

**Why this priority**: Reverb is the headline of this feature and the first effect in the product's audio toolkit. A preset-only version already delivers it.

**Independent Test**: Add a voice clip, turn Reverb on with the Hall preset, play it and hear the echo tail, turn it off and hear the dry clip again, then undo and redo each step.

**Acceptance Scenarios**:

1. **Given** a selected clip with sound, **When** the user taps Reverb, **Then** a reverb panel opens with an on/off switch, the preset list, and the settings, with the effect off by default.
2. **Given** the panel is open and Reverb is off, **When** the user turns it on, **Then** the clip is heard with the default preset right away, during playback and while paused (the tail is heard on the next play).
3. **Given** Reverb is on, **When** the user picks a different preset, **Then** the sound and all settings change to that preset's values, and the preset is shown as selected.
4. **Given** Reverb is on, **When** the user turns it off, **Then** the clip plays dry, and turning it on again restores the last settings.
5. **Given** a clip without sound (a photo, or a video whose sound was detached), **When** it is selected, **Then** Reverb is dimmed with a short explanation.

---

### User Story 2 - Tune the reverb settings (Priority: P1)

A user opens the reverb panel and adjusts the settings to shape the effect: how big the space is, how long the tail lasts, how bright or dull it sounds, and how much reverb is mixed with the original sound. Changes are heard as they drag.

**Why this priority**: The user asked explicitly for control of the reverb options, and presets alone do not let them match the effect to their clip.

**Independent Test**: With Reverb on, drag Room size, Decay, Damping, and Mix and hear each change while playing; drag Mix to 0% and hear the dry clip; save and reopen the project and find every value as set.

**Acceptance Scenarios**:

1. **Given** Reverb is on, **When** the user opens the settings, **Then** they see Room size, Decay time, Damping, Pre-delay, and Mix, each with its current value shown as a number.
2. **Given** playback is running, **When** the user drags a setting, **Then** the change is heard within 100 ms and the clip keeps playing without a gap.
3. **Given** the user changed a setting after choosing a preset, **When** the values no longer match the preset, **Then** the preset shows as "Custom".
4. **Given** a setting was changed, **When** the user taps Reset, **Then** all settings return to the selected preset's values (or the default preset when Custom).
5. **Given** a drag on a setting, **When** the user releases, **Then** it counts as one edit and one undo step restores the previous value.

---

### User Story 3 - Mute a clip's sound (Priority: P2)

A user selects a video clip with sound and taps Mute to silence its sound without changing its volume or losing it, and taps Unmute to bring it back. Audio clips keep their existing Mute.

**Why this priority**: Silencing a video's original sound is common, but Detach audio and Volume 0% already allow it, so this is a convenience.

**Independent Test**: Select a video with sound, tap Mute and hear silence from that clip while music over it keeps playing, tap Unmute and hear the sound at its earlier volume, and undo each step.

**Acceptance Scenarios**:

1. **Given** a selected video clip with attached sound, **When** the clip tool bar shows, **Then** it offers Mute (labeled Unmute while muted).
2. **Given** a muted video clip, **When** the timeline plays, **Then** that clip is silent, other clips and audio lanes are unaffected, and the clip shows a muted indicator.
3. **Given** a clip with Volume 60% and Reverb on, **When** it is muted and then unmuted, **Then** volume and reverb settings are unchanged.
4. **Given** a muted video clip, **When** the user detaches its audio, **Then** the new audio clip is muted too, so the result sounds the same.
5. **Given** a video clip without sound, **When** it is selected, **Then** Mute is dimmed.

---

### User Story 4 - One place for the audio tools (Priority: P3)

The clip tool bar lists Volume, Mute, and Reverb together as the audio tools, with badges on the timeline for clips that have reverb, and the same controls in the inspector on the unfolded editor and below the hinge in tabletop.

**Why this priority**: It follows the existing layout rules and visual language but builds on the working tools.

**Independent Test**: Turn on reverb for a clip, see its badge on the timeline, unfold the device and change a reverb setting from the inspector, then half-fold and open the panel below the hinge.

**Acceptance Scenarios**:

1. **Given** a clip with Reverb on, **When** it appears on the timeline, **Then** it shows a small "Rv" badge (together with any speed or volume badge).
2. **Given** the unfolded editor with a clip selected, **When** the inspector shows, **Then** it hosts Volume, Mute, and the Reverb controls, changing the same values as the phone panel.
3. **Given** tabletop posture, **When** the reverb panel opens, **Then** it sits on the controls side of the hinge and nothing interactive is under the hinge.
4. **Given** the reverb panel is open, **When** the device is folded, unfolded, or rotated, **Then** the panel stays open with the same values.

---

### Edge Cases

- A clip is shorter than the reverb tail: the tail is cut at the clip's end and does not play over the next clip.
- Reverb on a muted clip: no sound, and the settings are kept.
- Reverb on a clip at 0% volume: silent (volume applies after reverb, so the tail is silent too), settings kept.
- Reverb on a sped-up or slowed clip: the effect follows the clip and sounds natural at the new speed.
- Very high Mix with high Decay could be loud: the output level is kept from clipping harshly, as it is for a volume boost.
- Split a clip with reverb: both halves keep the same settings; the tail of the first half ends at the cut.
- Duplicate, detach audio, and replace audio keep the reverb settings.
- Missing source media: the settings stay editable and are kept.
- A project is saved and reopened, or the app is force-closed after a change: mute and reverb come back exactly as set, with at most 5 seconds of work lost.
- Many clips with reverb at once on a mid-range device: preview stays at 30 fps and audio does not stutter, or the app lowers preview quality and says so.

## Requirements *(mandatory)*

### Functional Requirements

**Reverb**

- **FR-001**: Users MUST be able to turn Reverb on or off for any video clip with attached sound and any audio clip, off by default.
- **FR-002**: Reverb MUST offer presets: Small room, Medium room, Hall, Cave, and Plate, each setting all the reverb settings at once.
- **FR-003**: Reverb MUST offer these settings: Room size (0% to 100%), Decay time (0.2 s to 10 s), Damping (0% to 100%, how much high frequency the tail loses), Pre-delay (0 ms to 200 ms), and Mix (0% to 100%, reverb against the original sound).
- **FR-004**: Each setting MUST show its current value as a number with its unit, and changes MUST be heard in the preview within 100 ms while the user drags, with no gap in playback; Room size is the exception and applies when the user releases it.
- **FR-004a**: On the phone panel, Mix and Decay time MUST be visible next to the presets, and Room size, Damping, and Pre-delay MUST be one tap away under "More"; the inspector shows all five.
- **FR-005**: Presets are built in and cannot be added, edited, or deleted. Picking a preset MUST set all settings; changing a setting afterwards MUST show the preset as "Custom"; Reset MUST return to the selected preset (or the default preset when Custom).
- **FR-006**: Turning Reverb off MUST keep the settings so that turning it on again restores them.
- **FR-007**: Reverb applies to one clip only and MUST NOT affect other clips, including the attached sound of a video against music over it.
- **FR-008**: The reverb tail MUST be cut at the end of the clip and MUST NOT sound over the next clip or after the clip.
- **FR-009**: Reverb MUST be unavailable (dimmed with a short reason) for photos, video clips without sound, and video clips whose sound is detached.

**Mute and volume**

- **FR-010**: Users MUST be able to mute and unmute the attached sound of a video clip as well as an audio clip; Mute MUST be independent of Volume and Reverb and MUST NOT change them.
- **FR-011**: A muted clip MUST show a muted indicator on the timeline, and detaching a muted video clip's audio MUST give a muted audio clip.
- **FR-012**: Volume (from feature 002) MUST remain available with its current behavior and appear with Mute and Reverb as one group of audio tools.

**Editing integration**

- **FR-013**: Split, duplicate, detach audio, replace audio, and trim MUST keep mute and reverb settings (both halves of a split get the same settings).
- **FR-014**: Every change MUST be undoable and redoable as one step per gesture.
- **FR-015**: Mute and reverb settings MUST be saved with the project, restored on reopening, and covered by the same autosave guarantee as other edits.
- **FR-016**: Source media files MUST NOT be modified by these tools.
- **FR-017**: Export of reverb belongs to the export feature; this feature records the settings the export will use and MUST make the preview and saved project correct.

**Design and layout**

- **FR-018**: The reverb panel and audio tools MUST follow the existing visual language, work in compact, medium, expanded (inspector from 840 dp) and tabletop arrangements, keep an open panel through fold, unfold and rotation, and keep nothing interactive under the hinge.
- **FR-019**: Controls MUST have accessible labels, touch targets of at least 48 dp, scalable text, and values readable by a screen reader.
- **FR-020**: A clip with Reverb on MUST show a compact "Rv" badge on the timeline.

### Key Entities

- **Clip mute**: an on/off state on a clip's sound; separate from volume.
- **Reverb**: an optional effect on a clip's sound with an on/off state, a preset name (or Custom), and five settings: room size, decay time, damping, pre-delay, mix.
- **Reverb preset**: a named set of the five settings.
- **Clip** (video or audio, from 001 and 002): gains a mute state (video clips) and a reverb.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can turn on a reverb preset and hear it within 15 seconds of selecting a clip, on their first attempt.
- **SC-002**: A setting change (except Room size, which applies on release) is heard within 100 ms of the drag and playback never pauses for it.
- **SC-003**: With reverb on up to 5 clips in a project, preview holds 30 frames per second at 1080p on a mid-range device and audio plays without dropouts.
- **SC-004**: 100% of mute, reverb, and setting changes can be undone and redone back to the identical project.
- **SC-005**: After force-closing the app right after a change, 100% of projects reopen with the same mute and reverb settings (at most 5 seconds of work lost).
- **SC-006**: With Mix at 0% or Reverb off, the clip's sound is indistinguishable from the original and the reverb tail never extends past the clip end in 100% of tested cases.
- **SC-007**: On each affected screen and arrangement (compact, expanded with inspector, tabletop), a design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.
- **SC-008**: Source media files are byte-identical before and after these edits in 100% of tested cases.

## Assumptions

- Mute and Volume already exist for audio clips (001) and volume for all clips with sound (002); this feature adds Mute for video clips' attached sound and groups the tools.
- The designer's reference has no Reverb panel; the controls use the existing visual language and are reviewed with the designer, with deviations recorded.
- Five presets and five settings are a starting set; the exact preset values are tuned by ear during implementation.
- Reverb is per clip and fixed over the clip; changing settings over time (automation, keyframes) and other effects (echo, equalizer, pitch, noise reduction) belong to later features.
- Reverb has no separate left and right controls; output is stereo or mono as the clip is.
- Export of audio effects belongs to the export feature.
- Feature 001's and 002's engine, undo, autosave, clip tool bar, inspector, and fold layouts are in place and reused.
