package com.petruvideo.app

import android.app.Application
import com.petruvideo.app.di.ApplicationScope
import com.petruvideo.core.export.ExportCoordinator
import com.petruvideo.core.export.ExportRecovery
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PetruVideoApp : Application() {
    @Inject lateinit var pressure: ResourcePressureMonitor
    @Inject lateinit var exportCoordinator: ExportCoordinator
    @Inject lateinit var exportRecovery: ExportRecovery
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // A render cut short by the app being closed is reported as interrupted; its leftovers are removed.
        scope.launch { exportRecovery.run() }
        scope.launch { pressure.level.collect(exportCoordinator::onPressure) }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        pressure.onTrimMemory(level)
    }
}
