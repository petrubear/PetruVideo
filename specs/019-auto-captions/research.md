# Research: Auto Captions

## 1. Recognizer
- **Decision**: `SpeechRecognizer.createOnDeviceSpeechRecognizer` (Android 13, API 33) with the audio given through `EXTRA_AUDIO_SOURCE` (a pipe whose other end we write 16 kHz mono PCM into) plus `EXTRA_AUDIO_SOURCE_CHANNEL_COUNT`, `..._ENCODING`, `..._SAMPLING_RATE`. Languages come from `checkRecognitionSupport` (installed on-device languages). `isOnDeviceRecognitionAvailable` gates the tool.
- **Why**: it is the only recognizer that is both on the phone and part of Android; it accepts audio from a stream, so the microphone and its permission are not needed.
- **Rejected**: the default `SpeechRecognizer` (may use the network); bundling a model (no offline way to fetch one here and a large size); `RecognizerIntent` activity (needs the microphone and the user).
- **Not testable on the emulator**: real recognition. The service and view model are tested with a fake `SpeechTranscriber`; the real one is checked on the phone later, and its availability check is tested to report "unavailable" without crashing.

## 2. Phrases from loudness, not from the recognizer
Phrase times come from the waveform peaks: a bin (50 ms) is speech when `peak x clip volume >= 0.03`; gaps under 400 ms are bridged; phrases under 300 ms are dropped; a phrase over 8 s is cut at the quietest bin in its last 3 s, repeatedly. Times map to the timeline as `clipStart + (source - clip.sourceStart) x 1000 / speed`. Reversed clips and clips with a curve are skipped (the sound is not forward speech).

## 3. Phrase audio
`SoundCodec.decode(uri, startUs, endUs)` already returns PCM for a range; it is mixed to mono, resampled linearly to 16 kHz, and written to the pipe. Each phrase is decoded on its own (a few seconds each), so memory stays small.

## 4. The command
`AddCaptions(lines, replace, newId)`: each line becomes `TextClip(caption = true)` centered at x 500 and y 880 permille, size 50, white text, background box enabled at 60% opacity, a short fade-in and fade-out of 0 (none) so timing is exact. Lines shorter than `TEXT_MIN_MS` are dropped; at most 200 are added. With `replace`, text clips with `caption = true` are removed first. One undo step.

## 5. Persistence
Room 15 to 16 auto migration: `text_clip.caption INTEGER NOT NULL DEFAULT 0`.

## 6. Editor
Text tab bar: Add text and Auto captions. The panel (phone sheet, unfolded inspector section, tabletop lower half) shows the availability: Available (language chips for installed languages, Replace switch, Start), NeedsNewerAndroid ("Auto captions need Android 13 or newer"), LanguageMissing (names the language, lists installed ones, "Open speech settings"). While running: progress bar and Cancel. When done: a note "Added 14 captions, 2 phrases skipped". The settings button opens `Settings.ACTION_VOICE_INPUT_SETTINGS` through an editor effect.
