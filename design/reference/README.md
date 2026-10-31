# Design reference images

Exports of the designer's PetruVideo UI Reference canvas (https://claude.ai/artifact/D8dMeHZE4XUAvNj4wERyNo),
rendered from the artboards as of canvas version 14. They are for side-by-side design review only. The golden
screenshots that tests compare against are generated from the Compose UI (`src/test/snapshots` in each module) and
approved by the designer; they are never pixel-compared with these mock images.

| File | Artboard | Size | Golden test that is reviewed against it |
|------|----------|------|------------------------------------------|
| Main.png | Projects | 390x844 | `ProjectsScreenshotTest` (T044) |
| Import.png | Multi-import | 390x844 | `AddMediaScreenshotTest` (T062) |
| Editor.png | Timeline and trim | 390x844 | `EditorScreenshotTest` (T088, T105) |
| EditorAudio.png | Detached audio selected | 390x844 | `EditorScreenshotTest` (T105) |
| Effects.png, Pip.png, Audio.png, Export.png | Later features | 390x844 | none in this feature (entry points only) |
| FoldEditor.png | Foldable: unfolded editor | 1040x780 | `FoldScreenshotTest` (T127) |
| FoldTabletop.png | Foldable: tabletop posture | 1040x780 | `FoldScreenshotTest` (T127) |
| FoldLibrary.png | Foldable: projects and media | 1040x780 | `FoldProjectsScreenshotTest` (T127) |
| AddMediaPanel.png | Foldable: Add media panel | 1040x780 | `AddMediaLayoutTest` and golden (T120, T121) |
| AddMediaTabletop.png | Foldable: Add media in tabletop | 1040x780 | `AddMediaLayoutTest` and golden (T120, T121) |

## Approved deviations

None recorded yet. The designer review (T136) records each approved deviation here.
