# Feature Specification: Spanish Translation

**Feature Branch**: `032-spanish-translation`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Translate the app to Spanish and follow the phone's language." (chosen from the list of what is still missing from the app)

## Scope note

Today every word the app shows is English and most of it is written into the screens themselves, so a Spanish speaker sees English everywhere. This feature makes the whole app available in Spanish (neutral Latin American Spanish, no regional slang) and makes it follow the phone's language: Spanish phones get Spanish, every other language keeps English. English stays as it is. Not included: other languages, a language picker inside the app (Android's own per-app language setting is used instead), right-to-left layouts, and translating anything the user wrote or imported (project names, text on the video, file names).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The app speaks Spanish on a Spanish phone (Priority: P1)

A user whose phone is set to Spanish opens PetruVideo and sees every screen, button, label, panel, and message in Spanish. A user whose phone is set to English, or to any language that is not Spanish, sees the same English app as today.

**Why this priority**: This is the request.

**Independent Test**: Set the phone to Spanish and open every screen and panel; set it to English and check nothing changed.

**Acceptance Scenarios**:

1. **Given** the phone's language is Spanish (any country), **When** the user opens the app, **Then** the Projects, Templates, Exports, Settings, import, editor, and export screens and all their panels, sheets, and dialogs are in Spanish.
2. **Given** the phone's language is English, **When** the user opens the app, **Then** every text is the same as before this feature.
3. **Given** the phone's language is one the app does not have (for example Japanese), **When** the user opens the app, **Then** it shows English.
4. **Given** the app in Spanish, **When** the user walks through the editor tools, **Then** no English text remains except names the user chose and product or technical names (PetruVideo, 1080p, fps, MP4).

---

### User Story 2 - Messages, errors, and notifications are in Spanish too (Priority: P1)

The short messages that appear while working (for example "Audio moved to the next free spot", "Up to 3 audio layers play together"), the reasons a tool is not available, the error and storage messages, the permission explanations, and the notification shown while an export runs are all in the phone's language.

**Why this priority**: A Spanish app that falls back to English on the first error or the first disabled tool is not translated.

**Independent Test**: In Spanish, trigger a tool that is not available, a refused edit, a low-storage condition, and an export, and read each message and the notification.

**Acceptance Scenarios**:

1. **Given** the app in Spanish and a clip with no sound, **When** the user taps a sound tool, **Then** the reason is shown in Spanish.
2. **Given** the app in Spanish, **When** an edit is refused (for example, too many audio layers), **Then** the message is in Spanish.
3. **Given** the app in Spanish, **When** an export runs, **Then** its progress screen, the notification, and the finished or failed message are in Spanish.
4. **Given** the app in Spanish, **When** the app asks for a permission, **Then** the explanation shown before the system dialog is in Spanish.

---

### User Story 3 - Numbers, times, and plurals read correctly in Spanish (Priority: P2)

Counts, relative times, and durations follow Spanish rules: "1 clip", "2 clips"; "hace 2 min", "Ayer"; "0:34". Nothing reads as an English sentence with Spanish words dropped in.

**Why this priority**: Wrong plurals and word order make a translated app feel careless, but the app works without it.

**Independent Test**: In Spanish, check a project with 0, 1, and several clips, and projects edited a minute ago, yesterday, and last month.

**Acceptance Scenarios**:

1. **Given** projects with 0, 1, and 3 clips, **When** the cards show their clip count, **Then** each reads correctly in Spanish (for example "1 clip", "3 clips").
2. **Given** projects edited a minute, an hour, a day, and a month ago, **When** the list is shown in Spanish, **Then** the times read as Spanish relative times.
3. **Given** a length of 90 seconds, **When** it is shown, **Then** the format is the same as in English (minutes and seconds with a colon).

---

### User Story 4 - Choosing the language per app and switching without losing work (Priority: P2)

On Android 13 and newer the user can set PetruVideo's language in the phone's per-app language settings, and the app switches to it without restarting and without losing the playhead, selection, open panels, a running export, or unsaved edits.

**Why this priority**: Many people want a Spanish app on an English phone; it is useful but not needed for the main request.

**Independent Test**: With the editor open and a panel showing, change the app's language in the phone's settings and come back.

**Acceptance Scenarios**:

1. **Given** Android 13 or newer, **When** the user opens the phone's per-app language settings, **Then** PetruVideo offers English and Spanish.
2. **Given** the editor with a selected clip and an open panel, **When** the language changes, **Then** the texts change within one second, the activity is not recreated, and the playhead, selection, and panel are unchanged.
3. **Given** an export running, **When** the language changes, **Then** the export keeps running and its progress is shown in the new language.

