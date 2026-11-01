package com.petruvideo.core.export

import android.content.ContentUris
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.Robot
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Exports real projects on the device and checks the files; also the gallery saving and cancel cleanup. */
class ExportPipelineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = File(context.cacheDir, "export-test-${System.nanoTime()}")
    private val savedNames = mutableListOf<String>()

    private class Repo : ExportRepository {
        override suspend fun settings(projectId: String) = null
        override suspend fun saveSettings(projectId: String, settings: ExportSettings) = Unit
        override suspend fun job(projectId: String): StoredJob? = null
        override suspend fun saveJob(job: StoredJob) = Unit
        override suspend fun markInterrupted() = 0
        val records = mutableListOf<com.petruvideo.core.model.export.ExportRecord>()
        override suspend fun addRecord(record: com.petruvideo.core.model.export.ExportRecord) {
            records += record
        }
    }

    private val encoders = object : EncoderCapabilities {
        override val limits = DeviceLimits(2160, 60)
        override val bitrateRange = 500_000..40_000_000
    }

    @Before
    fun setUp() {
        cache.mkdirs()
    }

    @After
    fun tearDown() {
        scope.cancel()
        cache.deleteRecursively()
        savedNames.forEach { name ->
            context.contentResolver.delete(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Video.Media.DISPLAY_NAME} = ?", arrayOf(name),
            )
        }
    }

    private fun item(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    private fun project(vararg clips: VideoClip, media: List<MediaItem>, audio: List<AudioClip> = emptyList()) = Timeline(
        projectId = "p",
        format = ProjectFormat(320, 180, 30),
        media = media.associateBy { it.id },
        videoLane = clips.toList(),
        audioLane = audio,
    )

    private val repo = Repo()

    private fun coordinator(saver: ExportSaver = GallerySaver(context)) =
        ExportCoordinator(TransformerRunner(context), saver, repo, encoders, { Long.MAX_VALUE }, cache, scope)

    private fun waitForEnd(c: ExportCoordinator, timeoutMs: Long = 120_000): ExportState {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val state = c.job.value?.state
            if (state != null && !state.active) return state
            Thread.sleep(100)
        }
        error("export did not finish: ${c.job.value?.state}")
    }

    private fun retriever(uri: String) = MediaMetadataRetriever().apply { setDataSource(context, Uri.parse(uri)) }

    private fun MediaMetadataRetriever.int(key: Int) = extractMetadata(key)?.toInt() ?: 0

    private var lastTimes: List<Long> = emptyList()

    private fun videoSamples(uri: String): Int {
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(uri), null) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")
        }
        extractor.selectTrack(track)
        var count = 0
        val times = mutableListOf<Long>()
        while (extractor.sampleTime >= 0) {
            count++
            if (times.size < 10) times += extractor.sampleTime / 1000
            extractor.advance()
        }
        lastTimes = times
        extractor.release()
        return count
    }

    private fun export(timeline: Timeline, settings: ExportSettings, name: String): Pair<ExportState, String?> {
        val saver = object : ExportSaver {
            val inner = GallerySaver(context)
            override suspend fun save(file: File, displayName: String): SavedVideo? =
                inner.save(file, displayName).also { savedNames += displayName }
        }
        val c = coordinator(saver)
        c.start("p", name, timeline, settings) shouldBe null
        val end = waitForEnd(c)
        return end to (end as? ExportState.Finished)?.uri
    }

    @Test
    fun aSpedUpClipAndAPortraitClipExportWithTheRightLengthSizeAndSound() {
        val a = item("video_with_audio.mp4")
        val p = item("video_portrait.mp4")
        val timeline = project(
            VideoClip("c1", a.id, 0, 2_000, true, speedMilli = 2000),
            VideoClip("c2", p.id, 0, 2_000, p.hasAudio),
            media = listOf(a, p),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-a")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        val duration = r.int(MediaMetadataRetriever.METADATA_KEY_DURATION)
        (duration in 2_930..3_100) shouldBe true
        val w = r.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val h = r.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        minOf(w, h) shouldBe 720
        (maxOf(w, h) * 9 / 16 - 720 in -2..2) shouldBe true
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
    }

    @Test
    fun aMutedProjectExportsWithTheRightLength() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true, muted = true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-mute")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_100) shouldBe true
        r.release()
    }

    @Test
    fun theFrameRateAndLengthMatchWhenTheRateEqualsTheFootage() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-30")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_100) shouldBe true
        r.release()
        val samples = videoSamples(uri)
        check(samples in 56..64) { "video samples = $samples, first times (ms) = $lastTimes" }
    }

    @Test
    fun reverbMusicAndAnAudioLaneExportWithSound() {
        val a = item("video_silent.mp4")
        val music = item("audio.mp3")
        val hall = ReverbPresets.settings(ReverbPreset.Hall, true)
        val audio = AudioClip(
            "m1", music.id, 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, reverb = hall, volumePercent = 150,
        )
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a, music), audio = listOf(audio))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-rv")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
    }

    @Test
    fun theGalleryRowIsPublishedNotPendingAndLivesInThePetruVideoAlbum() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-gallery")
        state.shouldBeFinished()
        val id = ContentUris.parseId(Uri.parse(uri))
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.IS_PENDING, MediaStore.Video.Media.RELATIVE_PATH),
            "${MediaStore.Video.Media._ID} = ?", arrayOf(id.toString()), null,
        )!!.use {
            it.moveToFirst() shouldBe true
            it.getInt(0) shouldBe 0
            it.getString(1).startsWith("Movies/PetruVideo") shouldBe true
        }
        File(cache, "${(state as ExportState.Finished).uri.hashCode()}").exists() shouldBe false
        cache.listFiles().orEmpty().none { it.name.endsWith(".mp4") } shouldBe true
    }

    @Test
    fun aFinishedExportAddsOneHistoryRecordWithTheSavedVideo() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-history")
        state.shouldBeFinished()
        val record = repo.records.single()
        record.uri shouldBe uri
        record.projectName shouldBe "pipeline-history"
        (record.bytes > 0 && record.durationMs in 900..1_200) shouldBe true
        record.settings.quality shouldBe Quality.Smaller
    }

    @Test
    fun aMissingFileFailsSavingWithoutLeavingAGalleryRow() {
        val saver = GallerySaver(context)
        val ghost = File(cache, "ghost.mp4")
        val name = "ghost-${System.nanoTime()}.mp4"
        val result = runBlocking { saver.save(ghost, name) }
        result shouldBe null
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME} = ?", arrayOf(name), null,
        )!!.use { it.count shouldBe 0 }
    }

    @Test
    fun cancelStopsWithinTwoSecondsAndLeavesNothingBehind() {
        val a = item("video_with_audio.mp4")
        val clips = (0 until 6).map { VideoClip("c$it", a.id, 0, 2_000, true) }
        val timeline = project(*clips.toTypedArray(), media = listOf(a))
        val name = "cancel-test"
        val c = coordinator(object : ExportSaver {
            override suspend fun save(file: File, displayName: String): SavedVideo? {
                savedNames += displayName
                return GallerySaver(context).save(file, displayName)
            }
        })
        c.start("p", name, timeline, ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Higher)) shouldBe null
        val begin = System.currentTimeMillis()
        while (c.job.value?.state !is ExportState.Rendering && System.currentTimeMillis() - begin < 30_000) Thread.sleep(50)
        val cancelAt = System.currentTimeMillis()
        c.cancel()
        val end = waitForEnd(c, 10_000)
        end shouldBe ExportState.Cancelled
        ((System.currentTimeMillis() - cancelAt) < 2_000) shouldBe true
        cache.listFiles().orEmpty().none { it.name.endsWith(".mp4") } shouldBe true
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?", arrayOf("$name%"), null,
        )!!.use { it.count shouldBe 0 }
    }

    @Test
    fun theSizeEstimateIsCloseToTheRealFileSize() {
        // A software encoder (the emulator's) does not hold the bitrate on simple footage; SC-003 is judged on hardware.
        assumeTrue("needs a hardware H.264 encoder", hasHardwareAvcEncoder())
        val a = item("video_with_audio.mp4")
        val timeline = project(
            VideoClip("c1", a.id, 0, 2_000, true),
            VideoClip("c2", a.id, 0, 2_000, true),
            media = listOf(a),
        )
        val ratios = listOf(
            ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller),
            ExportSettings(Resolution.R720, FrameRate.F30, Quality.Higher),
            ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended),
        ).map { settings ->
            val c = coordinator()
            val estimate = c.plan(timeline, settings).estimatedBytes
            c.start("p", "pipeline-size", timeline, settings) shouldBe null
            val end = waitForEnd(c) as ExportState.Finished
            savedNames += "pipeline-size-${c.job.value!!.startedAt}.mp4"
            end.bytes.toDouble() / estimate
        }
        check(ratios.all { it in 0.85..1.15 }) { "real size / estimate = $ratios" }
    }

    @Test
    fun audioToolsExportWithTheFadesAndSoundOfThePreview() {
        val a = item("video_silent.mp4")
        val music = item("audio.mp3")
        val fx = AudioFx(
            fade = Fade(600, 600),
            noise = NoiseReduction(true, NoiseStrength.Medium),
            eq = EqPresets.settings(EqPreset.Warm),
            voice = VoiceFx(Echo(true, 150, 2), Robot(), Pitch(true, 3), Bass(true, 40)),
        )
        val audio = AudioClip("m1", music.id, 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, fx = fx)
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a, music), audio = listOf(audio))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-fx")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_150) shouldBe true
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
        val rms = audioRms(uri, listOf(0L to 60L, 900L to 1_100L, 1_940L to 2_000L))
        check(rms[0] < rms[1] * 0.35f) { "start level ${rms[0]} should be far below the middle ${rms[1]}" }
        check(rms[2] < rms[1] * 0.35f) { "end level ${rms[2]} should be far below the middle ${rms[1]}" }
    }

    /** Decodes the audio track and returns the RMS (0..1) of each [windows] range in milliseconds. */
    private fun audioRms(uri: String, windows: List<Pair<Long, Long>>): List<Float> {
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(uri), null) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")
        }
        val format = extractor.getTrackFormat(track)
        extractor.selectTrack(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        val sums = DoubleArray(windows.size)
        val counts = LongArray(windows.size)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val out = codec.dequeueOutputBuffer(info, 10_000)
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else if (out >= 0) {
                val shorts = codec.getOutputBuffer(out)!!.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
                var frame = 0L
                while (shorts.remaining() >= channels) {
                    val timeMs = info.presentationTimeUs / 1000 + frame * 1000 / rate
                    var sample = 0.0
                    repeat(channels) { sample += shorts.get() / 32768.0 }
                    sample /= channels
                    windows.forEachIndexed { i, (from, to) ->
                        if (timeMs in from until to) {
                            sums[i] += sample * sample
                            counts[i]++
                        }
                    }
                    frame++
                }
                codec.releaseOutputBuffer(out, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return sums.indices.map { if (counts[it] == 0L) 0f else kotlin.math.sqrt(sums[it] / counts[it]).toFloat() }
    }

    private fun textClip(id: String, start: Long, duration: Long, x: Int = 500, text: String = "TITLE") = TextClip(
        id, text, start, duration, xPermille = x, yPermille = 500, sizePermille = 200,
        style = TextStyle(
            colorArgb = 0xFFFFFFFF.toInt(),
            background = TextBackground(true, 0xFF000000.toInt(), 100),
        ),
    )

    private fun frameAt(uri: String, ms: Long): android.graphics.Bitmap {
        val r = retriever(uri)
        val frame = r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST)!!
        r.release()
        return frame
    }

    /** Pixels whose color differs clearly between two frames of the same size. */
    private fun changedPixels(a: android.graphics.Bitmap, b: android.graphics.Bitmap, left: Float = 0f, right: Float = 1f): Int {
        var count = 0
        for (y in 0 until a.height step 2) {
            for (x in (a.width * left).toInt() until (a.width * right).toInt() step 2) {
                val p = a.getPixel(x, y)
                val q = b.getPixel(x, y)
                val d = kotlin.math.abs(android.graphics.Color.red(p) - android.graphics.Color.red(q)) +
                    kotlin.math.abs(android.graphics.Color.green(p) - android.graphics.Color.green(q)) +
                    kotlin.math.abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(q))
                if (d > 150) count++
            }
        }
        return count
    }

    /** Pixels that are nearly white in a frame; the title's white letters on its black box. */
    private fun whitePixels(frame: android.graphics.Bitmap, left: Float = 0f, right: Float = 1f): Int {
        var count = 0
        for (y in 0 until frame.height step 2) {
            for (x in (frame.width * left).toInt() until (frame.width * right).toInt() step 2) {
                val c = frame.getPixel(x, y)
                if (android.graphics.Color.red(c) > 235 && android.graphics.Color.green(c) > 235 &&
                    android.graphics.Color.blue(c) > 235
                ) count++
            }
        }
        return count
    }

    @Test
    fun textShowsOnlyInsideItsWindowAndAtItsPlace() {
        val a = item("video_silent.mp4")
        val plain = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a))
        val titled = plain.copy(textLane = listOf(textClip("t1", 600, 800, x = 250)))
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val (s1, withoutText) = export(plain, settings, "pipeline-notext")
        val (s2, withText) = export(titled, settings, "pipeline-text")
        s1.shouldBeFinished()
        s2.shouldBeFinished()
        // The same footage with and without the title: frames match outside the window and differ inside, on the left.
        val outside = changedPixels(frameAt(withoutText!!, 300), frameAt(withText!!, 300)) +
            changedPixels(frameAt(withoutText, 1_700), frameAt(withText, 1_700))
        val inside = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000))
        check(inside > 1_500 && outside < inside / 5) { "changed pixels inside $inside, outside $outside" }
        val left = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000), 0f, 0.5f)
        val right = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000), 0.5f, 1f)
        check(left > right * 3) { "text should sit on the left half: left $left right $right" }
    }

    @Test
    fun textAfterTheVideoExtendsTheProjectOverBlack() {
        val a = item("video_silent.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, false), media = listOf(a)).copy(
            textLane = listOf(textClip("t1", 1_200, 1_300)),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-text-tail")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 2_400..2_650) shouldBe true
        r.release()
        (whitePixels(frameAt(uri, 2_000)) > 800) shouldBe true
    }

    @Test
    fun aProjectWithOnlyTextExports() {
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            textLane = listOf(textClip("t1", 0, 2_000)),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-text-only")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_900..2_200) shouldBe true
        r.release()
        (whitePixels(frameAt(uri, 1_000)) > 800) shouldBe true
    }

    private fun meanLuma(frame: android.graphics.Bitmap, l: Float = 0f, t: Float = 0f, r: Float = 1f, b: Float = 1f): Float {
        var sum = 0.0
        var n = 0
        for (y in (frame.height * t).toInt() until (frame.height * b).toInt() step 2) {
            for (x in (frame.width * l).toInt() until (frame.width * r).toInt() step 2) {
                val c = frame.getPixel(x, y)
                sum += 0.2126 * android.graphics.Color.red(c) + 0.7152 * android.graphics.Color.green(c) +
                    0.0722 * android.graphics.Color.blue(c)
                n++
            }
        }
        return (sum / n).toFloat()
    }

    private fun filtered(filter: ClipFilter, name: String): String {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true, filter = filter), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        state.shouldBeFinished()
        return uri!!
    }

    @Test
    fun filtersChangeTheExportedPictureAsTheMatrixPredicts() {
        val plain = frameAt(filtered(ClipFilter(), "pipeline-flt-plain"), 1_000)
        val bright = frameAt(filtered(ClipFilter(brightness = 100), "pipeline-flt-bright"), 1_000)
        val mono = frameAt(filtered(ClipFilter(look = FilterLook.BlackAndWhite), "pipeline-flt-bw"), 1_000)
        val vignette = frameAt(filtered(ClipFilter(vignette = 100), "pipeline-flt-vig"), 1_000)
        check(meanLuma(bright) > meanLuma(plain) + 25) { "bright ${meanLuma(bright)} vs plain ${meanLuma(plain)}" }
        var spread = 0
        for (y in 0 until mono.height step 8) for (x in 0 until mono.width step 8) {
            val c = mono.getPixel(x, y)
            spread += maxOf(
                kotlin.math.abs(android.graphics.Color.red(c) - android.graphics.Color.green(c)),
                kotlin.math.abs(android.graphics.Color.green(c) - android.graphics.Color.blue(c)),
            )
        }
        val samples = (mono.height / 8) * (mono.width / 8)
        check(spread / samples < 12) { "black and white keeps color: mean channel spread ${spread / samples}" }
        val cornersPlain = meanLuma(plain, 0f, 0f, 0.15f, 0.15f) + meanLuma(plain, 0.85f, 0.85f, 1f, 1f)
        val cornersVignette = meanLuma(vignette, 0f, 0f, 0.15f, 0.15f) + meanLuma(vignette, 0.85f, 0.85f, 1f, 1f)
        check(cornersVignette < cornersPlain * 0.6f) { "corners $cornersVignette vs $cornersPlain" }
        val centerPlain = meanLuma(plain, 0.4f, 0.4f, 0.6f, 0.6f)
        val centerVignette = meanLuma(vignette, 0.4f, 0.4f, 0.6f, 0.6f)
        check(kotlin.math.abs(centerVignette - centerPlain) < 12f) { "center $centerVignette vs $centerPlain" }
    }

    @Test
    fun aNeutralFilterExportsLikeNoFilter() {
        val a = filtered(ClipFilter(look = FilterLook.Vivid, intensity = 0), "pipeline-flt-neutral")
        val b = filtered(ClipFilter(), "pipeline-flt-none")
        check(kotlin.math.abs(meanLuma(frameAt(a, 1_000)) - meanLuma(frameAt(b, 1_000))) < 1.5f)
    }

    @Test
    fun aPhotoWithAFilterExports() {
        val photo = item("photo.jpg")
        val timeline = project(
            VideoClip("p1", photo.id, 0, 1_500, false, filter = ClipFilter(brightness = 100)), media = listOf(photo),
        )
        val plain = project(VideoClip("p1", photo.id, 0, 1_500, false), media = listOf(photo))
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val (s1, u1) = export(timeline, settings, "pipeline-photo-flt")
        val (s2, u2) = export(plain, settings, "pipeline-photo-plain")
        check(s2 is ExportState.Finished) { "plain photo export: $s2" }
        check(s1 is ExportState.Finished) { "filtered photo export: $s1" }
        check(meanLuma(frameAt(u1!!, 700)) > meanLuma(frameAt(u2!!, 700)) + 25)
    }

    private fun hasHardwareAvcEncoder() = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any {
        it.isEncoder && it.isHardwareAccelerated && it.supportedTypes.any { t -> t.equals("video/avc", true) }
    }

    private fun ExportState.shouldBeFinished() {
        (this is ExportState.Finished) shouldBe true
    }
}
