# UI Contracts: Effects, Audio Tab, and Templates

## Editor tab bar and unfolded rail
Effects and Audio become active. Selecting Effects shows the Effects bar: Transition (enabled when a cut is selected), Effect (enabled when a video clip is selected), Remove. Audio shows: Fade, Noise, EQ, Voice, Volume, Add music; tools needing a clip are dimmed with the reason when none is selected.

## Transition
Tap the marker at a cut (48 dp target) to select it. Panel: five kind tiles and a Length slider (0.2 to 2.0 s). Marker is a small diamond on the lane. Messages: "Limited to half of the shorter clip".

## Clip effect
Panel: four tiles and a Strength slider; Reset. Clip badge "Fx".

## Add music
System document picker (audio). Success: selected audio clip at the playhead. Failure: "That file can't be used as audio".

## Templates screen (home, Templates entry)
List of six cards (name, length, slot count, drawn sample, Use). One column compact, two from 840 dp. Use opens the media picker (multi-select, limit = slots). Confirm creates the project and opens the editor; cancel returns with nothing created.

## Layouts
Compact: bars at the bottom. Unfolded: rail entries plus inspector section. Tabletop: bars on the controls half. All targets 48 dp, labels for accessibility.
