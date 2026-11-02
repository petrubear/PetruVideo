package com.petruvideo.core.media.voicesplit

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

/** The Open-Unmix UMX-HQ vocals graph: stereo magnitude spectrogram in, estimated vocals magnitude out. */
class UmxVocalsModel(private val context: Context) : AutoCloseable {
    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val session: OrtSession by lazy {
        val bytes = context.assets.open(ASSET).use { it.readBytes() }
        environment.createSession(bytes, OrtSession.SessionOptions())
    }

    /**
     * [magnitudes] is channel, then bin, then frame: `[2][bins][frames]` flattened. The answer has the same layout.
     */
    fun estimate(magnitudes: FloatArray, bins: Int, frames: Int): FloatArray {
        val shape = longArrayOf(1, CHANNELS.toLong(), bins.toLong(), frames.toLong())
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(magnitudes), shape).use { input ->
            session.run(mapOf(session.inputNames.first() to input)).use { result ->
                val buffer = (result[0] as OnnxTensor).floatBuffer
                return FloatArray(buffer.remaining()).also { buffer.get(it) }
            }
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        const val ASSET = "voicesplit/umx-hq-vocals.onnx"
        const val CHANNELS = 2
    }
}
