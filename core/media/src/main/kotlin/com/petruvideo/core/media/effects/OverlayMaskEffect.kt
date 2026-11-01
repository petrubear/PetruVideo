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
import com.petruvideo.core.model.OverlayMask

/**
 * The mask of one overlay clip as one shader pass: the picture is kept inside a shape centred on it (outside, when
 * inverted) with a soft edge. The values are read at every frame, so a slider drag needs no rebuild.
 */
@OptIn(UnstableApi::class)
class OverlayMaskHandle(mask: OverlayMask) {
    @Volatile private var current: OverlayMask = mask

    val mask: OverlayMask get() = current

    fun update(next: OverlayMask) {
        current = next
    }

    val glEffect: GlEffect = GlEffect { _: Context, _: Boolean -> MaskShader(this) }
}

@OptIn(UnstableApi::class)
private class MaskShader(private val handle: OverlayMaskHandle) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    override fun configure(inputWidth: Int, inputHeight: Int) = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            val mask = handle.mask
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatUniform("uKind", mask.kind.ordinal.toFloat())
            program.setFloatUniform("uSize", mask.size / PERCENT)
            program.setFloatUniform("uFeather", maxOf(mask.feather / PERCENT * MAX_FEATHER, MIN_FEATHER))
            program.setFloatUniform("uInvert", if (mask.invert) 1f else 0f)
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

    private companion object {
        const val PERCENT = 100f
        const val MAX_FEATHER = 0.5f
        const val MIN_FEATHER = 0.002f
        const val VERTEX_COUNT = 4

        // The shader's kind numbers are MaskKind.ordinal: 0 None, 1 Circle, 2 Rectangle, 3 Linear.
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
            uniform float uKind;
            uniform float uSize;
            uniform float uFeather;
            uniform float uInvert;
            varying vec2 vUv;

            void main() {
              vec4 px = texture2D(uTexSampler, vUv);
              vec2 p = (vUv - 0.5) * 2.0;
              float d = 0.0;
              if (uKind < 1.5) {
                d = length(p) - uSize;
              } else if (uKind < 2.5) {
                d = max(abs(p.x), abs(p.y)) - uSize;
              } else {
                d = vUv.x - mix(-uFeather, 1.0 + uFeather, uSize);
              }
              float inside = 1.0 - smoothstep(-uFeather, uFeather, d);
              float cover = mix(inside, 1.0 - inside, uInvert);
              gl_FragColor = vec4(px.rgb, px.a * cover);
            }
        """
    }
}
