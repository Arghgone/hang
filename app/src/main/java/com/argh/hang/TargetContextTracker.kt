package com.argh.hang

import android.os.SystemClock

/**
 * Keeps a protected app "in focus" while the user navigates nested settings
 * screens where the app name is no longer visible (e.g. App info >
 * Notifications > a specific category). The context survives as long as the
 * user stays inside management surfaces and expires automatically.
 */
class TargetContextTracker {

    companion object {
        /** Context survives this long without re-confirmation. */
        private const val CONTEXT_TTL_MS = 3 * 60_000L
    }

    var activePackage: String? = null
        private set

    private var lastConfirmedAt = 0L

    /** Call when a protected app is explicitly visible on a management screen. */
    fun confirm(packageName: String) {
        activePackage = packageName
        lastConfirmedAt = SystemClock.elapsedRealtime()
    }

    /**
     * Call on every event. Keeps the context alive inside management
     * surfaces; drops it when the user leaves them or the TTL expires.
     * Returns the active target, if still valid.
     */
    fun update(isManagementContext: Boolean): String? {
        if (activePackage == null) return null
        val expired = SystemClock.elapsedRealtime() - lastConfirmedAt > CONTEXT_TTL_MS
        if (expired || !isManagementContext) {
            clear()
            return null
        }
        return activePackage
    }

    fun clear() {
        activePackage = null
        lastConfirmedAt = 0L
    }
}
