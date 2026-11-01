# Feature Specification: Audio Tools Part 2 - Fades, Noise Reduction, EQ, Voice Effects

**Feature Branch**: `005-audio-effects-part2`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Audio panel part 2: add fade in and fade out, noise reduction, equalizer (EQ), and voice effects (Echo, Robot, Pitch, Bass) for audio and video clips, building on the existing mute, volume and reverb. Controls with presets plus key settings, live in the preview, respected by export."

## Scope note

Mute, Volume and Reverb exist (features 001 to 003) and export already renders them (004). This feature adds five more audio tools to the same group: Fade, Noise reduction, Equalizer, and Voice effects (Echo, Robot, Pitch, Bass). Each applies to one clip, is heard live in the preview, is saved with the project, and is rendered by export exactly as heard.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fade a clip in and out (Priority: P1)

A user selects a clip with sound, opens Fade, and sets a fade-in length and a fade-out length so the sound rises smoothly at the start and falls smoothly at the end. They hear it right away and can undo.

**Why this priority**: Fades are the most common audio edit, are simple, and remove clicks at cuts.

**Independent Test**: Add a music clip, set a 2 s fade in and a 3 s fade out, play the start and the end and hear the ramps, set both to 0 and hear the original, undo and redo each step.

**Acceptance Scenarios**:

1. **Given** a selected clip with sound, **When** the user opens Fade, **Then** they see Fade in and Fade out, each from 0 s up to half the clip's length, default 0 s.
2. **Given** playback is running, **When** the user drags a fade length, **Then** the change is heard within 100 ms without a gap.
3. **Given** a clip with both fades, **When** the sum would exceed the clip length, **Then** the second fade is limited so the two never overlap, and a short message says so.
4. **Given** a clip with a fade, **When** it is trimmed, split, sped up or slowed down, **Then** fades stay at the clip's start and end and keep their length in timeline time, limited if the clip becomes too short.
5. **Given** a fade, **When** shown on the timeline, **Then** a small ramp marker appears at the affected end of the clip.

---

### User Story 2 - Clean up voice with noise reduction (Priority: P1)

A user selects a clip recorded with background hiss or hum, turns on Noise reduction, and chooses a strength. The background noise drops while the voice stays clear.

**Why this priority**: Noisy recordings are the main audio problem in phone footage.

**Independent Test**: Add a voice clip with steady background noise, turn Noise reduction on at Medium, hear less noise during playback, turn it off and hear the original, undo and redo.

**Acceptance Scenarios**:

1. **Given** a selected clip with sound, **When** the user opens Noise reduction, **Then** it shows an on/off switch and a strength of Low, Medium or High, off by default, default strength Medium.
2. **Given** it is on, **When** the user changes strength, **Then** the change is heard within 100 ms and playback never pauses.
3. **Given** a clip with steady noise, **When** Noise reduction is on at High, **Then** the noise level is clearly lower than the original and speech remains intelligible.
4. **Given** a clip with music only, **When** Noise reduction is on, **Then** the clip still plays without gaps or artifacts that make it unusable; the panel notes it is meant for voice.

---

### User Story 3 - Shape the tone with the equalizer (Priority: P2)

A user opens the Equalizer, picks a preset (Flat, Voice boost, Bass boost, Treble boost, Warm, Telephone), and can drag the band sliders (Low, Mid, High) to fine tune. Changes are heard live.

**Why this priority**: Tone control is a common need but more advanced than fades and noise cleanup.

**Independent Test**: Apply Bass boost, hear more low end, drag High down and hear it duller, tap Reset to return to the preset, undo and redo.

**Acceptance Scenarios**:

1. **Given** a selected clip with sound, **When** the user opens the Equalizer, **Then** they see the preset list and three bands (Low, Mid, High), each from -12 dB to +12 dB, off (Flat) by default.
2. **Given** the panel is open, **When** the user picks a preset, **Then** all bands change to the preset's values and the preset shows as selected.
3. **Given** a preset is selected, **When** the user changes a band, **Then** the preset shows as "Custom".
4. **Given** playback, **When** the user drags a band, **Then** the change is heard within 100 ms without a gap.
5. **Given** any combination of bands, **When** the clip plays, **Then** the level is kept from harsh clipping.

---

### User Story 4 - Voice effects (Priority: P2)

A user opens Voice effects and picks one effect for the clip: None, Echo, Robot, Pitch or Bass, adjusts its main setting, and hears it live.

**Why this priority**: These are the fun, creative effects people expect in a video editor, but they depend on the same panel and chain as the other tools.

