package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class ThemeChoiceTest {
    @Test
    fun systemFollowsThePhone() {
        ThemeChoice.System.isDark(systemDark = true) shouldBe true
        ThemeChoice.System.isDark(systemDark = false) shouldBe false
    }

    @Test
    fun lightAndDarkIgnoreThePhone() {
        ThemeChoice.Light.isDark(systemDark = true) shouldBe false
        ThemeChoice.Light.isDark(systemDark = false) shouldBe false
        ThemeChoice.Dark.isDark(systemDark = true) shouldBe true
        ThemeChoice.Dark.isDark(systemDark = false) shouldBe true
    }

    @Test
    fun theDefaultSettingFollowsThePhone() {
        AppSettings().theme shouldBe ThemeChoice.System
    }
}
