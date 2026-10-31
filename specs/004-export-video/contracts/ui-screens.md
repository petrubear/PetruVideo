# UI Contract: Export Video

Reference: the designer's Export artboard (`design/reference/Export.png`). Extends the 001 to 003 contracts.

## Entry

- The editor's Export button (top bar, all layouts) and the project detail Export button open the Export destination. Dimmed with a reason when the project has no clips.

## Export screen (compact)

- Top bar: back arrow, title "Export".
- Preview card with a play button and the project length badge.
- Settings card: segmented controls Resolution (720p, 1080p, 2K, 4K), Frame rate (24, 30, 50, 60), Quality (Smaller, Recommended, Higher). Unavailable options are dimmed with a one-line reason shown when tapped. Rows below: Format (MP4 · H.264), Bitrate, Estimated size (accent color).
- Progress card (visible while rendering, finished, or failed): "Rendering in background", percentage, bar, Cancel; after success "Saved to gallery" with Open and Share; after failure the reason and Retry.
- Primary button "Save to gallery" (disabled while rendering or when blocked, with the reason above it).
- Events: `SelectResolution`, `SelectFrameRate`, `SelectQuality`, `Start`, `Cancel`, `Retry`, `Open`, `Share`, `Back`.

## Expanded

- Preview on the left, settings and progress on the right; button under the settings. With a vertical hinge, preview on one side and settings on the other.

## Tabletop

- Preview above the hinge; settings, progress, and the button below; nothing interactive under the hinge.

## Notification

- Title "Exporting <project name>", progress bar, percentage, Cancel action; on finish "Export saved" with a tap target that opens the app on the finished export; on failure the reason.

## State

`ExportState` for the screen: `settings`, `options` (each with allowed flag and reason), `bitrate`, `estimatedBytes`, `blocker` (none, no clips, missing media, not enough storage), `job` (the `ExportState` of the current or last job). The screen state survives configuration changes because the view model reads the coordinator.

## Accessibility

Segmented controls expose each option with its state and reason; the progress is announced at 10% steps; buttons are at least 48 dp; text scales.
