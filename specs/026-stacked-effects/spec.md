# Feature Specification: Stacked Effects

**Feature Branch**: `026-stacked-effects`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Several picture effects on one clip: up to three of different kinds, each with its own Strength, drawn in the order they were added; chosen on the Effects tool; saved, one undo step per change, exported as in the preview."

## Scope note

A main-track clip can carry up to three effects of different kinds (Blur, Pixelate, Mirror, Shake, Glitch, RGB split, Glow, VHS, Grain). Not included: reordering effects, effects on overlay clips, or the same kind twice.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Combine effects on one clip (Priority: P1)

A user picks Glow on a clip and then taps Grain: both are active. Tapping an active tile selects it and shows its own Strength slider; Remove takes out just that effect.

**Independent Test**: Add Glow and Grain to a clip, export, and see that the picture differs from the same clip with only Glow.

**Acceptance Scenarios**:

1. **Given** a clip without effects, **When** the user taps a tile, **Then** that effect is added at Strength 50 and its tile is marked.
2. **Given** a clip with one effect, **When** the user taps another tile, **Then** both are active and drawn, the first underneath.
3. **Given** two active effects, **When** the user taps an active tile, **Then** its Strength slider and Remove action are shown.
4. **Given** three effects, **When** the user taps a fourth tile, **Then** nothing is added and the message "Up to 3 effects per clip" appears.
5. **Given** an effect, **When** Remove is tapped, **Then** only that effect is removed.
6. **Given** any change, **When** committed, **Then** it is one undo step; Strength shows live while moving.

### User Story 2 - Kept, saved, exported (Priority: P2)

All effects are saved with the project, copied by clip split and duplicate, and exported as in the preview. Projects saved before this feature open with their single effect unchanged.

## Requirements *(mandatory)*

- **FR-001**: A video clip MUST hold up to 3 picture effects of different kinds, each with Strength 0 to 100.
- **FR-002**: The effects MUST be drawn in the order added, in the preview and the export.
- **FR-003**: Adding, changing, and removing an effect MUST each be one undo step; adding a fourth MUST be refused with a message.
- **FR-004**: The effects MUST be saved, copied by split and duplicate, and a project saved with one effect MUST open with that effect.
- **FR-005**: A clip with no effects MUST look and export exactly as before.

## Success Criteria *(mandatory)*

- **SC-001**: A second effect can be added in 2 taps after the first.
- **SC-002**: In an export, a clip with two effects differs measurably from the same clip with the first only.
- **SC-003**: 100% of effect lists survive close and reopen, undo, redo, split, and duplicate in automated tests, and the migration keeps the earlier single effect.

## Assumptions

- Storage uses three effect slots in the clip row (Room schema 22); the first slot is the existing one.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
