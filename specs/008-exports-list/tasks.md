# Tasks: Exports List

- [X] T001 Baseline green
- [X] T002 `ExportRecord` model; Room v8 (entity, DAO, `AutoMigration(7, 8)`, schema 8); repository methods with default no-ops; migration and DAO round-trip tests
- [X] T003 Coordinator test (JVM): a finished export adds one record with the name, settings, bytes, length; failed and cancelled add none; implement in `ExportCoordinator`
- [X] T004 `ExportsViewModel` tests (newest first, remove with undo restores, missing file marked, empty) and implementation
- [X] T005 `ExportsScreen` (list, two columns from 840 dp, tabletop, thumbnails, Play, Share, Remove snackbar with Undo), `ExportsRoute` with intents; goldens, Compose and accessibility tests
- [X] T006 Projects bottom navigation Exports entry active and nav route; update the projects test that expected "coming soon"
- [X] T007 Emulator: export adds a record; run all suites; detekt, lint; stop emulator; results
- [ ] T008 Device checks and designer review: deferred
