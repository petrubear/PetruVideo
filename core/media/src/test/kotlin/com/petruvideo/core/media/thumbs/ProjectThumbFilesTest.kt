package com.petruvideo.core.media.thumbs

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Test

class ProjectThumbFilesTest {
    private val dir = createTempDirectory("thumbs").toFile()

    @Test
    fun theFileNameFollowsTheFirstClip() {
        val a = ProjectThumbFiles.fileFor(dir, "p1", "m1@0")
        ProjectThumbFiles.fileFor(dir, "p1", "m1@0") shouldBe a
        ProjectThumbFiles.fileFor(dir, "p1", "m2@0") shouldNotBe a
        ProjectThumbFiles.fileFor(dir, "p1", "m1@500") shouldNotBe a
        a.name.startsWith("p1-") shouldBe true
        a.name.endsWith(".png") shouldBe true
    }

    @Test
    fun cleanupKeepsTheCurrentFileAndOnlyThatProjectsOthers() {
        val current = File(dir, "p1-aaa.png").apply { writeText("x") }
        val old = File(dir, "p1-bbb.png").apply { writeText("x") }
        val legacy = File(dir, "p1.png").apply { writeText("x") }
        val other = File(dir, "p2-ccc.png").apply { writeText("x") }

        ProjectThumbFiles.deleteOthers(dir, "p1", keep = current)

        current.exists() shouldBe true
        old.exists() shouldBe false
        legacy.exists() shouldBe false
        other.exists() shouldBe true
    }

    @Test
    fun cleanupWithNothingToKeepRemovesEveryFileOfTheProject() {
        val a = File(dir, "p1-aaa.png").apply { writeText("x") }
        val other = File(dir, "p2-ccc.png").apply { writeText("x") }

        ProjectThumbFiles.deleteOthers(dir, "p1", keep = null)

        a.exists() shouldBe false
        other.exists() shouldBe true
    }
}
