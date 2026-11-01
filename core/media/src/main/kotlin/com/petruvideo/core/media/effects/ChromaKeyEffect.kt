package com.petruvideo.core.media.effects

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import com.petruvideo.core.model.ChromaKey

/**
 * The chroma key of one overlay clip as one shader pass: pixels near the key colour (compared by chroma, so shading
 * matters little) become transparent. The settings are read at every frame, so a slider drag needs no rebuild.
 * [checker] draws a checkerboard in the see-through areas; it is only turned on for the preview.
 */
@OptIn(UnstableApi::class)
class ChromaKeyHandle(key: ChromaKey) {
    @Volatile private var settings: ChromaKey = key
    @Volatile var checker: Boolean = false

    val current: ChromaKey get() = settings

    fun update(key: ChromaKey) {
        settings = key
    }

    val glEffect: GlEffect = GlEffect { _: Context, _: Boolean -> ChromaKeyShader(this) }
}

@OptIn(UnstableApi::class)
private class ChromaKeyShader(private val handle: ChromaKeyHandle) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }
    private var width = 1
    private var height = 1

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth
        height = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            val key = handle.current
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatsUniform("uKey", floatArrayOf(red(key.color), green(key.color), blue(key.color)))
            program.setFloatUniform("uThreshold", key.strength / PERCENT * MAX_THRESHOLD)
            program.setFloatUniform("uSoft", MIN_SOFT + key.smoothness / PERCENT * SOFT_RANGE)
            program.setFloatUniform("uSpill", key.spill / PERCENT)
            program.setFloatUniform("uChecker", if (handle.checker) 1f else 0f)
            program.setFloatsUniform("uCells", floatArrayOf(CELLS_ACROSS, CELLS_ACROSS * height / width))
            program.setBufferAttribute(
                "aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private fun red(color: Int) = ((color shr RED_SHIFT) and CHANNEL_MASK) / CHANNEL_MAX
    private fun green(color: Int) = ((color shr GREEN_SHIFT) and CHANNEL_MASK) / CHANNEL_MAX
    private fun blue(color: Int) = (color and CHANNEL_MASK) / CHANNEL_MAX

    private companion object {
        const val PERCENT = 100f
        const val MAX_THRESHOLD = 0.5f
        const val MIN_SOFT = 0.02f
        const val SOFT_RANGE = 0.2f
        const val CELLS_ACROSS = 24f
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val CHANNEL_MASK = 0xFF
        const val CHANNEL_MAX = 255f
        const val VERTEX_COUNT = 4

        const val VERTEX = """
            attribute vec4 aFramePosition;
            varying vec2 vUv;
            void main() {
              gl_Position = aFramePosition;
              vUv = aFramePosition.xy * 0.5 + 0.5;
            }
        """

        const val FRAGMENT = """
            precision highp float;
            uniform sampler2D uTexSampler;
            uniform vec3 uKey;
            uniform float uThreshold;
            uniform float uSoft;
            uniform float uSpill;
            uniform float uChecker;
            uniform vec2 uCells;
            varying vec2 vUv;

            vec2 chroma(vec3 c) {
              return vec2(-0.1687 * c.r - 0.3313 * c.g + 0.5 * c.b, 0.5 * c.r - 0.4187 * c.g - 0.0813 * c.b);
            }

            void main() {
              vec4 px = texture2D(uTexSampler, vUv);
              float dist = distance(chroma(px.rgb), chroma(uKey));
              float keep = smoothstep(uThreshold, uThreshold + uSoft, dist);
              // Near the key, the key's dominant channel is pulled down to the larger of the other two.
              float top = max(uKey.r, max(uKey.g, uKey.b));
              vec3 mask = vec3(float(uKey.r >= top), float(uKey.g >= top && uKey.r < top), float(uKey.b >= top && uKey.r < top && uKey.g < top));
              float others = max(max(px.r * (1.0 - mask.r), px.g * (1.0 - mask.g)), px.b * (1.0 - mask.b));
              float lead = dot(px.rgb, mask);
              float near = 1.0 - smoothstep(uThreshold + uSoft, uThreshold + uSoft + 0.25, dist);
              vec3 rgb = px.rgb - mask * max(lead - others, 0.0) * uSpill * near;
              float alpha = px.a * keep;
              if (uChecker > 0.5) {
                vec2 cell = floor(vUv * uCells);
                float tone = mod(cell.x + cell.y, 2.0) < 1.0 ? 0.8 : 0.55;
                gl_FragColor = vec4(mix(vec3(tone), rgb, alpha), 1.0);
              } else {
                gl_FragColor = vec4(rgb, alpha);
              }
            }
        """
    }
}
