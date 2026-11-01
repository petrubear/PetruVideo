package com.petruvideo.app

import android.app.Application
import com.petruvideo.app.di.ApplicationScope
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.data.db.reversedRanges
import com.petruvideo.core.data.repo.SettingsRepository
import com.petruvideo.core.export.ExportCoordinator
import com.petruvideo.core.export.ExportRecovery
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.media.reverse.ReverseNeed
import com.petruvideo.core.media.reverse.ReversePreparer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PetruVideoApp : Application() {
    @Inject lateinit var pressure: ResourcePressureMonitor
    @Inject lateinit var exportCoordinator: ExportCoordinator
    @Inject lateinit var exportRecovery: ExportRecovery
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var database: PetruDatabase
    @Inject lateinit var reverse: ReversePreparer
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // A render cut short by the app being closed is reported as interrupted; its leftovers are removed.
        scope.launch { exportRecovery.run() }
        // Backwards copies that no reversed clip uses any more (for instance after Reverse was turned off) are removed.
        scope.launch { reverse.sweep(database.reversedRanges().map { ReverseNeed(it.media, it.startMs, it.endMs) }) }
        scope.launch { pressure.level.collect(exportCoordinator::onPressure) }
        scope.launch { settings.settings.collect { pressure.setForcedReduced(it.lowerPreview) } }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        pressure.onTrimMemory(level)
    }
}
