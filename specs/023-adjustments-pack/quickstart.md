# Quickstart: Adjustments Pack

1. `gradle --offline test verifyPaparazziDebug detekt lintDebug` for model, command, golden, and static checks.
2. `gradle --offline connectedDebugAndroidTest` for the migration test, the export test (exposure raises mean brightness, neutral is unchanged), and accessibility checks.
3. By hand: open a clip, Filters, Adjust, drag each new slider, check the preview, export.

## Results

Full connected suite on the emulator: 308 tests, 0 failures after updating the filter panel test to the four groups (Light, Tone, Colour, Detail). Export test: exposure brightens a mid grey, tint moves green to magenta, neutral adjustments change nothing.
