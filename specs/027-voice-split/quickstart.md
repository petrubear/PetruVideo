# Quickstart: Voice Split

1. `gradle --offline :core:model:test` for the layer rule, rows, and `SplitVoice` command tests, and the STFT round trip.
2. `gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug` for the view model, goldens, and static checks.
3. `ANDROID_SERIAL=emulator-5554 gradle --offline connectedDebugAndroidTest` for the model run on a synthetic mixture (stems sum to the mixture, the model checksum matches), the overlapping-audio export test, the migration check (none needed), and accessibility. Never run it with the phone connected.
4. By hand on the phone: record yourself singing and playing, select the clip, Split voice, then add reverb to Backing only and export.
