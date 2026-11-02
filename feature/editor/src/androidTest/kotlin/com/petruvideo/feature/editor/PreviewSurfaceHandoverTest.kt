package com.petruvideo.feature.editor

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.feature.editor.ui.detachPlayer
import io.kotest.assertions.throwables.shouldThrow
import org.junit.Test

/** A fold or unfold swaps the preview view while the player lives on (the app used to crash on it). */
@OptIn(UnstableApi::class)
class PreviewSurfaceHandoverTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Test
    fun releasingTheOldViewAfterTheNewOneTookThePlayerDoesNotCrash() {
        var error: Throwable? = null
        onMain {
            val player = CompositionPlayer.Builder(context).build()
            val old = PlayerView(context).also { it.player = player }
            val new = PlayerView(context).also { it.player = player }
            error = runCatching { old.detachPlayer() }.exceptionOrNull()
            new.detachPlayer()
            player.release()
        }
        check(error == null) { "$error" }
    }

    @Test
    fun clearingTheOldViewDirectlyIsWhatUsedToCrash() {
        onMain {
            val player = CompositionPlayer.Builder(context).build()
            val old = PlayerView(context).also { it.player = player }
            PlayerView(context).also { it.player = player }
            shouldThrow<IllegalArgumentException> { old.player = null }
            player.release()
        }
    }
}
