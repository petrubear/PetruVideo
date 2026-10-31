package com.petruvideo.app

import android.app.Application
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PetruVideoApp : Application() {
    @Inject lateinit var pressure: ResourcePressureMonitor

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        pressure.onTrimMemory(level)
    }
}
