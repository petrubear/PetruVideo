# Tasks: Canvas Background

- [x] T001 Model and command: `CanvasBackground`, `BlurStrength`, palette, `Timeline.canvas`, `SetCanvas`; tests first (`core/model/.../Canvas.kt`, `Models.kt`, `engine/CanvasCommands.kt`, `CanvasTest`)
- [x] T002 Persistence: Room v18 columns, mapping, migration 17 to 18; migration and round-trip tests (`core/data/.../db`, `OriginalsUnchangedTest`)
- [x] T003 [US1] Rendering: `FitWithBackgroundEffect` and the builder switch; export tests: Black equals the old output, Colour fills the bars, Blur shows the picture's colours (`core/media/.../effects`, `CompositionBuilder.kt`, `ExportPipelineTest`); record findings in plan.md
- [x] T004 [US1] Editor: Background button on the Effects tab, panel, view model events and tests including the tap count (SC-001) (`feature/editor/...`)
- [x] T005 [US1] Goldens, accessibility and Compose tests for the panel
- [x] T006 [US2] Combined with a transition, a filter, and a transform: export tests (`ExportPipelineTest`)
- [x] T007 Run all suites, record results, commit
- [ ] T008 Device check of the blur look and speed and designer review: deferred until the user is back
