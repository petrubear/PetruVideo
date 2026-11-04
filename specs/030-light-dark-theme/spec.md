# Feature Specification: Light and Dark Theme

**Feature Branch**: `030-light-dark-theme`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "The app should have a light and dark theme. Now only the dark theme exists. It should follow the system theme, light or dark, and change accordingly."

## Scope note

Today every screen is dark, whatever the phone is set to. This feature adds a light look for every screen and makes the app pick the look from the phone's own light or dark setting, switching when that setting changes. The dark look stays as it is now. Not included: a setting inside the app to force light or dark, custom colors, and a different look for the exported video (the exported picture never depends on the theme).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The app follows the phone's light or dark setting (Priority: P1)

A user whose phone is set to light mode opens PetruVideo and sees a light app. A user whose phone is set to dark mode sees the dark app they see today.

**Why this priority**: This is the request. Without it nothing else matters.

**Independent Test**: Set the phone to light, open the app and check every main screen is light; set it to dark and check the app is dark.

**Acceptance Scenarios**:

1. **Given** the phone is in light mode, **When** the user opens the app, **Then** the Projects, Templates, Exports, Settings, import and editor screens use the light look.
2. **Given** the phone is in dark mode, **When** the user opens the app, **Then** every screen looks as it does today.
3. **Given** the phone has no clear setting (scheduled or automatic dark mode), **When** the system switches, **Then** the app follows the system's current mode.

---

### User Story 2 - The look changes while the app is open (Priority: P1)

The user changes the phone between light and dark (quick settings, schedule, battery saver) while PetruVideo is open, and the app changes its look right away without losing anything.

**Why this priority**: "Change accordingly" is part of the request, and a restart that loses an edit would be a bug.

**Independent Test**: Open a project in the editor, play it, switch the phone's mode, and check the look changes with the playhead, selection, open panels, and playback unchanged.

**Acceptance Scenarios**:

1. **Given** a project is open in the editor, **When** the phone switches between light and dark, **Then** the editor changes look within one second, keeps the playhead, selection and any open panel, and does not stop playback or restart.
2. **Given** an export is running, **When** the phone switches mode, **Then** the export keeps running and its progress is shown in the new look.
3. **Given** the app is in the background, **When** the phone switches mode and the user returns, **Then** the app shows the new look from the first frame.

---

### User Story 3 - Every part of the app is readable in both looks (Priority: P1)

In both looks, text, icons, buttons, the timeline lanes and their clips, sliders, panels, dialogs and notices can be read and used. Nothing is left dark-on-dark or light-on-light.

**Why this priority**: A light look with unreadable parts is worse than no light look.

**Independent Test**: Walk every screen and panel in both looks and check text and controls against the readable-contrast goal, using screenshots of each.

**Acceptance Scenarios**:

1. **Given** either look, **When** the user opens any screen, panel, sheet or dialog, **Then** text and icons are readable against their background and controls are visible.
2. **Given** the editor in either look, **When** the user looks at the timeline, **Then** the video, audio, music, overlay, and effects lanes and their clips stay clearly different from each other and from the background, and the playhead and selection outline are visible.
3. **Given** the preview area, **When** a video plays in either look, **Then** the video picture is shown unchanged and the area around it does not wash out the picture.

---

### User Story 4 - System bars and the first frame match (Priority: P2)

The status bar, the navigation bar and the screen shown while the app starts match the current look, so there is no dark flash on a light phone or a light flash on a dark phone.

**Why this priority**: It is what makes the app feel finished, but the app works without it.

**Independent Test**: Cold start the app in each mode and check the first frame, the status bar icons and the navigation bar.

**Acceptance Scenarios**:

1. **Given** a light phone, **When** the app starts from closed, **Then** the first frame is light, with dark status bar icons.
2. **Given** a dark phone, **When** the app starts from closed, **Then** the first frame is dark, with light status bar icons.

---

### Edge Cases

