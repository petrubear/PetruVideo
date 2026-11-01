package com.petruvideo.app

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.Test

/** The app works fully offline (FR-028): the merged manifest, libraries included, asks for no network access. */
class ManifestPermissionsTest {
    private val manifest =
        File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")

    private val permissions: List<String> by lazy {
        Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun `the merged manifest was found`() {
        manifest.exists() shouldBe true
    }

    @Test
    fun `no network permission is declared`() {
        permissions shouldNotContain "android.permission.INTERNET"
        permissions shouldNotContain "android.permission.ACCESS_NETWORK_STATE"
        permissions shouldNotContain "android.permission.ACCESS_WIFI_STATE"
    }

    @Test
    fun `only media, export, and microphone permissions are requested, besides the playback wake lock`() {
        val allowed = listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WAKE_LOCK",
            // Export: the background render service, its progress notification, and the legacy save on Android 8 to 9.
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PROCESSING",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            // Voice-over: the microphone, asked only when the user first taps Record (feature 013).
            "android.permission.RECORD_AUDIO",
        )
        permissions.filterNot {
            it.startsWith("android.permission.READ_MEDIA_") || it in allowed || it.contains("DYNAMIC_RECEIVER")
        }.shouldBe(emptyList())
    }
}
