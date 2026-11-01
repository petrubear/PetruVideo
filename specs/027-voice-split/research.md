# Research: Voice Split

- **Model**: Open-Unmix UMX-HQ vocals, ONNX export by edgetools on Hugging Face, MIT weights; UMX-L is cleaner but non-commercial licence; Spleeter ONNX exports have unclear licence and a 2-stem/4-stem weight mix-up risk.
- **Backing as subtraction**: guarantees Voice + Backing = original, no second model, and works for any instrument.
- **Chunking**: UMX uses a bidirectional LSTM, so chunk edges lose context; overlap with discarded margins avoids audible seams.
- **No Wiener refinement** (the export is validated for the plain ratio mask).
- **Runtime**: ONNX Runtime Android ships arm64 and x86_64, so the emulator can run the real model.
