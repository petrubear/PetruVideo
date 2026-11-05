# Feature Specification: Theme Setting

**Feature Branch**: `033-theme-setting`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "An in-app theme setting in Settings: Follow system (default), Light or Dark. The choice is remembered, applies immediately without restarting, and the default keeps today's behavior of following the phone."

## Scope note

Today the app takes its light or dark look from the phone's setting and nothing inside the app can change that. This feature adds a Theme choice to the Settings screen with three options: Follow system, Light and Dark. Follow system is the default and behaves exactly as the app does today. Not included: custom colors, a different look per screen, and a different look for the exported video (the exported picture never depends on the theme).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pick the app's look in Settings (Priority: P1)

A user opens Settings, sees a Theme choice with Follow system, Light and Dark, and picks Dark while the phone is in light mode. The whole app turns dark.

**Why this priority**: This is the request.

**Independent Test**: With the phone in light mode, choose Dark in Settings and check that Settings and every other screen use the dark look. Choose Light and check the opposite with the phone in dark mode.

**Acceptance Scenarios**:

1. **Given** the phone is in light mode and the choice is Follow system, **When** the user picks Dark, **Then** the app uses the dark look on every screen.
2. **Given** the phone is in dark mode and the choice is Follow system, **When** the user picks Light, **Then** the app uses the light look on every screen.
3. **Given** the choice is Light or Dark, **When** the phone switches between light and dark, **Then** the app keeps the chosen look.
4. **Given** the choice is Follow system, **When** the phone switches between light and dark, **Then** the app follows it, as it does today.

---

### User Story 2 - The change is immediate and loses nothing (Priority: P1)

The user changes the Theme choice and the app changes its look right away, without restarting. If the user came from the editor, nothing is lost.

**Why this priority**: A restart that loses an edit or stops an export would be a bug.

**Independent Test**: Open a project, go to Settings from the editor's navigation if reachable or from Projects, change the choice, and return. Check the look changed at once and the project, playhead and selection are as they were.

**Acceptance Scenarios**:

1. **Given** the user changes the Theme choice, **When** the choice is saved, **Then** the app shows the new look within one second and does not restart or leave the Settings screen.
2. **Given** an export is running, **When** the user changes the choice, **Then** the export keeps running and its progress is shown in the new look.

---

### User Story 3 - The choice is remembered (Priority: P2)

The user picks Light, closes the app completely, and opens it again. The app is light from the first frame, without flashing the other look first.

**Why this priority**: A choice that resets would make the setting pointless.

**Independent Test**: Choose a look, force close the app, reopen it, and check the look and the Settings choice.

**Acceptance Scenarios**:

1. **Given** the user chose Light or Dark, **When** the app is closed and opened again, **Then** it opens in that look and Settings shows that choice selected.
2. **Given** a user who never opened Theme, **When** the app opens, **Then** the choice is Follow system and the look follows the phone.

---

### Edge Cases

- The phone's setting changes while the choice is Light or Dark: the app does not change.
- The app is updated from a version without the setting: the choice is Follow system, so the look does not change for anyone.
- The system bars (status and navigation bars) match the chosen look, not only the phone's setting, so icons stay readable.
- The app's own language and the Theme choice are independent; changing one never changes the other.
- Choice names appear in the app's language (English or Spanish).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Settings MUST show a Theme choice with exactly three options: Follow system, Light and Dark.
- **FR-002**: The default MUST be Follow system, and with it the app MUST behave as it does today.
- **FR-003**: With Light or Dark selected, every screen, panel, dialog and notice MUST use that look whatever the phone's setting is.
- **FR-004**: A change of the choice MUST take effect immediately, without restarting the app and without losing the open project, playhead, selection, open panel or a running export.
- **FR-005**: The choice MUST be remembered across closing and reopening the app, and the app MUST open in the chosen look without first showing the other.
- **FR-006**: The system bars MUST match the look in use.
- **FR-007**: The option names and the Theme title MUST be available in English and Spanish and follow the phone's language.
- **FR-008**: The choice MUST NOT change the exported video or any project data.
- **FR-009**: The Theme choice MUST be usable with a screen reader: each option has a name, the selected option is announced, and each can be reached and chosen without touch gestures.

### Key Entities

- **Theme choice**: one of Follow system, Light or Dark; one value for the whole app; stored with the app's other settings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can change the app's look from Settings in two taps or fewer after opening Settings.
- **SC-002**: After a change, the whole app shows the new look in under one second, with no restart.
- **SC-003**: After closing and reopening the app, 100% of launches show the chosen look from the first frame.
- **SC-004**: For users who never touch the setting, there is no visible change from the current app.
- **SC-005**: All three options and the title read correctly in both English and Spanish.

## Assumptions

- The Settings screen already exists and holds other choices (default export quality, lower preview), so Theme is added there.
- The light and dark looks from the previous theme feature stay as they are; only who decides which one is used changes.
- Choice names: "Follow system", "Light", "Dark" in English; "Seguir al sistema", "Claro", "Oscuro" in Spanish.
