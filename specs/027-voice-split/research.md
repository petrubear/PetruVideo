# Research: Voice Split

- **Decision**: Open-Unmix UMX-HQ vocals (ONNX export by edgetools, MIT weights). **Why**: 17 MB, MIT confirmed, simple spectrogram contract, low memory. **Alternatives**: Demucs 6-part (258 MB, or 136 MB fp16, about 1.1 GB of memory, hybrid waveform branch, licence to verify); Spleeter ONNX exports (unclear licence, 2-stem and 4-stem weight mix-up risk); UMX-L (CC BY-NC-SA, non-commercial). Demucs stays a possible later "High quality" option behind the same interface.
- **Decision**: Backing is the original minus Voice. **Why**: Voice plus Backing equals the original exactly, works for any instrument, needs no second model. **Alternative**: run the accompaniment graph; not available here.
- **Decision**: Allow overlapping audio-lane clips up to 3 layers, in parallel rows. **Why**: Voice and Backing must play at once; the lane today is one back-to-back sequence and the engine rejects overlaps; it also enables music under voice-over. **Alternatives**: a stem-pair clip with two sources inside (a new kind of clip and custom controls); a fixed second lane (limited to two layers, duplicates the lane logic).
- **Decision**: `nextFreeStart` snaps only when all 3 layers are full. **Why**: otherwise moving one stem would be pushed away from the other. **Risk**: behaviour change for music, duplicate, and voice-over placement; covered by updating the existing tests.
- **Decision**: Chunk with context margins. **Why**: Open-Unmix uses a bidirectional LSTM, so chunk edges lose context.
- **Decision**: No Wiener refinement (the export is validated for the plain ratio mask).
- **Decision**: Cancel on edit rather than lock. **Why**: chosen in clarify; the result must always match the clip it was made from.
- **Runtime**: ONNX Runtime Android ships arm64 and x86_64 libraries, so the emulator can run the real model.
