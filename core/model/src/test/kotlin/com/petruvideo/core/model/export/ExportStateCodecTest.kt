package com.petruvideo.core.model.export

import io.kotest.matchers.shouldBe
import org.junit.Test

class ExportStateCodecTest {
    @Test
    fun `every state survives a round trip`() {
        listOf(
            ExportState.Waiting,
            ExportState.Rendering(42),
            ExportState.Finished("content://media/1", 9_000, 12_000),
            ExportState.Cancelled,
            ExportState.Failed(ExportFailure.MissingMedia("a: b.mp4")),
            ExportState.Failed(ExportFailure.NotEnoughStorage(123_456)),
            ExportState.Failed(ExportFailure.EncoderError("codec died")),
            ExportState.Failed(ExportFailure.Interrupted),
            ExportState.Failed(ExportFailure.Pressure),
            ExportState.Failed(ExportFailure.SaveFailed),
        ).forEach { ExportStateCodec.decode(ExportStateCodec.encode(it)) shouldBe it }
    }

    @Test
    fun `active states are Waiting and Rendering only`() {
        ExportState.Waiting.active shouldBe true
        ExportState.Rendering(1).active shouldBe true
        ExportState.Cancelled.active shouldBe false
        ExportState.Failed(ExportFailure.Pressure).active shouldBe false
    }
}
