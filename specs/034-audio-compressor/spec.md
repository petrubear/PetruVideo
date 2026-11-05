# Feature Specification: Audio Compressor

**Feature Branch**: `034-audio-compressor`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "Let's add a compressor audio effect."

## Scope note

The audio panel already has Volume, Reverb, Fade, Noise reduction, Equalizer and Voice effects. This feature adds a Compressor to the same group. A compressor evens out loudness: it turns down the loud parts and lets quiet parts be heard, so speech and music stay at a steady level. It applies to one clip, is heard live in the preview, is saved with the project and is rendered by export exactly as heard. Not included: a limiter on the whole project, a compressor on the exported mix as a whole, a live reduction meter, and sidechain control (ducking already exists).

## Clarifications

### Session 2026-11-05

- Q: Which controls beyond the presets? → A: Strength and Level, plus an Advanced section with Threshold, Ratio, Attack and Release.
- Q: Show a reduction meter? → A: No; judge by ear.
- Q: Where in the sound chain? → A: After the Equalizer, before Voice effects.
- Q: Automatic makeup gain? → A: No; Level is manual and each preset sets its own Level.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Even out a voice with a preset (Priority: P1)

A user has a clip where the speaker is sometimes quiet and sometimes loud. They open the clip's audio tools, turn on Compressor, pick the Voice preset, and the loud and quiet parts become closer in level.

**Why this priority**: This is the whole point of a compressor, and presets make it usable by anyone.

**Independent Test**: Take a clip with a loud shout after quiet speech. With Compressor off, note the level difference. Turn on the Voice preset and check by ear and on the clip's level display that the difference is smaller.

**Acceptance Scenarios**:

1. **Given** a clip with sound and Compressor off, **When** the user turns it on, **Then** the Voice preset is selected and the clip sounds more even right away.
2. **Given** Compressor is on, **When** the user picks another preset, **Then** the settings change to that preset and the sound changes within 100 ms without stopping playback.
3. **Given** Compressor is on, **When** the user turns it off, **Then** the clip sounds exactly as it did before and the settings are kept for next time.

---

### User Story 2 - Fine-tune with two controls, or go deeper (Priority: P2)

A user wants more or less effect. They move Strength to reduce the loud parts more or less, and Level to make the whole clip louder or quieter after compression. A user who knows compressors opens the Advanced section and sets Threshold, Ratio, Attack and Release directly.

**Why this priority**: Presets cover most cases, but the right amount depends on the clip.

**Independent Test**: Turn Strength from 0% to 100% on a clip with big level changes and hear the loud parts come down; raise Level and hear the whole clip get louder without distortion.

**Acceptance Scenarios**:

1. **Given** Compressor is on, **When** the user changes Strength or Level, **Then** the change is heard within 100 ms, the values show as numbers with units, and the preset shows as "Custom".
2. **Given** Strength is 0%, **When** the user listens, **Then** the clip is not compressed (only Level applies).
3. **Given** a custom setting, **When** the user taps Reset, **Then** the settings return to the selected preset, or to Voice when it was Custom.
4. **Given** Compressor is on, **When** the user opens Advanced and changes Threshold, Ratio, Attack or Release, **Then** the change is heard within 100 ms, Strength follows the new Threshold and Ratio, and the preset shows as "Custom".

---

### User Story 3 - What you hear is what you export (Priority: P1)

The exported video has the same compressed sound as the preview.

**Why this priority**: An effect that sounds different after export would be a bug.

**Independent Test**: Export a clip with Compressor on and compare it with the preview by ear and by comparing loudness over time.

**Acceptance Scenarios**:

1. **Given** a project with Compressor on one clip, **When** it is exported, **Then** the file's sound matches the preview.
2. **Given** the project is saved and reopened, **When** the user opens the clip's audio tools, **Then** the same Compressor settings are shown.

---

### Edge Cases

