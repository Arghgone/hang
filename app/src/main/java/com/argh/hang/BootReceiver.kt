package com.argh.hang

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Starts OverlayService after device boot.
 *
 * On Android 14+, BOOT_COMPLETED fires before the user has decrypted storage.
 * We defer actual service startup to UserUnlockedReceiver which fires after
 * the user unlocks the device for the first time.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                if (Build.VERSION.SDK_INT >= 24) {
                    // Defer to UserUnlockedReceiver — storage may not yet be decrypted.
                    DiagnosticLog.watchdog("boot-received-deferred-to-user-unlock")
                } else {
                    startOverlayService(context)
                }
            }
        }
    }

    companion object {
        fun startOverlayService(context: Context) {
            DiagnosticLog.watchdog("boot-starting-overlay-service")
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
