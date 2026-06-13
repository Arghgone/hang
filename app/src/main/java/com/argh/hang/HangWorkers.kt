package com.argh.hang

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker that checks whether OverlayService is running and
 * restarts it if not. Scheduled as a periodic task (minimum 15-minute
 * interval) by OverlayService.onCreate().
 */
class OverlayServiceCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!OverlayService.isRunning.get()) {
            DiagnosticLog.watchdog("periodic-check-restarting-service")
            startService(applicationContext)
        }
        return Result.success()
    }
}

/**
 * One-shot expedited WorkManager worker triggered from OverlayService.onTaskRemoved().
 * Restarts OverlayService within ~10 seconds of the task being removed.
 */
class RestartOverlayServiceWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        DiagnosticLog.watchdog("expedited-restart-starting-service")
        startService(applicationContext)
        return Result.success()
    }
}

private fun startService(context: Context) {
    val intent = Intent(context, OverlayService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
