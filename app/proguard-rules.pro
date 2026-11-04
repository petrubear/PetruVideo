# Keep rules added per feature as needed.

# ONNX Runtime's native code looks its Java classes up by name (for example OnnxJavaType and TensorInfo), so they must
# not be renamed or removed; the voice split model fails to load otherwise (feature 031, release build check).
-keep class ai.onnxruntime.** { *; }