**Independent Test**: Apply Pitch up on a voice clip and hear a higher voice at the same speed, switch to Robot and hear a robotic voice, set Echo to a long delay and hear repeats, return to None and hear the original.

**Acceptance Scenarios**:

1. **Given** a selected clip with sound, **When** the user opens Voice effects, **Then** they see None, Echo, Robot, Pitch and Bass, with None selected by default.
2. **Given** an effect is selected, **When** the panel shows, **Then** it offers that effect's main settings: Echo (delay and repeats), Robot (intensity), Pitch (semitones from -12 to +12, speed unchanged), Bass (amount).
3. **Given** playback, **When** the user drags an effect setting, **Then** the change is heard within 100 ms without a gap, except where documented.
4. **Given** the user picks another effect, **When** it applies, **Then** the previous effect is replaced, not stacked, and its settings are kept for when the user returns to it.
5. **Given** Pitch is used on a sped-up clip, **When** it plays, **Then** the pitch shift is measured against the natural pitch of the speed-adjusted sound, so +0 semitones leaves it as it is today.

---

### User Story 5 - One audio group, saved, exported and unfolded (Priority: P3)

The clip tool bar shows Volume, Mute, Fade, Noise reduction, Equalizer, Voice and Reverb together, with timeline badges, inspector sections on the unfolded editor and the panel below the hinge in tabletop. Everything is saved with the project and exported as heard.

**Why this priority**: Layout and persistence follow the existing rules and make the earlier stories complete.

**Independent Test**: Set every tool on one clip, save and reopen the project, export it, and compare the exported sound to the preview; unfold and use the inspector; half-fold and use the panel.

**Acceptance Scenarios**:

1. **Given** a clip with any new tool active, **When** it appears on the timeline, **Then** it shows a compact badge for that tool alongside speed, volume and reverb badges, without overflowing the clip.
2. **Given** the unfolded editor, **When** a clip is selected, **Then** the inspector hosts all audio tools and changes the same values as the phone panels.
3. **Given** tabletop posture, **When** a panel opens, **Then** it sits on the controls side of the hinge and nothing interactive is under the hinge.
4. **Given** an open panel, **When** the device is folded, unfolded or rotated, **Then** the panel stays open with the same values.
5. **Given** a project with all tools set, **When** it is exported, **Then** the file sounds like the preview (same fades, noise reduction, tone, effect, reverb and volume).

---

### Edge Cases

- Clips without sound (photos, video with detached sound): all new tools are dimmed with a short reason, as for Reverb.
- Muted clips: tools stay editable, no sound is heard, settings are kept.
- Fade at volume 0%: silent, settings kept.
- Split: the first half keeps the fade in and loses the fade out; the second half keeps the fade out and loses the fade in; other tools are copied to both halves.
- Duplicate, detach audio, replace audio: all settings are kept (replace keeps them as for Reverb and Volume).
- Echo repeats and Pitch tails end at the clip end and never sound over the next clip.
- Several tools at once: they combine in a fixed order, and the result stays free of harsh clipping.
- A project is saved and reopened, or the app is force-closed after a change: all settings come back exactly as set, with at most 5 seconds of work lost.
- Many clips with effects on a mid-range device: preview stays smooth, or the app lowers preview quality and says so; export is not affected.
- Missing source media: settings stay editable and are kept.

## Requirements *(mandatory)*

### Functional Requirements

**Fade**

- **FR-001**: Users MUST be able to set a fade-in length and a fade-out length on any video clip with attached sound and any audio clip, each from 0 s to half the clip's timeline length, default 0 s.
- **FR-002**: Fades MUST rise or fall smoothly with no audible click; the two fades MUST never overlap, and the second one set is limited with a short message.
- **FR-003**: Fades MUST stay anchored to the clip's start and end through trim, speed change, split and duplicate, and be limited if the clip becomes too short.

**Noise reduction**

- **FR-004**: Users MUST be able to turn Noise reduction on or off, off by default, with strength Low, Medium or High (default Medium).
- **FR-005**: Noise reduction MUST lower steady background noise noticeably at High without making speech unintelligible.

**Equalizer**

- **FR-006**: The Equalizer MUST offer presets Flat, Voice boost, Bass boost, Treble boost, Warm and Telephone, and three bands (Low, Mid, High) from -12 dB to +12 dB, with Flat as the default.
- **FR-007**: Picking a preset MUST set all bands; changing a band afterwards MUST show the preset as "Custom"; Reset MUST return to the selected preset (Flat when Custom).
- **FR-008**: The equalizer output MUST be kept from harsh clipping.

