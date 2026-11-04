# Research: Light and Dark Theme

## 1. How features read colors

- **Decision**: keep `PetruColors.X` and `TrackColors.X` names; make each a `@Composable @ReadOnlyComposable` getter over a `CompositionLocal` holding the current `PetruPalette`.
- **Rationale**: 72 files read these objects directly. Keeping the names makes the change mostly mechanical and keeps one source of colors (FR-012). Non-composable uses are found by the compiler and fixed one by one.
- **Alternatives**: (a) use `MaterialTheme.colorScheme` everywhere: it has no slots for lane colors, boost, or the surface steps, and means rewriting 72 files; (b) a mirrored `LightColors` object and `if (dark)` at every use: duplicates the choice 72 times and lets a screen use a color of one look only.

## 2. Following the system and switching live

- **Decision**: `isSystemInDarkTheme()` in `PetruTheme`, and `uiMode` added to the activity's `configChanges`.
- **Rationale**: without `uiMode` Android recreates the activity on a night-mode change, which loses the editor's transient state and would stop playback (Principle VIII forbids it). With it, `onConfigurationChanged` goes to the Compose host and `isSystemInDarkTheme()` recomposes.
- **Alternatives**: let the activity restart and restore state: rejected, it breaks FR-003 and risks the running export UI and recording.

## 3. System bars and the first frame

- **Decision**: `enableEdgeToEdge(SystemBarStyle.auto(transparent, transparent) { dark })` called from a `DisposableEffect(darkTheme)` in `setContent`; window background in `values/themes.xml` (light) and `values-night/themes.xml` (dark).
- **Rationale**: bar icon color must change at runtime, not only in `onCreate`; the window background is what shows before Compose draws, so it must match to avoid a flash (SC-004).
- **Alternatives**: forcing the window background from Compose only: leaves the first frame wrong.

## 4. Light palette

- **Decision**: same accent hue (a slightly darker teal so it keeps 3:1 against white and the on-accent text keeps 4.5:1), same lane hues with darker borders and light tint fills, surfaces from a near-white background down through three steps, text near-black and a muted gray that meets 4.5:1.
- **Rationale**: keeps the product identity and lane recognition between looks (FR-006).
- **Alternatives**: invert the dark palette mechanically: gives poor contrast on the accent and on lane tints.
- **Check**: a JVM test computes the WCAG contrast ratio for every text-on-surface pair in both palettes and fails below 4.5:1 (3:1 for the icon and outline pairs).

## 5. Editor preview backdrop and picture content

- **Decision**: the preview backdrop stays near-black in both looks; thumbnails, project pictures, text-over-video and stickers are not recolored; scrims and badges over pictures keep fixed dark/white colors (they are over pictures, not over the surface).
- **Rationale**: FR-007 and FR-008. Video is judged against a neutral dark surround in editors, and these colors do not depend on the app's surface.

## 6. Dynamic color

- **Decision**: not used.
- **Rationale**: the spec's assumption keeps the product palette; dynamic color would change lane colors per wallpaper.

## 7. Existing goldens

- **Decision**: set `nightMode = NightMode.NIGHT` in every Paparazzi rule so current goldens stay dark and unchanged (SC-005), then add light goldens for one screen of each family.
- **Rationale**: Paparazzi defaults to light, which would silently turn every golden light.
