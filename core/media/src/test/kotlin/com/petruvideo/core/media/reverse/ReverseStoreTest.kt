package com.petruvideo.core.media.reverse

import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Test

class ReverseStoreTest {
    private fun media(id: String, size: Long) = MediaItem(
        id, MediaKind.Video, "content://$id", "$id.mp4", 60_000, 1920, 1080, 30f, true,
        Fingerprint(size, 1, 60_000, "video/mp4"),
    )

    private val root = createTempDirectory("reversed").toFile()
    private val store = ReverseStore(root)
    private val a = media("a", 100)
    private val b = media("b", 200)

    private fun make(m: MediaItem, s: Long, e: Long) = store.newFile(m, s, e).also { it.writeText("x") }

    @Test
    fun `a copy serves any range inside it and the file range is mirrored`() {
        make(a, 10_000, 20_000)
        store.find(a, 10_000, 20_000)!!.endMs shouldBe 20_000
        val inner = store.find(a, 12_000, 18_000)!!
        inner.fileRange(12_000, 18_000) shouldBe (2_000L..8_000L)
        store.find(a, 9_000, 18_000) shouldBe null
        store.find(a, 12_000, 21_000) shouldBe null
        store.find(b, 12_000, 18_000) shouldBe null
    }

    @Test
    fun `the tightest copy that covers the range is used`() {
        make(a, 0, 30_000)
        make(a, 10_000, 20_000)
        store.find(a, 12_000, 18_000)!!.startMs shouldBe 10_000
    }

    @Test
    fun `sweep deletes copies nothing uses and keeps the ones a clip needs`() {
        val keep = make(a, 10_000, 20_000)
        val drop = make(a, 40_000, 50_000)
        val other = make(b, 0, 5_000)
        store.sweep(listOf(ReverseNeed(a, 12_000, 18_000)))
        keep.exists() shouldBe true
        drop.exists() shouldBe false
        other.exists() shouldBe false
        File(root, "junk.mp4").also { it.writeText("x") }
        store.sweep(emptyList())
        root.listFiles()!!.isEmpty() shouldBe true
    }
}
