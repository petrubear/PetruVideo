package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.template.TemplateCatalog
import com.petruvideo.core.model.template.TemplateFiller
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays projects with transitions and clip effects through the preview engine (feature 011). */
@OptIn(UnstableApi::class)
class TransitionPlaybackTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var engine: Media3PreviewEngine

    @Before
    fun setUp() {
        instrumentation.runOnMainSync { engine = Media3PreviewEngine(context) }
    }

    @After
    fun tearDown() = instrumentation.runOnMainSync { engine.release() }

    private fun item(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun waitFor(timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end && !condition()) Thread.sleep(50)
        check(condition()) { "state ${engine.state.value}" }
    }

    private fun project(a: MediaItem, b: MediaItem, kind: TransitionKind, vararg overlays: OverlayClip, extra: List<MediaItem> = emptyList()) =
        Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = (listOf(a, b) + extra).associateBy { it.id },
            videoLane = listOf(
                VideoClip("c1", a.id, 0, 1_500, true, transitionOut = Transition(kind, 500)),
                VideoClip("c2", b.id, 0, 1_500, false, effects = listOf(ClipEffect(EffectKind.Blur, 40))),
            ),
            overlayLane = overlays.toList(),
        )

    @Test
    fun aTransitionPlaysToTheEndAndTheProjectIsShorterByItsLength() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        onMain { engine.load(project(a, b, TransitionKind.Dissolve), PreviewQuality.Full) }
        engine.state.value.durationMs shouldBe 2_500
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 400 }
        onMain { engine.setClipEffectLive("c2", ClipEffect(EffectKind.Blur, 90)) }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun everyKindPlaysAndATransitionWorksTogetherWithAnOverlay() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        val photo = item("photo.jpg")
        fun timeline(kind: TransitionKind) = project(
            a, b, kind,
            *(if (kind == TransitionKind.Slide) arrayOf(OverlayClip("o1", photo.id, 0, 1_000, 300)) else emptyArray()),
            extra = listOf(photo),
        )
        onMain { engine.load(timeline(TransitionKind.Dissolve), PreviewQuality.Full) }
        onMain { engine.play() }
        // Each change of kind rebuilds the composition while the preview plays.
        for (kind in TransitionKind.entries.drop(1)) {
            waitFor { engine.state.value.isPlaying }
            onMain { engine.load(timeline(kind), PreviewQuality.Full) }
            Thread.sleep(300)
            engine.state.value.error shouldBe null
        }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun threeClipsWithTwoTransitionsPlayToTheEnd() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = listOf(a, b).associateBy { it.id },
            videoLane = listOf(
                VideoClip("c1", a.id, 0, 1_500, true, transitionOut = Transition(TransitionKind.Dissolve, 500)),
                VideoClip("c2", b.id, 0, 1_500, false, transitionOut = Transition(TransitionKind.Dissolve, 500)),
                VideoClip("c3", a.id, 0, 1_500, true),
            ),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended || engine.state.value.error != null }
        engine.state.value.error shouldBe null
    }

    @Test
    fun everyEffectOnAMiddleClipPlaysThroughTwoTransitions() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        for (kind in EffectKind.entries) {
            val timeline = Timeline(
                projectId = "p", format = ProjectFormat(320, 180, 30),
                media = listOf(a, b).associateBy { it.id },
                videoLane = listOf(
                    VideoClip("c1", a.id, 0, 1_500, true, transitionOut = Transition(TransitionKind.Dissolve, 500)),
                    VideoClip(
                        "c2", b.id, 0, 1_500, false, transitionOut = Transition(TransitionKind.Dissolve, 500),
                        effects = listOf(ClipEffect(kind, 60)),
                    ),
                    VideoClip("c3", a.id, 0, 1_500, true),
                ),
            )
            onMain { engine.load(timeline, PreviewQuality.Full) }
            onMain { engine.play() }
            waitFor { engine.state.value.ended || engine.state.value.error != null }
            check(engine.state.value.error == null) { "$kind: ${engine.state.value.error}" }
        }
    }

    @Test
    fun twoPiecesOfOneSourceWithFadeBlackAndEffectsPlayToTheEnd() {
        val a = item("video_with_audio.mp4")
        val total = a.durationMs!!
        val cut = total / 3
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = mapOf(a.id to a),
            videoLane = listOf(
                VideoClip(
                    "c1", a.id, 0, cut, true, transitionOut = Transition(TransitionKind.FadeBlack, 1_000),
                    effects = listOf(ClipEffect(EffectKind.Pixelate, 50)),
                ),
                VideoClip("c2", a.id, cut, total, true, effects = listOf(ClipEffect(EffectKind.Mirror, 50))),
            ),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended || engine.state.value.error != null }
        engine.state.value.error shouldBe null
    }

    @Test
    fun playingFromTheMiddleOfAProjectWithATransitionReachesTheEnd() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = listOf(a, b).associateBy { it.id },
            videoLane = listOf(
                VideoClip("c1", a.id, 0, 1_500, true, transitionOut = Transition(TransitionKind.FadeBlack, 1_000)),
                VideoClip("c2", b.id, 0, 2_000, false, effects = listOf(ClipEffect(EffectKind.Mirror, 50))),
            ),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        // Each run ends the project, so every start after the first is a seek back from the end.
        for (from in listOf(300L, 1_000L, 2_000L, 0L)) {
            onMain { engine.seekTo(from) }
            onMain { engine.play() }
            waitFor { engine.state.value.ended || engine.state.value.error != null }
            check(engine.state.value.error == null) { "from $from: ${engine.state.value.error}" }
        }
    }

    @Test
    fun threeSegmentsOfOneSourceEachWithItsOwnEffectAndTransitionPlayToTheEnd() {
        val a = item("video_with_audio.mp4")
        val total = a.durationMs!!
        val p1 = total / 3
        val p2 = total * 2 / 3
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = mapOf(a.id to a),
            videoLane = listOf(
                VideoClip(
                    "c1", a.id, 0, p1, true, transitionOut = Transition(TransitionKind.FadeBlack, 500),
                    effects = listOf(ClipEffect(EffectKind.Pixelate, 50)),
                ),
                VideoClip(
                    "c2", a.id, p1, p2, true, transitionOut = Transition(TransitionKind.Slide, 500),
                    effects = listOf(ClipEffect(EffectKind.Mirror, 50)),
                ),
                VideoClip(
                    "c3", a.id, p2, total, true, transitionOut = Transition(TransitionKind.Dissolve, 500),
                    effects = listOf(ClipEffect(EffectKind.Glitch, 50)),
                ),
            ),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended || engine.state.value.error != null }
        engine.state.value.error shouldBe null
    }

    @Test
    fun threeFullHdSegmentsAt50FpsEachWithItsOwnEffectAndTransitionPlayToTheEnd() {
        val a = item("video_1080p50.mp4")
        val total = a.durationMs!!
        val p1 = total / 3
        val p2 = total * 2 / 3
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(1080, 1920, 30),
            media = mapOf(a.id to a),
            videoLane = listOf(
                VideoClip(
                    "c1", a.id, 0, p1, true, transitionOut = Transition(TransitionKind.FadeBlack, 1_000),
                    effects = listOf(ClipEffect(EffectKind.Pixelate, 50)),
                ),
                VideoClip(
                    "c2", a.id, p1, p2, true, transitionOut = Transition(TransitionKind.Slide, 1_000),
                    effects = listOf(ClipEffect(EffectKind.Mirror, 50)),
                ),
                VideoClip(
                    "c3", a.id, p2, total, true, transitionOut = Transition(TransitionKind.Dissolve, 1_000),
                    effects = listOf(ClipEffect(EffectKind.Glitch, 50)),
                ),
            ),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended || engine.state.value.error != null }
        engine.state.value.error shouldBe null
    }

    @Test
    fun everyTemplateFilledWithOneOrTwoVideosPlaysToTheEnd() {
        val a = item("video_1080p50.mp4")
        val b = item("video_with_audio.mp4")
        var n = 0
        for (template in TemplateCatalog.all) {
            for (items in listOf(listOf(a), listOf(a, b))) {
                val fill = TemplateFiller.fill(template, items, "p") { "id${n++}" }!!
                onMain { engine.load(fill.timeline, PreviewQuality.Full) }
                onMain { engine.play() }
                waitFor(60_000) { engine.state.value.ended || engine.state.value.error != null }
                check(engine.state.value.error == null) {
                    "${template.id} with ${items.size}: ${engine.state.value.error}"
                }
            }
        }
    }
}
