# Tasks: Chroma Key

- [x] T001 Model: `Chroma.kt`, `OverlayClip.chroma`, `normalized`, invariant, `SetChromaKey` command, tests in core/model
- [x] T002 Data: Room v19 columns, mapper, `Migration18To19Test`
- [x] T003 Spike: `ChromaKeyEffect` shader, handle, attach to overlay items, confirm alpha blending in preview and export
- [x] T004 Live preview: `setOverlayLive` updates handle, `setChromaChecker`, tests
- [x] T005 Editor: events, view model (commit through `SetChromaKey`, checker flag, pick mode), `ChromaSampler`
- [x] T006 UI: Chroma key tab in `OverlayPanel`, pick tap layer, goldens, accessibility cases
- [x] T007 Export test: keyed overlay shows the layer beneath; key off identical to before
- [x] T008 Run all suites, mark tasks, commit, install
