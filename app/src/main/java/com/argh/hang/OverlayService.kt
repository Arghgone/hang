package com.argh.hang

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Long-running foreground service that owns the overlay window.
 *
 * Responsibilities:
 *  - Starts as foreground with a persistent low-importance notification.
 *  - Creates OverlayEngine and attaches the overlay window.
 *  - Runs a HealthWatchdog poll loop (30 s screen-on, 120 s screen-off).
 *  - Polls canDrawOverlays() every 60 s as an XOS-specific mitigation.
 *  - Schedules a WorkManager periodic check and an expedited restart on task removal.
 *  - Handles orientation changes to show/hide the overlay per config.
 */
class OverlayService : Service() {

    companion object {
        val isRunning = AtomicBoolean(false)
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hang_overlay"
        private const val PERIODIC_WORK_NAME = "overlay_service_check"
        private const val OVERLAY_POLL_MS = 60_000L
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayEngine: OverlayEngine
    private lateinit var stateEnforcer: StateEnforcer
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var pollIntervalMs = 30_000L
    private var overlayPermPollTick = 0

    private val watchdogRunnable: Runnable = object : Runnable {
        override fun run() {
            val config = ConfigRepository(this@OverlayService).snapshot()
            stateEnforcer.check(config)
            // XOS: check overlay permission every ~60 s independently
            overlayPermPollTick++
            if (overlayPermPollTick * pollIntervalMs >= OVERLAY_POLL_MS) {
                overlayPermPollTick = 0
                if (!Settings.canDrawOverlays(this@OverlayService)) {
                    DiagnosticLog.xosSurface(packageName, null, "overlay-permission-poll-revoked")
                    RecoveryEngine.requestWizard(this@OverlayService, RecoveryIssue.OVERLAY_PERMISSION_REMOVED)
                }
            }
            watchdogHandler.postDelayed(this, pollIntervalMs)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pollIntervalMs = when (intent.action) {
                Intent.ACTION_SCREEN_ON -> 30_000L
                Intent.ACTION_SCREEN_OFF -> 120_000L
                else -> pollIntervalMs
            }
            DiagnosticLog.watchdog("screen-state-changed interval=${pollIntervalMs}ms")
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayEngine = OverlayEngine()
        stateEnforcer = StateEnforcer(this, overlayEngine, windowManager)

        createNotificationChannel()
        startForegroundCompat(buildNotification())

        // Attach overlay
        val config = ConfigRepository(this).snapshot()
        if (config.overlayEnabled) {
            overlayEngine.attach(this, windowManager, config)
            updateOrientationVisibility(config)
        }

        // Screen on/off polling adjustment
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)

        // Health watchdog
        watchdogHandler.postDelayed(watchdogRunnable, pollIntervalMs)

        // WorkManager periodic check
        val periodicRequest = PeriodicWorkRequestBuilder<OverlayServiceCheckWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )

        DiagnosticLog.watchdog("service-created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-attach overlay if it was lost while the service was recreated.
        val config = ConfigRepository(this).snapshot()
        if (config.overlayEnabled && !overlayEngine.isAttached()) {
            DiagnosticLog.overlay("onStartCommand-re-attach", "")
            overlayEngine.attach(this, windowManager, config)
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val config = ConfigRepository(this).snapshot()
        updateOrientationVisibility(config)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        DiagnosticLog.watchdog("task-removed-scheduling-restart")
        val request = OneTimeWorkRequestBuilder<RestartOverlayServiceWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(request)
    }

    override fun onDestroy() {
        isRunning.set(false)
        watchdogHandler.removeCallbacks(watchdogRunnable)
        runCatching { unregisterReceiver(screenReceiver) }
        overlayEngine.detach(windowManager)
        DiagnosticLog.watchdog("service-destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Notification -------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hang Reminder",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent reminder overlay is active."
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val config = ConfigRepository(this).snapshot()
        val largeIcon = config.overlayImagePath
            ?.let { File(it) }
            ?.takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            @Suppress("NewApi")
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // --- Orientation --------------------------------------------------------

    private fun updateOrientationVisibility(config: ProtectionConfig) {
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        overlayEngine.updateVisibility(isPortrait, config)
    }
}
