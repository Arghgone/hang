package com.argh.hang

import android.util.Log

/**
 * Structured diagnostic logging. Writes to Logcat and maintains an in-memory
 * circular buffer (last 200 entries) for display and export in the settings UI.
 *
 * Logcat tag: HangProtect.
 */
object DiagnosticLog {

    private const val TAG = "HangProtect"
    private const val MAX_ENTRIES = 200

    @Volatile
    var enabled: Boolean = true

    private val buffer = ArrayDeque<String>(MAX_ENTRIES)

    fun getRecentEntries(): List<String> = synchronized(buffer) { buffer.toList() }

    private fun record(level: String, msg: String) {
        if (!enabled) return
        val ts = System.currentTimeMillis()
        val entry = "[$ts] $level $msg"
        synchronized(buffer) {
            if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
            buffer.addLast(entry)
        }
        Log.d(TAG, msg)
    }

    fun screen(
        currentPackage: String?,
        className: String?,
        screenTitle: String?,
        visibleTextPreview: String?,
        activeTarget: String?,
    ) {
        record(
            "SCREEN",
            "pkg=$currentPackage class=$className title=$screenTitle " +
                "target=$activeTarget text=\"${visibleTextPreview?.take(200)}\"",
        )
    }

    fun decision(
        targetPackage: String?,
        action: ProtectedAction?,
        triggerSource: String,
        intercepted: Boolean,
        reason: String,
    ) {
        record(
            "DECISION",
            "target=$targetPackage action=${action?.name} " +
                "source=$triggerSource intercepted=$intercepted reason=$reason",
        )
    }

    fun verification(targetPackage: String?, action: String?, outcome: String) {
        record("VERIFY", "target=$targetPackage action=$action outcome=$outcome")
    }

    fun overlay(event: String, detail: String) {
        record("OVERLAY", "event=$event detail=$detail")
    }

    fun recovery(event: String, detail: String) {
        record("RECOVERY", "event=$event detail=$detail")
    }

    fun xosSurface(pkg: String, className: String?, event: String) {
        record("XOS", "pkg=$pkg class=$className event=$event")
    }

    fun watchdog(event: String) {
        record("WATCHDOG", event)
    }
}
