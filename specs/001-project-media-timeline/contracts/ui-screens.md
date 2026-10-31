# Contract: UI Screens

Source of truth for appearance is the designer's UI Reference canvas (https://claude.ai/artifact/D8dMeHZE4XUAvNj4wERyNo). This contract defines, per screen, the state each view model exposes and the events it accepts, so layout work in all postures draws from one behavior.

Design artboards: Projects, Multi-import, Timeline and trim, Detached audio selected, Foldable unfolded editor, Foldable tabletop posture, Foldable projects and media. Screens for effects, picture in picture, audio editing, and export are out of scope.

## Arrangement rules (all screens)

| Arrangement | When | Layout |
|-------------|------|--------|
| Compact | Compact width (under 600 dp), or the cover screen | Single pane, designed phone layout |
| Compact, wide content | Medium width (600 to 839 dp) | Same layouts as compact with more grid columns and a content max width |
| Expanded | Expanded width (840 dp and up), flat | Multi-pane as designed (editor: rail, Media panel, preview, inspector, timeline; projects: list + detail). Add media is a large centered panel over the editor |
| Tabletop | Half-opened with horizontal fold | Editor: preview on one side of the hinge, transport, timeline, and tool bar on the other; Add media fills the half below the hinge. Projects: two-pane on one side of the hinge, or the compact layout if that area is too narrow |

No interactive element may overlap a separating hinge or fold occlusion. Changing arrangement preserves screen state.

## ProjectsScreen

- **State**: `continueEditing: ProjectSummary?`, `projects: List<ProjectSummary>` (thumbnail, duration, name, relative date, status), `filter: All | Drafts | Exported`, `counts`, `query`, `selectedProjectId` (expanded only), `isLoading`.
- **Events**: `NewProject`, `OpenProject(id)`, `SelectProject(id)`, `SetFilter`, `SetQuery`, `Rename(id, name)`, `Duplicate(id)`, `Delete(id)` (after confirmation), `OpenSearch`, `OpenSettings`.
- **Empty states**: no projects (only the New project tile); no results for query or filter.
- **Inactive**: bottom navigation entries Templates and Exports are dimmed with a "coming soon" message.

## AddMediaScreen

- **State**: `tab: Videos | Photos | Audio`, `albums`, `selectedAlbum`, `items` (thumbnail, duration, isSelected, selectionIndex), `selection` (ordered), `totalDurationMs`, `permission: Granted | Partial | Denied | NotAsked`, `importProgress`.
- **Events**: `SwitchTab`, `SwitchAlbum`, `ToggleItem(id)`, `AddToTimelineInOrder` (the only add action; there is no library), `Close`, `RequestAccess`, `OpenSystemPicker`, `CancelImport`.
- **Rules**: selection persists across tab and album changes; deselecting renumbers; per-file errors are reported without stopping the batch.

## EditorScreen

- **State**: `project` (name, format label, save status), `timeline`, `selection`, `playback` (isPlaying, positionMs, durationMs), `canUndo`, `canRedo`, `arrangement`, `projectMedia` (media referenced by clips, for the Media panel), `missingMedia: Set`, `toolbar` (active tab Edit; others dimmed), `clipTools` (derived from selection), `inspector` (expanded only).
- **Events**: `Play`, `Pause`, `SeekTo(ms)`, `StepFrame(dir)`, `Select(clip)`, `Trim(clip, edge, ms)`, `Split`, `Duplicate`, `Delete`, `DetachAudio`, `ReplaceAudio(media)`, `Mute(clip, muted)`, `MoveClip(...)`, `AddClips(...)`, `Undo`, `Redo`, `Fullscreen`, `Relink(media)`, `RemoveMissing(clip)`, `TapInactiveTool(tool)`, drag-and-drop from the Media panel. Photo duration changes through `Trim` on a photo clip.
- **Clip tools by selection**:
  - Video clip: Split, Speed (dimmed), Volume (dimmed), Detach (only when `attachedAudio`), Duplicate, Delete.
  - Audio clip: Split, Mute, Replace, Duplicate, Delete.
- **Keyboard** (hardware keyboard present): Space play/pause, Left/Right step, Delete removes selection, undo/redo chords.
- **Missing media**: missing clips show a placeholder with Relink and Remove; playback skips them.
- **Tabletop**: transport shows previous clip, play, next clip, split; the tool bar sits below the timeline.

## Cross-screen behavior

- Inactive tools show a "coming soon" message and change nothing.
- All icon-only controls have accessibility labels; touch targets are at least 48dp; text scales with the system setting.
- Screen state survives rotation, fold, unfold, resize, and split-screen without restarting playback or losing edits.
