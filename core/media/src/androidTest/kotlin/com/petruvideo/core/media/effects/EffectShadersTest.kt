package com.petruvideo.core.media.effects

import android.graphics.Bitmap
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.annotation.OptIn
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petruvideo.core.model.EffectKind
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Compiles every picture effect on the device's GL driver and renders the new ones (feature 035): no change at
 * strength 0, a visible change at full strength.
 */
@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class EffectShadersTest {
    private lateinit var display: EGLDisplay
    private lateinit var context: EGLContext
    private val size = 64
    private val added = EffectKind.entries.drop(9)

    @Before
    fun openGl() {
        display = GlUtil.getDefaultEglDisplay()
        context = GlUtil.createEglContext(display)
        GlUtil.createFocusedPlaceholderEglSurface(context, display)
    }

    @After
    fun closeGl() {
        GlUtil.destroyEglContext(display, context)
    }

    @Test
    fun everyEffectProgramCompilesAndLinks() {
        for (kind in EffectKind.entries) {
            val program = GlProgram(EffectShaderSources.VERTEX, EffectShaderSources.fragmentFor(kind))
            program.delete()
        }
    }

    @Test
    fun theNewEffectsLeaveThePictureAloneAtZeroAndChangeItAtFullStrength() {
        val input = render(null, 0f, 0f)
        val tooMuchChange = mutableListOf<String>()
        val noChange = mutableListOf<String>()
        for (kind in added) {
            if (differing(input, render(kind, 0f, 0.1f)) > 0.01f) tooMuchChange += kind.name
            val changed = listOf(0.1f, 0.3f).maxOf { differing(input, render(kind, 1f, it)) }
            if (changed < 0.02f) noChange += kind.name
        }
        tooMuchChange shouldBe emptyList<String>()
        noChange shouldBe emptyList<String>()
    }

    /** The fraction of bytes that differ by more than 3 levels. */
    private fun differing(a: ByteArray, b: ByteArray): Float {
        var count = 0
        for (i in a.indices) if (abs((a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)) > 3) count++
        return count.toFloat() / a.size
    }

    /** Draws the test picture through [kind] (or plainly when null) and returns its RGBA bytes. */
    private fun render(kind: EffectKind?, strength: Float, seconds: Float): ByteArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) {
            val checker = if ((x / 8 + y / 8) % 2 == 0) 40 else 0
            val color = (0xFF shl 24) or ((x * 4 + checker).coerceAtMost(255) shl 16) or
                ((y * 4).coerceAtMost(255) shl 8) or (255 - x * 2 - y * 2).coerceIn(0, 255)
            bitmap.setPixel(x, y, color)
        }
        val textures = IntArray(2)
        GLES20.glGenTextures(2, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, size, size, 0, GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE, null)
        val fbo = IntArray(1)
        GLES20.glGenFramebuffers(1, fbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
            textures[1], 0)
        GLES20.glViewport(0, 0, size, size)
        val effect = kind ?: EffectKind.Letterbox
        val program = GlProgram(EffectShaderSources.VERTEX, EffectShaderSources.fragmentFor(effect))
        program.use()
        program.setSamplerTexIdUniform("uTexSampler", textures[0], 0)
        program.setFloatUniform("uStrength", if (kind == null) 0f else strength)
        if (effect in EffectShaderSources.USES_ASPECT) program.setFloatUniform("uAspect", 1f)
        if (effect in EffectShaderSources.USES_TIME) program.setFloatUniform("uTime", seconds)
        program.setBufferAttribute(
            "aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
        )
        program.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        val pixels = ByteBuffer.allocateDirect(size * size * 4)
        GLES20.glReadPixels(0, 0, size, size, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        program.delete()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteFramebuffers(1, fbo, 0)
        GLES20.glDeleteTextures(2, textures, 0)
        return ByteArray(size * size * 4).also { pixels.get(it) }
    }
}
