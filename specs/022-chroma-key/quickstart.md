# Quickstart: Chroma Key

1. `gradle --offline :core:model:testDebugUnitTest` for model, command, invariant tests.
2. `gradle --offline connectedDebugAndroidTest` for the migration test, the shader test (a pure-green frame becomes transparent, a red one stays), the export test (keyed overlay shows the layer beneath), and accessibility checks.
3. By hand: add a green-screen clip as an overlay, open Overlay, Chroma key tab, switch on, move Strength, tap Pick on the backdrop, turn the checkerboard on, export.

## Results

Full connected suite on the emulator: 0 failures (one export-module start-up flake was rerun clean, 36 tests). The shader and alpha blending worked on the first try; the export test shows the layer beneath the keyed backdrop and an unchanged kept side.