**Voice effects**

- **FR-009**: Users MUST be able to choose one voice effect per clip from None, Echo, Robot, Pitch and Bass, default None.
- **FR-010**: Each effect MUST expose its main setting(s): Echo (delay 50 ms to 1000 ms, repeats 1 to 5), Robot (intensity 0% to 100%), Pitch (-12 to +12 semitones, speed unchanged), Bass (amount 0% to 100%).
- **FR-011**: Choosing another effect MUST replace the current one and keep the other effects' settings for when they are chosen again.
- **FR-012**: Echo repeats and effect tails MUST be cut at the clip's end.

**Shared behavior**

- **FR-013**: For every tool, a setting change MUST be heard in the preview within 100 ms of the drag and playback MUST NOT pause for it; any setting that must apply on release is documented in the plan.
- **FR-014**: Each tool MUST show its current values as numbers with units, have a Reset, and work with accessible labels, touch targets of at least 48 dp and scalable text.
- **FR-015**: Tools MUST be unavailable (dimmed with a short reason) for clips without sound; they apply to one clip only and MUST NOT affect other clips.
- **FR-016**: Tools MUST combine in a fixed order that is the same in preview and export, with Volume and Fade last; the plan documents the order.
- **FR-017**: Every change MUST be undoable and redoable as one step per gesture.
- **FR-018**: All settings MUST be saved with the project, restored on reopening, covered by the same autosave guarantee, and kept by split (see Edge Cases), duplicate, detach, replace and trim.
- **FR-019**: Export MUST render all tools exactly as heard in the preview.
- **FR-020**: Source media files MUST NOT be modified by these tools.

**Design and layout**

- **FR-021**: The audio tools MUST follow the existing visual language and work in compact, medium, expanded (inspector from 840 dp) and tabletop arrangements, keep an open panel through fold, unfold and rotation, and keep nothing interactive under the hinge.
- **FR-022**: The tool bar MUST stay usable with seven audio tools: grouped under one Audio entry that opens a list of tools if they do not fit, each at least 48 dp wide.
- **FR-023**: A clip with any of these tools active MUST show compact badges on the timeline that never overflow the clip (extra badges collapse into a count).

### Key Entities

- **Fade**: fade-in length and fade-out length on a clip, in timeline time.
- **Noise reduction**: on/off state and a strength (Low, Medium, High).
- **Equalizer**: a preset name (or Custom) and three band gains.
- **Voice effect**: the chosen effect (None, Echo, Robot, Pitch, Bass) and a stored setting set per effect.
- **Clip** (from 001 to 003): gains fade, noise reduction, equalizer and voice effect alongside speed, volume, mute and reverb.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a fade, or turn on noise reduction, within 15 seconds of selecting a clip, on their first attempt.
- **SC-002**: A setting change is heard within 100 ms of the drag and playback never pauses for it, for every tool.
- **SC-003**: With up to 5 clips using several tools, preview holds 30 frames per second at 1080p on a mid-range device and audio plays without dropouts, or the app lowers preview quality and says so.
- **SC-004**: 100% of changes can be undone and redone back to the identical project.
- **SC-005**: After force-closing the app right after a change, 100% of projects reopen with the same settings (at most 5 seconds of work lost).
- **SC-006**: With every tool off or at its neutral value, the clip's sound is indistinguishable from the original, and no tail passes the clip end, in 100% of tested cases.
- **SC-007**: An exported file with all tools on matches the preview: fades start and end within 40 ms of the preview, and a listening check finds the same tone, noise level and effect in 100% of tested cases.
- **SC-008**: On each affected screen and arrangement, a design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.
- **SC-009**: Source media files are byte-identical before and after these edits in 100% of tested cases.

## Assumptions

- Mute, Volume and Reverb (001 to 003) and Export (004) are in place and reused, including undo, autosave, the clip tool bar, inspector, fold layouts and the shared preview/export builder.
- The designer's reference has no panels for these tools; they use the existing visual language and are reviewed with the designer, with deviations recorded.
- Settings are fixed over the clip; changing them over time (automation, keyframes) is a later feature.
- Only one voice effect is active per clip; stacking effects is out of scope.
- Preset values, noise reduction strengths and effect ranges are a starting set tuned by ear during implementation.
- Noise reduction is designed for steady noise (hiss, hum, fans), not for removing other voices or sudden sounds.
- Fade and pitch tools use the clip's own sound only; no separate audio is generated.
- Fades are linear or smooth-curve by default; a choice of curve shape is out of scope.
