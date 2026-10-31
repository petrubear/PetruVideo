package com.petruvideo.core.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.petruvideo.core.model.export.ExportState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Starts the foreground service that keeps an export alive while the app is in the background. */
fun interface ExportServiceStarter {
    fun start()
}

class ContextExportServiceStarter(private val context: Context) : ExportServiceStarter {
    override fun start() {
        context.startForegroundService(Intent(context, ExportService::class.java))
    }
}

/**
 * Foreground service for an export: a progress notification with a Cancel action, and a closing notification with the
 * result that opens the app. It stops itself when the job ends.
 */
@AndroidEntryPoint
class ExportService : Service() {
    @Inject lateinit var coordinator: ExportCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) coordinator.cancel()
        ensureChannel()
        startForegroundCompat(progressNotification(0, "Preparing export"))
        if (!observing) {
            observing = true
            scope.launch {
                coordinator.job.collect { job ->
                    val state = job?.state ?: return@collect
                    when (state) {
                        is ExportState.Waiting -> update(progressNotification(0, job.projectName))
                        is ExportState.Rendering -> update(progressNotification(state.progress, job.projectName))
                        else -> {
                            finishWith(resultNotification(state, job.projectName))
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun finishWith(notification: Notification) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).notify(RESULT_ID, notification)
        stopSelf()
    }

    private fun update(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(ID, notification)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(ID, notification, type)
        } else {
            startForeground(ID, notification)
        }
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Export", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun openApp(): PendingIntent {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(this, 0, launch, flags)
    }

    private fun progressNotification(progress: Int, name: String): Notification {
        val cancel = PendingIntent.getService(
            this, 1, Intent(this, ExportService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Exporting $name")
            .setContentText("$progress%")
            .setProgress(PROGRESS_MAX, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .addAction(0, "Cancel", cancel)
            .build()
    }

    private fun resultNotification(state: ExportState, name: String): Notification {
        val text = when (state) {
            is ExportState.Finished -> "Export saved"
            ExportState.Cancelled -> "Export cancelled"
            else -> "Export did not finish"
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(name)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
    }

    private companion object {
        const val CHANNEL = "export"
        const val ID = 4001
        const val RESULT_ID = 4002
        const val PROGRESS_MAX = 100
        const val ACTION_CANCEL = "com.petruvideo.export.CANCEL"
    }
}
