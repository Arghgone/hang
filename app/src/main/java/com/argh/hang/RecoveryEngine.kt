package com.argh.hang

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings

/** Issues that require the Recovery Wizard to guide the user. */
enum class RecoveryIssue {
    ACCESSIBILITY_REMOVED,
    OVERLAY_PERMISSION_REMOVED,
    BATTERY_RESTRICTED,
    NOTIFICATION_DISABLED,
    OVERLAY_DETACHED,
}

/**
 * Handles both silent (automatic) and manual (wizard) recovery.
 *
 * Silent recovery: re-attaches the overlay window without user interaction.
 * Wizard recovery: launches RecoveryWizardActivity with a deep-link to the
 * relevant system settings screen.
 */
object RecoveryEngine {

    /** Minimum time between wizard launches to avoid flooding the user. */
    private const val WIZARD_COOLDOWN_MS = 30_000L
    private var lastWizardAt = 0L

    /** Attempt silent overlay re-attachment. */
    fun recoverOverlay(
        context: Context,
        engine: OverlayEngine,
        windowManager: android.view.WindowManager,
        config: ProtectionConfig,
    ) {
        DiagnosticLog.recovery("silent-recovery", "re-attaching overlay")
        engine.attach(context, windowManager, config)
    }

    /** Launch RecoveryWizardActivity if cooldown has elapsed. */
    fun requestWizard(context: Context, issue: RecoveryIssue) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWizardAt < WIZARD_COOLDOWN_MS) {
            DiagnosticLog.recovery("wizard-suppressed", "cooldown: issue=$issue")
            return
        }
        lastWizardAt = now
        DiagnosticLog.recovery("wizard-launch", "issue=$issue")
        context.startActivity(
            Intent(context, RecoveryWizardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(RecoveryWizardActivity.EXTRA_ISSUE, issue.name)
            },
        )
    }

    /** Returns the deep-link Intent for the given issue, or null for OVERLAY_DETACHED. */
    fun deepLinkFor(context: Context, issue: RecoveryIssue): Intent? = when (issue) {
        RecoveryIssue.ACCESSIBILITY_REMOVED ->
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        RecoveryIssue.OVERLAY_PERMISSION_REMOVED ->
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        RecoveryIssue.BATTERY_RESTRICTED ->
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        RecoveryIssue.NOTIFICATION_DISABLED ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        RecoveryIssue.OVERLAY_DETACHED -> null
    }
}
