# Research: Voice-over Recording

## 1. Recorder and file format
- **Decision**: `MediaRecorder` with `AudioSource.MIC`, output format `AAC_ADTS`, encoder AAC, 44.1 kHz, mono, 96 kbps.
- **Why**: ADTS is a stream of frames with no trailer, so a file cut short by a call, a crash, or a killed process still plays up to the cut. An MPEG-4 file only becomes valid when the recorder is stopped, which would lose the take on process death (spec FR-007). Media3 reads ADTS with its own extractor.
- **Duration**: ADTS carries no duration, and the bitrate is fixed, so the clip length comes from the measured recording time (and, for a recovered file, from `size * 8 / bitrate`). The media item is built directly instead of probed, with those values and the file's size and time as fingerprint.
- **Rejected**: `AudioRecord` plus our own encoder (more code, same result); MPEG-4 with a periodic fragmenter (not available in `MediaRecorder`).

## 2. Level meter and timer
`MediaRecorder.getMaxAmplitude()` returns the peak since the last call; polling every 50 ms gives a smooth meter. The value is shown as `sqrt(peak / 32767)` so quiet speech still moves it. The timer uses the elapsed clock, not the polling count.

## 3. Count-in and playback
Count-in is 3 one-second steps in the view model; cancel stops it. When it ends, the view model seeks the preview to the start time, starts the recorder, and starts playback right after the recorder reports it has started (the microphone takes a moment to open, so this keeps the voice and the picture together), so the voice is aligned with the picture. The start time is the playhead when Record was tapped. The sound of the project plays while recording; the screen shows a "Use headphones" hint during the count-in.

## 4. Where it ends
Stop when: the user taps Stop; the next audio clip's start is reached; 30 minutes pass; usable storage drops below 20 MB; the app leaves the foreground (ON_STOP); a call or another app takes the microphone (`MediaRecorder` error or info callback). In every case what was recorded is kept. A recording under 0.5 s is deleted with the message "Too short".

## 5. Interruption and recovery
`pending.json` holds `{projectId, startMs, recordingId}` and is written when recording starts and deleted when the clip is added. Opening a project checks it: a matching file that is at least 0.5 s long becomes a clip at `startMs` (limited by the next clip) with the message "Recovered your voice-over"; otherwise the file and journal are removed. A journal for another project stays until that project opens.

## 6. Clip rules
`AddVoiceOver` places the clip at exactly `startMs`, uses `AudioOrigin.Added`, and rejects an overlap (the stop rule above prevents it). One undo step. All audio tools, saving, preview, export, relink and project duplicate already treat it as an added audio file.

## 7. Files and deletion
Recordings live in `filesDir/voiceovers`. After a project is deleted, a sweep removes files whose URI no media item references any more (the project copy made by Duplicate still references the same file, so it is kept until both are gone).

## 8. Permission
`RECORD_AUDIO` is requested with the activity result API when Record is tapped. States: Granted, NotAsked, Denied (can ask again), Blocked (asked, denied, and the system will not show the dialog again): Blocked gives a message with an Open settings action.

## 9. Testing without a microphone
The recorder is an interface; the view model tests use a fake. The real recorder is exercised on the emulator with the permission granted. The emulator must run with audio enabled (without `-no-audio`) for the microphone to open; its input may be silence, which is enough to test the pipeline. Voice quality and echo are a device check.
