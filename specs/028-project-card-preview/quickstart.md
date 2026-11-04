# Quickstart: Project Card Preview

## Prerequisites

- Phone connected over USB (or an emulator), debug build installed: `gradle --offline :app:installDebug`.
- At least two videos and one photo in the gallery.

## Scenarios

1. **Still picture (US1)**: create a project with a video first, leave the editor, open Projects. The Continue editing card, the grid card, the list row and the detail pane show a frame of that video. Repeat with a photo first.
2. **Follows the first clip (US2)**: move another clip to the start, go back; the picture changes. Trim the start of the first clip; the picture shows the new first frame. Delete every clip; the colored box returns.
3. **Never opened (US3)**: make a project from a template, do not open it in the editor, go to Projects; the colored box shows, then the picture appears within about 2 seconds. Install over an older version and check old projects get pictures.
4. **Missing media (edge)**: delete the first clip's source from the gallery, reopen Projects; the colored box shows, no error, the project still opens.
5. **Delete and duplicate**: delete a project and check its file is gone from `files/project-thumbs/`; duplicate one and check the copy shows a picture and changing the original's first clip does not change the copy.
6. **Fold and sizes**: with the picture showing, fold and unfold, rotate, enter split screen, and view the cover screen; the picture stays, the activity does not restart. Check compact, medium, expanded and tabletop.
7. **Scrolling**: with about 50 projects, scroll fast; no blank or wrong pictures stay and scrolling stays smooth.

## Automated checks

```bash
gradle --offline :feature:projects:testDebugUnitTest :core:media:testDebugUnitTest :core:data:testDebugUnitTest
gradle --offline :feature:projects:verifyPaparazziDebug
```

## Expected outcomes

See the success criteria SC-001 to SC-005 in [spec.md](spec.md).
