package com.petruvideo.core.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.export.Quality
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences("settings-test", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun defaultsThenChangesThatSurviveANewInstance() {
        val repo = PrefsSettingsRepository(prefs)
        repo.settings.value shouldBe AppSettings()
        repo.setDefaultQuality(Quality.Smaller)
        repo.setLowerPreview(true)
        repo.settings.value shouldBe AppSettings(Quality.Smaller, true)
        PrefsSettingsRepository(prefs).settings.value shouldBe AppSettings(Quality.Smaller, true)
    }

    @Test
    fun anUnknownStoredQualityFallsBackToRecommended() {
        prefs.edit().putString("default_quality", "Gone").commit()
        PrefsSettingsRepository(prefs).settings.value.defaultQuality shouldBe Quality.Recommended
    }
}
