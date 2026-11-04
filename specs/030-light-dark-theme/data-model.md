# Data Model: Light and Dark Theme

No stored data. The look is read from the phone on every start and change; nothing is saved or migrated.

## PetruPalette (in memory)

One value per name, for each look:

| Group | Names |
|---|---|
| Surfaces | `background`, `surfaceLow`, `surface`, `surfaceHigh`, `surfaceHighest` |
| Lines | `outline`, `outlineDashed` |
| Text | `onBackground`, `onSurfaceMuted`, `onAccent` |
| Signals | `accent`, `danger`, `boost` |
| Lanes | `video`, `audio`, `music`, `pip`, `effects`, each with a fill (`videoFill`, `audioFill`, `musicFill`, `pipFill`, `effectsFill`) |

Rules:
- `DarkPalette` has exactly today's values.
- `LightPalette` has a value for every name (a test checks both are complete and that no two lane colors are too close).
- Text pairs meet 4.5:1 and icon, outline and lane-border pairs meet 3:1 (checked by test).

## State

`darkTheme: Boolean` read from the system and passed down through the theme; never stored.