---

### Edge Cases

- Spanish text is longer than English (often 20 to 30%): nothing is cut off, overlaps, or pushes a control off screen at compact width, in the tool bar, in buttons, in tabs, on the cover screen, in tabletop posture, and with large font sizes.
- Names the user typed or imported (project names, text clips, file names, stickers) are shown as written and never translated.
- Names the app made for the user at creation ("Untitled 1", "Voice - name", "Backing - name") are made in the language of that moment and stay as saved afterwards.
- A project made in English and opened in Spanish: its content is unchanged and only the app's texts change.
- Exported files, their names, and the notes inside them do not depend on the language.
- A string with a number or a name inside it keeps the number or name in the right place for Spanish word order.
- The speech recognition language for auto captions is chosen by the user and is not tied to the app's language; the language names in that list are shown in the app's language.
- The first frame, the app name under the icon, and the notification channel names are in the phone's language.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every text the app shows to the user (screens, labels, buttons, tabs, tool names, panels, sheets, dialogs, messages, errors, reasons a tool is unavailable, permission explanations, notifications, notification channel names, accessibility descriptions, the app name) MUST come from translatable text resources, with an English and a Spanish version of each.
- **FR-002**: The app MUST show Spanish when the phone's language is Spanish, in any country, and English for every other language.
- **FR-003**: The Spanish MUST be neutral Latin American Spanish, with no regional slang, and use one consistent term for each concept across the app (a glossary is part of the plan).
- **FR-004**: Logic that is not part of the screens (the editing engine and the media code) MUST NOT contain text for the user; where it needs to explain something it MUST return a reason that the screen turns into text, so one translation exists for each message.
- **FR-005**: Texts with counts MUST use plural forms for each language, and texts with several inserted values MUST allow the Spanish word order.
- **FR-006**: Relative times, dates, and durations shown by the app MUST follow the language's rules; the duration format stays minutes and seconds with a colon.
- **FR-007**: Text the user wrote or imported MUST NOT be translated or changed.
- **FR-008**: On Android 13 and newer the app MUST appear in the phone's per-app language settings with English and Spanish.
- **FR-009**: A change of the phone's or the app's language while the app is open MUST update the texts within one second, MUST NOT recreate the activity, and MUST keep the playhead, selection, open panels, text being edited, unsaved edits, a running export, and a recording.
- **FR-010**: No text may be cut off, overlap, or hide a control in either language at compact, medium, and expanded widths, in tabletop posture, on the cover screen, and at the largest supported font size.
- **FR-011**: The build MUST fail when a text has no Spanish version, and MUST fail when a screen shows a literal text instead of a resource, so a missing translation cannot reach a release.
- **FR-012**: English MUST remain unchanged, text for text, so every existing English screenshot still matches.

### Key Entities *(include if feature involves data)*

- **Text resource**: A named text with an English and a Spanish version; it can hold counts (plural forms) and inserted values.
- **Glossary**: The agreed Spanish term for each editing concept (clip, timeline, overlay, keyframes, effects, filters, voice-over, ducking, reverb, equalizer, noise reduction, chroma key, mask, transition, export), kept in the repository.
- **Reason**: A code returned by the editing engine that a screen turns into a text (for example "no sound in this clip").

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In Spanish, a walk through every screen, panel, sheet, dialog, message, and the export notification finds 0 English texts other than user content and product or technical names.
- **SC-002**: 100% of texts have a Spanish version, enforced by the build (a missing one fails the build).
- **SC-003**: 0 literal user-facing texts remain in the screens, enforced by the build.
- **SC-004**: Every existing English screenshot is unchanged, and a Spanish screenshot exists for each screen family at compact width and at 1.3 times the font size, with no cut-off text.
- **SC-005**: Changing the language with the editor open updates the texts within 1 second in 100% of tries, without recreating the activity.
- **SC-006**: A Spanish speaker can export a video from a new project following only the Spanish texts, without needing English.

## Assumptions

- Only English and Spanish are supported now; the structure makes adding a language a matter of adding translations.
- "Spanish" means one set of texts for all Spanish-speaking countries, in neutral Latin American Spanish.
- The per-app language setting of Android 13+ is used; there is no language picker inside the app. On older Android versions the app follows the phone's language.
- Project names created by the app ("Untitled 1") are text saved with the project at creation, so they keep the language they were made in.
- The editing engine's reasons (for example for refused edits and unavailable tools) can be changed to codes without changing behavior; the screens show the text.
- The glossary is proposed in the plan and reviewed by the maintainer, a Spanish speaker, before release.
