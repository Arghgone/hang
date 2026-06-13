package com.argh.hang

import android.os.SystemClock

/**
 * Global authorization session granted after successful passage verification.
 * Replaces per-package unlock sessions — one session covers all protected actions on Hang.
 */
object AuthorizationSession {
    private var expiresAt: Long = 0L

    fun grant(durationMs: Long) {
        expiresAt = SystemClock.elapsedRealtime() + durationMs
    }

    fun isActive(): Boolean = SystemClock.elapsedRealtime() < expiresAt

    fun revoke() {
        expiresAt = 0L
    }
}
