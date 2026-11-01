# UI Contract: Audio Ducking

- **Audio panel** (phone sheet, unfolded inspector, tabletop lower half) for a selected audio-lane clip: a "Ducking" row with a switch; when on, an Amount slider (0 to 100%) and a Smoothness segmented control (Quick, Medium, Slow); a one-line note when nothing plays over the clip.
- **Timeline badge**: a small ducking badge on the audio clip (bottom end), not shown when off; content description "Ducking on, <amount> percent".
- **Not offered** for video and photo clips.
- **Feedback**: changes apply on release (one undo step); the preview reloads and plays with the new ducking within a second.
- **Accessibility**: switch and sliders have labels and 48 dp targets; contrast rules as elsewhere; large-font layout wraps.