- A clip without sound: Compressor is not offered, as for the other audio tools.
- Compressor together with Equalizer, Noise reduction, Voice effects, Reverb, Fade and Volume: they combine in a fixed order, the same in preview and export, and the final sound is kept from harsh clipping.
- Speed changes, trims, splits and duplicates keep the clip's Compressor settings.
- A very quiet clip: Compressor does not raise background noise to an obvious hiss at the Voice preset.
- Overlay clips with their own sound, detached audio and voice-over clips can use Compressor like any clip with sound.
- A change of Compressor is one undo step, like other audio tools.
- Names and labels appear in English and Spanish.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to turn Compressor on or off per clip, off by default, on any clip that has sound.
- **FR-002**: Compressor MUST offer the presets Gentle, Voice, Podcast and Punchy, with Voice selected when it is first turned on.
- **FR-003**: Users MUST be able to set Strength (0% to 100%) and Level (-12 dB to +12 dB, default 0 dB); changing either after picking a preset MUST show the preset as Custom. Level is manual: the app MUST NOT add makeup gain on its own, and each preset sets its own Level.
- **FR-003a**: An Advanced section, collapsed by default, MUST let users set Threshold (-60 dB to 0 dB), Ratio (1:1 to 20:1), Attack (1 ms to 200 ms) and Release (20 ms to 1000 ms). Strength moves Threshold and Ratio together, and changing Threshold or Ratio updates the Strength shown. Attack and Release are kept when Strength changes.
- **FR-004**: At 100% Strength the difference between the loudest and the quietest steady parts of a speech clip MUST be clearly smaller than with Compressor off, and at 0% Strength Compressor MUST NOT change the clip's dynamics.
- **FR-005**: Turning Compressor off MUST keep its settings; the other audio tools' settings MUST NOT change.
- **FR-006**: Compressor MUST combine with the other audio tools in one fixed order, the same in preview and export: Noise reduction, Equalizer, Compressor, then Voice effects; Reverb, Fade and Volume keep their existing positions. The final sound MUST be kept from harsh clipping.
- **FR-007**: A setting change MUST be heard in the preview within 100 ms of the control change and playback MUST NOT pause for it; if the device cannot keep up, the app lowers preview quality and says so, as for reverb.
- **FR-008**: Export MUST render Compressor exactly as heard in the preview.
- **FR-009**: Compressor settings MUST be saved with the project, kept through trim, speed change, split and duplicate, and shown again when the project is reopened.
- **FR-010**: The Compressor controls MUST show current values as numbers with units, have a Reset, and work with accessible labels, touch targets of at least 48 dp and scalable text.
- **FR-011**: Each Compressor change MUST be one undo step.
- **FR-012**: Labels, preset names and messages MUST exist in English and Spanish and follow the phone's language.

### Key Entities

- **Compressor**: on or off, a preset (Gentle, Voice, Podcast, Punchy or Custom), Strength (0 to 100), Level (-12 to +12 dB), Threshold (-60 to 0 dB), Ratio (1:1 to 20:1), Attack (1 to 200 ms) and Release (20 to 1000 ms). One per clip with sound; stored with the clip's other audio settings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can turn Compressor on with a preset from the clip's audio tools in three taps or fewer.
- **SC-002**: On a test clip of quiet speech followed by loud speech, the Voice preset reduces the loudness gap between the two parts by at least 40% compared with Compressor off.
- **SC-003**: A change of any Compressor control is heard within 100 ms, and playback never pauses for it on a supported device.
- **SC-004**: The exported sound matches the preview: loudness over time of the export stays within 1 dB of the preview for the same clip.
- **SC-005**: Existing projects open with no change in sound (Compressor off for every clip).
- **SC-006**: All Compressor labels read correctly in English and Spanish.

## Assumptions

- The Compressor sits in the clip's audio tools beside Equalizer and Voice effects, and follows their layout.
- Preset values are chosen by the project owner during design; the four named presets are the user-facing contract, not the exact numbers.
- Strength is a shortcut over Threshold and Ratio, so there is one set of values, not two competing ones.
- Overlay audio (feature 029) already supports the audio tools, so Compressor is available there too.
