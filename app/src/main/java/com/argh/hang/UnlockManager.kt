package com.argh.hang

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks temporary unlocks granted after a successful passage verification.
 * An unlock applies to a single package and expires automatically, restoring
 * protection without any user action.
 */
object UnlockManager {

    /** How long a successful verification keeps the action unlocked. */
    const val UNLOCK_WINDOW_MS: Long = 60_000

    private val unlockExpiry = ConcurrentHashMap<String, Long>()

    fun grantUnlock(packageName: String) {
        unlockExpiry[packageName] = SystemClock.elapsedRealtime() + UNLOCK_WINDOW_MS
    }

    fun isUnlocked(packageName: String): Boolean {
        val expiry = unlockExpiry[packageName] ?: return false
        if (SystemClock.elapsedRealtime() > expiry) {
            unlockExpiry.remove(packageName)
            return false
        }
        return true
    }

    fun revoke(packageName: String) {
        unlockExpiry.remove(packageName)
    }
}
