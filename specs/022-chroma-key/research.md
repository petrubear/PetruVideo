# Research: Chroma Key

- **Decision**: Key in CbCr (chroma) distance, not RGB distance. **Why**: ignores brightness differences across a lit backdrop. **Alternative**: RGB distance; rejected as it breaks on shadows.
- **Decision**: Straight alpha output from the effect step; the compositor applies the clip's opacity on top. **Spike**: confirm on device that Media3's overlay compositor honours texture alpha (spec task T003).
- **Decision**: Edit in the existing Overlay panel as a tab. **Why**: one panel slot in all three layouts, no new plumbing. **Alternative**: separate sheet like Canvas; rejected, the expanded layout wiring is heavy.
- **Decision**: Checkerboard drawn in the shader, preview only. **Why**: transparent pixels would show the main video, not a checkerboard.
- **Decision**: Pick samples a decoded frame, not the GPU. **Why**: simple and exact enough; no GL readback.
