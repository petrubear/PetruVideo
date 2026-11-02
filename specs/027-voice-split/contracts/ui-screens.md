# UI: Voice Split and Layered Audio

- **Clip tool** "Split voice" beside "Detach" for a selected main-track clip. Disabled reasons: "No sound in this clip", "Not available for reversed clips", "Not available with a speed curve", "Audio layers are full here".
- **Progress card** "Splitting voice": progress bar, "{n}% done", and Cancel (touch target 56 dp); the preview shrinks like during reverse. Failure message "Could not split the voice". Cancel on edit message "Split cancelled because the clip changed".
- **Panel note** (once, under the progress or tool): "Voice and Backing are an approximate split of the original."
- **Audio lane**: when clips overlap, the lane shows extra rows (up to 3), each 36 dp high with 4 dp gaps like the overlay lane; clip labels and content descriptions unchanged; a fourth layer is refused with "Up to 3 audio layers".
- **Clip names**: "Voice" and "Backing" with the source's name in the description.
