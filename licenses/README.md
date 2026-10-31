# Third-party licenses

## Fonts (bundled in the app)

| Font | License | Text |
|------|---------|------|
| Manrope | SIL Open Font License 1.1 | `Manrope-OFL.txt` |
| JetBrains Mono | SIL Open Font License 1.1 | `JetBrainsMono-OFL.txt` |

## Libraries (linked into the app)

All are Apache License 2.0 unless noted. Versions are pinned in `gradle/libs.versions.toml`.

- AndroidX: Activity, Compose (BOM, Foundation, Material3, Material3 adaptive, UI), Hilt navigation, Lifecycle,
  Media3 (ExoPlayer, Transformer, Effect, UI), Navigation, Room, Window
- Dagger Hilt
- Coil 3 (`coil-compose`, `coil-video`)
- kotlinx.coroutines

## Test and build tooling (not shipped)

JUnit 4 (EPL 1.0), Kotest assertions, Turbine, Paparazzi, detekt, Kotlin and Android Gradle plugins, KSP.

## Test media

The files in `core/media/src/androidTest/assets` are generated with ffmpeg for tests only; see the `LICENSES.md`
beside them. They are not part of the app.
