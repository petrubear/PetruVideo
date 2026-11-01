# Feature Specification: More Effects

**Feature Branch**: `025-more-effects`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "More effects: Glitch, RGB split (chromatic aberration), Glow, VHS and Grain, next to Blur, Pixelate, Mirror and Shake, each with a Strength slider, live in the preview, saved with the project, one undo step, exported as in the preview."

## Scope note

Five new picture effects for main-track clips, chosen the same way as the existing four on the Effects tool (one effect per clip, with Strength). Several effects stacked on one clip are out of scope and a follow-up: it changes how a clip's effect is stored and applied.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Give a clip a new look with one tap (Priority: P1)

A user selects a clip, opens Effects, and chooses Glitch, RGB split, Glow, VHS, or Grain. The preview shows it live and a Strength slider tunes it.

**Independent Test**: Choose each new effect on a striped photo, export, and compare a frame with the same clip without an effect.

**Acceptance Scenarios**:

1. **Given** the Effects tool, **When** opened, **Then** the tile row offers Blur, Pixelate, Mirror, Shake, Glitch, RGB split, Glow, VHS, and Grain (scrolling sideways), with the current one marked.
2. **Given** Glitch, **When** shown, **Then** horizontal bands of the picture jump sideways at random moments with a colour split; stronger means more and bigger jumps.
3. **Given** RGB split, **When** shown, **Then** the red and blue channels are shifted apart sideways; stronger means a bigger gap.
4. **Given** Glow, **When** shown, **Then** bright areas bloom softly into their surroundings.
5. **Given** VHS, **When** shown, **Then** the picture gets scan lines, sideways wobble, colour bleed, and slightly less colour.
6. **Given** Grain, **When** shown, **Then** moving film grain covers the picture.
7. **Given** any choice or Strength change, **When** committed, **Then** it is one undo step; Strength shows live while moving.

---

### User Story 2 - Kept, saved, exported (Priority: P2)

The effect is saved with the project, copied by clip split and duplicate, and exported as in the preview, together with filters, transitions, and keyframes.

**Acceptance Scenarios**:

1. **Given** a clip with a new effect, **When** the project is closed and reopened or duplicated, **Then** the effect and Strength are unchanged.
2. **Given** an export, **When** a frame is compared with the preview, **Then** they match within a small visual difference (time-based effects follow the project time).

## Edge Cases

- Remove effect returns the clip to exactly its earlier look.
- Projects saved before this feature open unchanged.
- Strength 0 shows (almost) no change; Strength 100 is the strongest look.

## Requirements *(mandatory)*

- **FR-001**: The effect kinds MUST include Glitch, RGB split, Glow, VHS, and Grain, each with Strength 0 to 100.
- **FR-002**: The effects MUST show live in the preview and in the export; time-based effects (Glitch, VHS, Grain) MUST follow the clip's time so the export matches.
- **FR-003**: Choosing and changing the effect MUST be one undo step and MUST be saved, copied, and restored like the existing four.
- **FR-004**: A clip without an effect MUST look and export exactly as before.
- **FR-005**: The effect row MUST stay usable in all layouts with labels for each tile.

## Success Criteria *(mandatory)*

- **SC-001**: A user can apply a new effect in 3 taps or fewer after selecting a clip.
- **SC-002**: In an export, each new effect changes the picture measurably compared with no effect (Glow brightens, the others change thousands of pixels).
- **SC-003**: 100% of choices survive close and reopen, undo, redo, and duplicate in automated tests.

## Assumptions

- Effect kinds are stored by name, so no database change is needed.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
