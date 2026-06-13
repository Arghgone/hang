package com.argh.hang

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires after the user unlocks the device for the first time after boot.
 * At this point, credential-encrypted storage is available and we can
 * safely start OverlayService.
 */
class UserUnlockedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_UNLOCKED) {
            DiagnosticLog.watchdog("user-unlocked-starting-overlay-service")
            BootReceiver.startOverlayService(context)
        }
    }
}
