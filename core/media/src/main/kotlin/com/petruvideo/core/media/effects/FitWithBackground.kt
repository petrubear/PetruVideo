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
import com.petruvideo.core.model.CanvasBackground
import kotlin.math.max
import kotlin.math.min

/**
 * Fits a clip into the project frame like the plain fit, and fills the rest of the frame with the project's background
 * (feature 021): a colour, or an enlarged blurred copy of the clip's own picture. One shader pass.
 */
@OptIn(UnstableApi::class)
class FitWithBackground(
    private val width: Int,
    private val height: Int,
    private val canvas: CanvasBackground,
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean) = FitShader(width, height, canvas)
}

@OptIn(UnstableApi::class)
class FitShader(
    private val width: Int,
    private val height: Int,
    private val canvas: CanvasBackground,
) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }
    private var fit = floatArrayOf(1f, 1f)
    private var cover = floatArrayOf(1f, 1f)

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        val fitScale = min(width.toFloat() / inputWidth, height.toFloat() / inputHeight)
        fit = floatArrayOf(inputWidth * fitScale / width, inputHeight * fitScale / height)
        val coverScale = max(width.toFloat() / inputWidth, height.toFloat() / inputHeight)
        cover = floatArrayOf(inputWidth * coverScale / width, inputHeight * coverScale / height)
        return Size(width, height)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatsUniform("uFit", fit)
            program.setFloatsUniform("uCover", cover)
            val colour = (canvas as? CanvasBackground.Colour)?.argb ?: 0
            program.setFloatsUniform(
                "uColor",
                floatArrayOf(
                    (colour shr RED_SHIFT and BYTE) / MAX, (colour shr GREEN_SHIFT and BYTE) / MAX,
                    (colour and BYTE) / MAX,
                ),
            )
            val blur = (canvas as? CanvasBackground.Blur)?.strength?.radiusPermille ?: 0
            program.setFloatUniform("uBlur", blur / PERMILLE)
            program.setFloatUniform("uAspect", height.toFloat() / width)
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
        const val VERTEX_COUNT = 4
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val BYTE = 0xFF
        const val MAX = 255f
        const val PERMILLE = 1000f

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
            uniform vec2 uFit;
            uniform vec2 uCover;
            uniform vec3 uColor;
            uniform float uBlur;
            uniform float uAspect;
            varying vec2 vUv;
            vec3 blurred(vec2 coverUv) {
              vec3 sum = texture2D(uTexSampler, clamp(coverUv, 0.0, 1.0)).rgb;
              float total = 1.0;
              for (int ring = 1; ring <= 2; ring++) {
                float radius = uBlur * float(ring) * 0.5;
                for (int i = 0; i < 6; i++) {
                  float angle = float(i) * 1.0471976;
                  vec2 offset = vec2(cos(angle) * uAspect, sin(angle)) * radius / uCover;
                  sum += texture2D(uTexSampler, clamp(coverUv + offset, 0.0, 1.0)).rgb;
                  total += 1.0;
                }
              }
              return sum / total * 0.85;
            }
            void main() {
              vec2 local = (vUv - 0.5) / uFit + 0.5;
              if (local.x >= 0.0 && local.x <= 1.0 && local.y >= 0.0 && local.y <= 1.0) {
                gl_FragColor = texture2D(uTexSampler, local);
              } else if (uBlur > 0.0) {
                gl_FragColor = vec4(blurred((vUv - 0.5) / uCover + 0.5), 1.0);
              } else {
                gl_FragColor = vec4(uColor, 1.0);
              }
            }
        """
    }
}