- Switching the look while a dialog, bottom sheet, picker, or the keyboard is open: they change look and stay open.
- Switching the look in split-screen, on the cover screen, on a tablet or foldable, and while folding or unfolding: same behavior, no restart.
- Switching the look while recording a voice-over or while the camera or picker of another app is in front: the recording continues; the app shows the new look when it returns.
- Images and video the user added (thumbnails, project pictures, clips, stickers with their own colors) are never recolored by the look.
- Text drawn into the video (titles, captions) keeps the colors the user chose and never changes with the look.
- Text and icons placed over a picture (duration badges on project pictures, handles on clips) stay readable over both bright and dark pictures.
- Large font sizes and the accessibility color settings keep working in both looks.
- Exported files and their quality do not depend on the look.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST have a light look and a dark look that cover every screen, panel, sheet, dialog, notice, and component.
- **FR-002**: The app MUST pick the look from the phone's current light or dark setting when it starts and MUST NOT need any setting inside the app.
- **FR-003**: When the phone's setting changes while the app is open, the app MUST change look within one second without restarting, and without losing the playhead, selection, open panels, text being edited, unsaved edits, playback, recording, or a running export.
- **FR-004**: The dark look MUST look as it does today.
- **FR-005**: In both looks, normal text MUST have a contrast of at least 4.5:1 against its background and large text, icons and control outlines at least 3:1.
- **FR-006**: The timeline lanes (video, audio, music, overlay, effects) MUST keep their separate colors in both looks and MUST stay clearly different from each other, from the background, and from the selection outline and the playhead.
- **FR-007**: The picture of a video, photo, thumbnail, or project preview MUST NOT be recolored by the look. The preview background is not part of the picture and MUST keep the video easy to judge in both looks.
- **FR-008**: Text and graphics rendered into the project or the export (titles, captions, stickers) MUST keep their own colors in both looks.
- **FR-009**: The status bar and navigation bar icons and the first frame shown at start MUST match the current look.
- **FR-010**: The change MUST work at compact, medium and expanded window sizes, in tabletop posture, on the cover screen, in split-screen, and across fold and unfold.
- **FR-011**: The screens MUST keep their layout, text and behavior; only colors and their tints change between the looks.
- **FR-012**: The app's own light and dark colors MUST come from one place, so that a screen cannot use a color that exists in only one look.

### Key Entities *(include if feature involves data)*

- **Look**: Light or dark. Chosen from the phone's setting, never stored in a project, and not saved by the app.
- **Color set**: The named colors of a look (backgrounds, surfaces, outlines, text, accent, warning, danger, boost, and the lane colors with their fills). Each name has a value in both looks.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the app's screens, panels, sheets and dialogs (checked from a list of all of them) are shown in both looks, and none is left in the wrong look after a switch.
- **SC-002**: After the phone's setting changes, the open app shows the new look within 1 second in 100% of tries, with the playhead, selection and open panels unchanged.
- **SC-003**: No text pair on any screen has less than 4.5:1 contrast (3:1 for large text, icons and outlines) in either look, checked by an automatic check over the screenshots of every screen.
- **SC-004**: Cold start on a light phone shows no dark flash and on a dark phone no light flash (the first frame matches the look in 100% of tries).
- **SC-005**: Every screen that had a screenshot test before has a screenshot in both looks, and the dark ones are unchanged from the earlier ones.
- **SC-006**: A project edited, exported, and reopened in one look gives the same exported file and the same project in the other look.

## Assumptions

- "Follow the system" means the phone's own light or dark setting; there is no in-app switch in this feature. A setting to force light or dark can be a later feature.
- Dark stays exactly as designed today; the light look is new and is designed from the same palette (same accent hue and lane hues, lighter surfaces and darker text).
- Dynamic color from the wallpaper is not used; the app keeps its own palette in both looks.
- The preview area of the editor uses a neutral backdrop that suits video in both looks (a dark backdrop is acceptable in the light look if it keeps the picture easy to judge, as video editors do); this is decided at design time and must meet FR-007.
- Lane fills that are dark tints today become light tints in the light look, with the lane border colors adjusted for contrast.
- The look is not stored: it is read from the phone each time, so there is nothing to migrate.
