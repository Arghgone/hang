package com.argh.hang

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat

/**
 * Compares desired vs actual state for all Hang subsystems.
 * Called by the health watchdog on every poll tick.
 * Logs each deviation and invokes RecoveryEngine as appropriate.
 */
class StateEnforcer(
    private val context: Context,
    private val overlayEngine: OverlayEngine,
    private val windowManager: WindowManager,
) {

    fun check(config: ProtectionConfig) {
        checkOverlay(config)
        checkOverlayPermission()
        checkAccessibility()
        checkNotifications()
        checkBattery()
    }

    // --- Individual checks --------------------------------------------------

    private fun checkOverlay(config: ProtectionConfig) {
        if (!config.overlayEnabled) return
        if (!Settings.canDrawOverlays(context)) return // handled by checkOverlayPermission
        if (!overlayEngine.isAttached()) {
            DiagnosticLog.watchdog("state-overlay-detached")
            RecoveryEngine.recoverOverlay(context, overlayEngine, windowManager, config)
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(context)) {
            DiagnosticLog.watchdog("state-overlay-permission-revoked")
            RecoveryEngine.requestWizard(context, RecoveryIssue.OVERLAY_PERMISSION_REMOVED)
        }
    }

    private fun checkAccessibility() {
        val services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
        if (!services.contains(context.packageName, ignoreCase = true)) {
            DiagnosticLog.watchdog("state-accessibility-disabled")
            RecoveryEngine.requestWizard(context, RecoveryIssue.ACCESSIBILITY_REMOVED)
        }
    }

    private fun checkNotifications() {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            DiagnosticLog.watchdog("state-notifications-disabled")
            RecoveryEngine.requestWizard(context, RecoveryIssue.NOTIFICATION_DISABLED)
        }
    }

    private fun checkBattery() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            DiagnosticLog.watchdog("state-battery-restricted")
            RecoveryEngine.requestWizard(context, RecoveryIssue.BATTERY_RESTRICTED)
        }
    }
}
