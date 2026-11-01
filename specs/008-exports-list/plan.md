# Implementation Plan: Exports List

**Branch**: `008-exports-list` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Keep a history of finished exports and show it on a new Exports screen. Approach: `ExportRecord` in `core:model`; a Room v8 table `export_record` with a DAO and repository methods (add, observe, remove, restore); `ExportCoordinator` adds a record when an export finishes; `feature:export` gains `ExportsViewModel`, `ExportsScreen` (list, thumbnails from the video's own frame, Play, Share, Remove with Undo, missing-file state) and `ExportsRoute`; the Projects bottom navigation Exports entry becomes active and navigates to the `exports` route.

## Technical Context

Kotlin 2.4, Room 2.8 (schema 7 to 8, auto migration), Compose, Hilt, no new libraries. Tests: JVM view model and coordinator tests, Room migration and DAO tests, Compose and accessibility tests, Paparazzi goldens (phone, two columns, tabletop), emulator test that an export adds a record and a failed one does not.

## Constitution Check

All PASS: native Kotlin (I); entries are app data, videos untouched (II); lazy list, off-main thumbnails (III); independent slice (IV); view model and coordinator tests on the JVM (V); data layer separate (VI); on-device (VII); adaptive layouts (VIII).

## Project Structure

```text
core/model/.../export/ExportRecord.kt
core/data/db: ExportRecordEntity, ExportDao additions, PetruDatabase v8, schemas/8.json; repo/ExportRepository additions (default no-ops so fakes keep compiling)
core/export: ExportCoordinator adds the record on success
feature/export: ExportsContract, ExportsViewModel, ExportsRoute, ui/ExportsScreen
feature/projects: ProjectsEvent.OpenExports, effect, bottom navigation entry active
app: Routes.EXPORTS, PetruNavHost
```

## Risks

- **R1**: Checking many files for existence is slow. Mitigation: check in the background after the list shows, only visible rows lazily.
- **R2**: Thumbnails of many videos. Mitigation: lazy rows, small frames, in-memory cache of recent bitmaps.
