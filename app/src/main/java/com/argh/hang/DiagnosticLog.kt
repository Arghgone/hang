package com.argh.hang

import android.util.Log

/**
 * Structured diagnostic logging used to identify missed bypass paths.
 * Logcat tag: HangProtect.
 *
 * Logged dimensions: current package, screen class, screen title, visible
 * text preview, active target app, detected action, trigger source,
 * interception decision, and verification outcome.
 */
object DiagnosticLog {

    private const val TAG = "HangProtect"

    /** Toggle for verbose bypass-hunting logs. */
    @Volatile
    var enabled: Boolean = true

    fun screen(
        currentPackage: String?,
        className: String?,
        screenTitle: String?,
        visibleTextPreview: String?,
        activeTarget: String?,
    ) {
        if (!enabled) return
        Log.d(
            TAG,
            "screen pkg=$currentPackage class=$className title=$screenTitle " +
                "target=$activeTarget text=\"${visibleTextPreview?.take(300)}\"",
        )
    }

    fun decision(
        targetPackage: String?,
        action: ProtectedAction?,
        triggerSource: String,
        intercepted: Boolean,
        reason: String,
    ) {
        if (!enabled) return
        Log.i(
            TAG,
            "decision target=$targetPackage action=${action?.name} " +
                "source=$triggerSource intercepted=$intercepted reason=$reason",
        )
    }

    fun verification(targetPackage: String?, action: String?, outcome: String) {
        if (!enabled) return
        Log.i(TAG, "verification target=$targetPackage action=$action outcome=$outcome")
    }
}
