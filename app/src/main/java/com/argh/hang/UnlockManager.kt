package com.argh.hang

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks per-app temporary unlock sessions granted after a successful passage
 * verification, plus the state needed to prevent prompt loops and duplicate
 * interceptions:
 *  - an unlock applies to a single package and expires automatically,
 *    restoring protection without any user action (automatic relock),
 *  - while a verification prompt is on screen no new prompts are launched,
 *  - identical (package, action) prompts are debounced after dismissal so
 *    backing out of a screen does not immediately re-trigger verification.
 */
object UnlockManager {

    /** How long a successful verification keeps a package unlocked. */
    const val UNLOCK_WINDOW_MS: Long = 60_000

    /** After a prompt closes, the same (package, action) is not re-prompted for this long. */
    private const val REPROMPT_COOLDOWN_MS: Long = 5_000

    private val unlockExpiry = ConcurrentHashMap<String, Long>()
    private val promptActive = AtomicBoolean(false)
    private val lastPromptDismissed = ConcurrentHashMap<String, Long>()

    fun grantUnlock(packageName: String, durationMs: Long = UNLOCK_WINDOW_MS) {
        unlockExpiry[packageName] = SystemClock.elapsedRealtime() + durationMs
    }

    fun isUnlocked(packageName: String): Boolean {
        val expiry = unlockExpiry[packageName] ?: return false
        if (SystemClock.elapsedRealtime() > expiry) {
            unlockExpiry.remove(packageName)
            return false
        }
        return true
    }

    /** Milliseconds remaining in the unlock session, or 0. */
    fun remainingMs(packageName: String): Long {
        val expiry = unlockExpiry[packageName] ?: return 0
        return (expiry - SystemClock.elapsedRealtime()).coerceAtLeast(0)
    }

    fun revoke(packageName: String) {
        unlockExpiry.remove(packageName)
    }

    // -- Prompt loop prevention ----------------------------------------------

    fun isPromptActive(): Boolean = promptActive.get()

    fun onPromptShown() {
        promptActive.set(true)
    }

    fun onPromptDismissed(packageName: String?, actionName: String?) {
        promptActive.set(false)
        if (packageName != null && actionName != null) {
            lastPromptDismissed["$packageName/$actionName"] = SystemClock.elapsedRealtime()
        }
    }

    fun isInRepromptCooldown(packageName: String, actionName: String): Boolean {
        val at = lastPromptDismissed["$packageName/$actionName"] ?: return false
        return SystemClock.elapsedRealtime() - at < REPROMPT_COOLDOWN_MS
    }
}
