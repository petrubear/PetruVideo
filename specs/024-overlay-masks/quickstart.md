# Quickstart: Overlay Masks

1. `gradle --offline test verifyPaparazziDebug detekt lintDebug`.
2. `gradle --offline connectedDebugAndroidTest` (migration, export mask test, accessibility).
3. By hand: add an overlay, Mask tab, Circle, drag Feather, Invert, export.

## Results

Full connected suite on the emulator: 311 tests, 0 failures, 3 skipped. Export test: a Circle mask cuts the corners, Invert flips it, no mask is unchanged.
