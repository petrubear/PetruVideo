# Feature Specification: Settings

**Feature Branch**: `009-settings`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Settings screen from the Projects header: choose the default export quality, always use lower preview quality, and see the app version."

## Scope note

The Settings button in the Projects header, disabled until now, opens a Settings screen with three items: default export quality, a switch for lower preview quality, and About. Themes, language, account, cloud, and notification settings are out of scope.

## Clarifications

### Session 2026-11-01

- Q: Does the default export quality override a project's own saved export choice? → A: No. A project's saved choice wins; the default only applies to projects that have never been exported.
- Q: Does lower preview quality change exported videos? → A: No. It only affects the editor's preview; exports always use full quality.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose the default export quality (Priority: P1)

A user opens Settings and picks Smaller, Recommended, or Higher as the default quality. A project exported for the first time starts the export screen with that quality selected.

**Why this priority**: People who share mostly to chat apps want Smaller everywhere without changing it per project.

**Independent Test**: Choose Smaller, open the export screen of a never-exported project and see Smaller selected; open one that was exported with Higher and see Higher.

**Acceptance Scenarios**:

1. **Given** Settings is open, **When** the user picks a quality, **Then** it is shown as selected and kept after restarting the app (default: Recommended).
2. **Given** a project with no saved export choice, **When** its export screen opens, **Then** the default quality is selected, still limited by what the footage and device allow.
3. **Given** a project with a saved export choice, **When** its export screen opens, **Then** its own choice is shown, not the default.

---

### User Story 2 - Lower preview quality (Priority: P2)

A user on a slower phone turns on "Lower preview quality". The editor preview then plays at reduced size and frame rate with heavy audio effects paused, as it does under heat or memory pressure, and exports stay full quality.

**Why this priority**: Gives manual control where automatic detection is not enough, but the automatic behavior already exists.

**Independent Test**: Turn the switch on, open a project and see smoother preview at lower quality and a message once; export and see full quality; turn it off and see full preview quality again.

**Acceptance Scenarios**:

1. **Given** the switch is off, **When** it is turned on, **Then** the editor preview switches to reduced quality within a second and the setting is kept after restart.
2. **Given** the switch is on, **When** the device is under no pressure, **Then** the preview stays reduced and no pressure message repeats.
3. **Given** the switch is on, **When** a project is exported, **Then** the exported video is rendered at full quality.

---

### User Story 3 - About and layouts (Priority: P3)

Settings shows the app name and version and works in all layouts: one column on the phone, centered with a maximum width when unfolded, below the hinge in tabletop.

**Why this priority**: Standard information and layout rules.

**Independent Test**: Open About and read the version; unfold and half-fold and see no content under the hinge.

**Acceptance Scenarios**:

1. **Given** Settings is open, **When** the user looks at About, **Then** it shows "PetruVideo" and the version.
2. **Given** the unfolded device, **When** Settings opens, **Then** content is centered at a readable width; **Given** tabletop, **Then** nothing interactive is under the hinge.
3. **Given** a screen reader and 2x text size, **When** Settings is used, **Then** every control has a label and value and nothing is clipped.

---

### Edge Cases

- A default quality that the footage cannot reach is lowered like any saved choice.
- Leaving Settings and returning shows the saved values.
- The switch on a device that is also under pressure behaves as under pressure (reduced), and turning the switch off does not restore Full while pressure remains.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Settings button in the Projects header MUST open the Settings screen with a way back.
- **FR-002**: Users MUST be able to choose the default export quality from Smaller, Recommended, and Higher (default Recommended); the choice MUST be kept across restarts.
- **FR-003**: A project without a saved export choice MUST open its export screen with the default quality; a project with one MUST keep it.
- **FR-004**: Users MUST be able to turn on Lower preview quality (default off); it MUST be kept across restarts and MUST make the editor preview use reduced quality while on.
- **FR-005**: Lower preview quality MUST NOT change exported videos.
- **FR-006**: About MUST show the app name and version.
- **FR-007**: The screen MUST follow the existing visual language, be centered at a readable width when unfolded, keep nothing interactive under the hinge in tabletop, scale text, have touch targets of at least 48 dp, and have accessible labels.

### Key Entities

- **App settings**: the default export quality and the lower-preview switch, kept on the device.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user changes the default quality in under 10 seconds.
- **SC-002**: 100% of settings survive a restart.
- **SC-003**: Turning on Lower preview quality takes effect in the open editor within 1 second.
- **SC-004**: Exported videos are identical in settings and quality whether Lower preview quality is on or off.
- **SC-005**: A design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.

## Assumptions

- The designer's reference shows the Settings button but no screen; the screen follows the existing visual language and is reviewed with the designer.
- Settings are stored on the device only.
