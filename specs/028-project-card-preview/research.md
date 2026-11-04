# Research: Project Card Preview

## Why the picture is not shown today

- **Finding**: `ProjectThumbnailUpdater.refresh` (called from `EditorViewModel` after edits) writes `project-thumbs/<id>.png` and calls `setThumbnail`. `Project.thumbnailRef` is read only by the Export screen. `PlaceholderThumbnail` is used by `ProjectCard`, `ContinueEditingCard`, `ProjectRow` and `ProjectDetailPane`, and none reads it.
- **Decision**: draw `thumbnailRef` in those four views; keep the gradient as the fallback.

## How to draw the picture

- **Decision**: Coil 3 `AsyncImage` with `ContentScale.Crop`, keyed on the path plus the project's `modifiedAt` so a rewritten file at the same path is reloaded.
- **Rationale**: Coil is already a dependency (Export screen), caches and decodes off the main thread, and handles lazy lists and recycled cards.
- **Alternatives**: decode the PNG by hand in a `produceState` (more code, no cache); make the file name unique per change and drop the cache key (also done, see below, so both are safe).

## Stale file at the same path

- **Decision**: write the file as `<projectId>-<hash of first clip key>.png`, delete the previous file of that project, and store the new path. A changed first clip then always changes `thumbnailRef`, so every cache and the Room flow update with no extra key.
- **Rationale**: the stored path is the single source of truth; no timing between a file write and the UI.
- **Alternatives**: keep one path and rely on the `modifiedAt` key (works but `modifiedAt` changes on every edit and would reload the picture needlessly).

## Projects with no picture (templates never opened, older projects)

- **Decision**: in `ProjectsViewModel`, collect the project summaries; for each project with `clipCount > 0` and `thumbnailRef == null`, in order, load it (`loadProject`) and call the existing `ThumbnailRefresher.refresh`. One project at a time; the loop is cancelled with the ViewModel. A project that fails (missing source) is remembered for the session so it is not tried again on every emission.
- **Rationale**: reuses the code path the editor uses; no new generator; sequential work keeps memory and CPU low.
- **Alternatives**: a WorkManager job at app start (survives process death but adds a dependency and delay for a 2-second job); generating the picture when the template or import creates the project (misses older projects and still needs a backfill).

## Media missing or unreadable

- **Decision**: the generator already returns null on failure; `refresh` then saves a null reference and the card shows the gradient. Nothing else is needed.

## Project delete and duplicate

- **Decision**: `deleteProject` deletes the picture file; `duplicateProject` copies it to a new file for the copy (or leaves the copy empty and lets the backfill make it).
- **Rationale**: avoids orphan files (FR-007) and a duplicate pointing at a file that the original may later replace.

## Fold, resize and tabletop

- **Decision**: no special code; the picture fills the box the adaptive layouts give each view and is rebuilt from the same state after a configuration change. Verified by the existing fold checks plus a screenshot test at compact, medium and expanded widths.

## Open items

None.
