# PetruVideo architecture

Native Android video editor, offline only. Kotlin, Jetpack Compose, MVVM with unidirectional data flow.

## Modules

| Module | Role |
|--------|------|
| `app` | Activity, navigation (Projects, Add media, Editor), Hilt wiring, manifest |
| `core:model` | Pure JVM editing engine: immutable `Timeline`, `Command`s, `EditHistory`, invariants |
| `core:data` | Room database, `ProjectRepository`, `ProjectSession` (open project), debounced `AutosaveWriter`, import |
| `core:media` | Probe, library queries, thumbnails, waveforms, missing-media checks, preview engine (Media3), resource pressure |
| `core:designsystem` | Theme, components, icons, adaptive layout (arrangements, hinge handling) |
| `feature:projects` | Projects list and the two-pane detail view |
| `feature:importmedia` | Add media: library grid, permissions, system picker, selection tray |
| `feature:editor` | Editor screens, timeline, trim handles, tool bars, expanded and tabletop layouts, shortcuts |

Dependencies point inward: features use `core:*`; `core:model` depends on nothing.

## State flow

1. `ProjectSession` (process scoped) holds the open `Timeline` and its `EditHistory`. Every edit is
   `session.apply(command)`: a pure function from timeline to `Applied` or `Rejected`. Undo and redo restore snapshots.
2. Each `Applied` timeline is published and handed to `AutosaveWriter`, which writes one Room transaction 1.5 s after
   the last edit, or at once on stop. A failed write keeps the pending timeline; running out of space shows
   `LowStorage` and the save is retried when the app resumes.
3. `EditorViewModel` combines the session, the `PreviewEngine` state, media availability, and local UI state into one
   `EditorState`. Screens are stateless: they take state and `onEvent`.
4. Because the session and the player outlive the screen, rotation, resize, fold, and unfold never lose the project,
   playhead, or edits. `MainActivity` handles configuration changes itself, so it is not recreated.

## Arrangements

`ArrangementResolver.resolve(ArrangementInput)` is a pure function of window size and fold state (never the device):

| Arrangement | When | Editor | Projects | Add media |
|-------------|------|--------|----------|-----------|
| Compact | under 840 dp (wide content 600 to 839 dp) | phone layout | grid, 2 or 3 columns | full screen, 3 or 5 columns |
| Expanded | 840 dp and up | rail, Media, preview, inspector, timeline | list and detail panes | centered panel, 6 columns |
| Tabletop | half-opened, horizontal separating hinge | preview above, controls below | two-pane below the hinge | picker below the hinge |

`AdaptiveScaffold` renders the slot for the arrangement. With a vertical separating hinge, `HingeSplit` leaves a gap
exactly as wide as the hinge so nothing interactive sits under it; in tabletop the scaffold splits the window at the
hinge.

## Testing

- Engine: JVM unit and property tests, including undo for every command.
- View models: JVM tests with fakes.
- Screens: Paparazzi golden images (`recordScreenshots`, `verifyScreenshots`), reviewed against `design/reference`.
- Device: instrumented tests for Room, media probing, timeline flows, trim and audio, keyboard, and rotation.
