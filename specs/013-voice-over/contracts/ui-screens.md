# UI Contracts: Voice-over Recording

## Audio tab bar (nothing selected)
Edit, Add music, Record voice-over (microphone icon). Tapping Record asks for permission when needed, then starts the count-in.

## Count-in
A bar at the bottom shows a large 3, 2, 1, the hint "Use headphones for a clean voice", and Cancel. Cancel adds nothing.

## Recording bar
Replaces the bottom bars while recording: a level meter, the elapsed time "0:07", a Stop button (red, 56 dp), and a live-region announcement "Recording" for accessibility. The preview plays and the timeline scrolls with the playhead. Other editing is paused; Back stops the recording like Stop.

## After Stop
The new audio clip is selected in the audio lane with its waveform; the Audio clip tools show. Messages (snackbar): "Too short", "Stopped at the next clip", "Storage is almost full, recording stopped", "Recovered your voice-over".

## Permission messages
Denied: "A voice-over needs the microphone". Blocked: "Microphone access is off" with an Open settings action.

## Layouts
Compact: the bar at the bottom. Unfolded and tabletop: the same bar in the timeline or controls area; the meter and timer stay visible next to the Stop button. Targets 48 dp or more, labels for accessibility, text scales.
