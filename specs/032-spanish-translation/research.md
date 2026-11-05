# Research: Spanish Translation

## 1. Where the texts live

- **Decision**: string resources in each module that shows them, named with a module prefix (`editor_`, `projects_`, `export_`, `import_`, `ds_`, `app_`), English in `values/strings.xml`, Spanish in `values-es/strings.xml`.
- **Rationale**: Android merges resources from all modules into one namespace, so prefixes prevent collisions, and each module stays self-contained. `values-es` covers every Spanish country.
- **Alternatives**: one central strings module: every screen change would touch it and modules would depend on a text bag.

## 2. Messages from view models

- **Decision**: a `UiText` sealed type (`Res(@StringRes id, args)`, `Plural(@PluralsRes id, count, args)`, `Raw(String)`) in `core:designsystem`, held in state (`message: UiText?`) and resolved to a string at the screen with `asString()`.
- **Rationale**: view models have no `Context` and must not (they outlive the activity); resolving at draw time makes a live language switch work for free, because the state holds ids, not already-translated sentences.
- **Alternatives**: inject a string provider that uses the application context: it would resolve once and keep the old language after a switch, and it makes tests need Android.

## 3. Reasons from the editing engine

- **Decision**: `core:model` returns enums or small codes (for example `VoiceSplitBlock { NoSound, Reversed, SpeedCurve, TooLong }`) and the editor maps them to texts; existing `RejectReason` and `Notice` already are codes.
- **Rationale**: `core:model` is pure JVM and must stay so; one message per reason means one translation.
- **Alternatives**: put resource ids in the model: it would depend on Android.

## 4. Enum labels

- **Decision**: enums that carry a display `label` in `feature:editor` (`ClipTool`, `ToolbarTab`, `AudioToolKind`, `TextToolKind`, and similar) take `@StringRes labelRes`; enums in `core:model` that are shown (reverb, equalizer, filter, effect, transition, font, shape presets) get a `label()` composable extension in the feature that shows them, using a `when` so the compiler lists every case.
- **Rationale**: the `when` fails the build if a new enum case has no text.

## 5. Plurals, times, and dates

- **Decision**: counts use `<plurals>` resources (`pluralStringResource`), relative times ("2 min ago", "Yesterday") use string resources chosen in one function in `feature:projects`, and durations keep the existing `m:ss` formatting.
- **Rationale**: Spanish has `one` and `other` (and `many` for large numbers in some forms); resources handle it. Relative times are a few fixed phrases.
- **Alternatives**: `DateUtils.getRelativeTimeSpanString`: its wording differs from today's English texts and would change the English output (FR-012).

## 6. The default project name

- **Decision**: the name made at creation ("Untitled 1") comes from a small provider in `core:data` that reads a string resource; the count query counts both the English and the Spanish prefix.
- **Rationale**: the name is saved text (FR-007 edge case): it keeps the language of its creation, and numbering stays unique across languages.

## 7. Per-app language and live switching

- **Decision**: `res/xml/locales_config.xml` listing `en` and `es`, `android:localeConfig` in the manifest, and `locale` added to the activity's `configChanges`.
- **Rationale**: Android 13+ shows the app in the per-app language settings; without `locale` in `configChanges` the activity is recreated on a change, which loses transient state (Principle VIII).
- **Alternatives**: AppCompat per-app locale API (AndroidX): not needed for two languages and adds a dependency.

## 8. Enforcement

- **Decision**: lint `MissingTranslation` as an error for every module; `scripts/check-no-literals.sh` greps screen code (`Text("`, `contentDescription = "`, `label = "`, `say("`, `title = "`) with an allow-list file for exceptions (product names, units), run in `ci.yml`.
- **Rationale**: FR-011. A grep is simple, fast, and has no new dependency; the allow-list keeps exceptions visible.
- **Alternatives**: a custom detekt rule: more code to write and maintain for the same result.

## 9. Spanish quality

- **Decision**: a glossary (`docs/glossary-es.md`) written first, neutral Latin American Spanish, informal "tu" avoided in favor of impersonal or imperative-neutral forms ("Agregar clip", "Exportar"); reviewed by the maintainer before release.
- **Rationale**: consistent terms across about 700 texts; the maintainer is a Spanish speaker.

## Proposed glossary (first version)

| English | Spanish |
|---|---|
| Project / Projects | Proyecto / Proyectos |
| Clip | Clip |
| Timeline | Línea de tiempo |
| Trim / Split / Duplicate / Delete | Recortar / Dividir / Duplicar / Eliminar |
| Speed / Reverse / Freeze frame | Velocidad / Invertir / Congelar fotograma |
| Overlay (picture in picture) | Superposición |
| Keyframes | Fotogramas clave |
| Effects / Filters / Transition | Efectos / Filtros / Transición |
| Audio / Volume / Mute / Detach audio | Audio / Volumen / Silenciar / Separar audio |
| Voice-over | Locución |
| Ducking | Atenuación automática |
| Reverb / Equalizer / Noise reduction | Reverberación / Ecualizador / Reducción de ruido |
| Fade in / Fade out | Aparecer / Desaparecer (audio: Fundido de entrada / de salida) |
| Chroma key | Clave cromática |
| Mask | Máscara |
| Captions / Stickers / Text | Subtítulos / Stickers / Texto |
| Export / Exports | Exportar / Exportaciones |
| Templates | Plantillas |
| Settings | Ajustes |
