package com.argh.hang

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches all app screens, including Settings, the system package installer,
 * the Play Store, and third-party uninstallers/cleaners. When a screen
 * relates to a protected app AND a protected action, it backs out of the
 * screen and launches the verification interruption, unless the package was
 * recently unlocked by a successful passage verification.
 */
class ProtectionAccessibilityService : AccessibilityService() {

    companion object {
        /** Minimum time between two interceptions, to avoid launch loops. */
        private const val INTERCEPT_DEBOUNCE_MS = 1_500L
    }

    private var lastInterceptAt = 0L
    private val labelCache = mutableMapOf<String, String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        // All apps are monitored (Settings, Play Store, package installer,
        // third-party uninstallers/cleaners). Skip our own UI so the
        // verification screen is never intercepted by itself.
        val sourcePackage = event.packageName?.toString()
        if (sourcePackage == null || sourcePackage == packageName) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastInterceptAt < INTERCEPT_DEBOUNCE_MS) return

        val root = rootInActiveWindow ?: return
        val config = ConfigRepository(this).snapshot()
        if (config.protectedPackages.isEmpty() || config.protectedActions.isEmpty()) return

        val screenText = StringBuilder().also { collectText(root, it, 0) }.toString().lowercase()
        if (screenText.isBlank()) return

        val targetPackage = config.protectedPackages.firstOrNull { pkg ->
            screenText.contains(pkg.lowercase()) ||
                appLabel(pkg)?.lowercase()?.let { screenText.contains(it) } == true
        } ?: return

        val action = config.protectedActions.firstOrNull { a ->
            a.keywords.any { screenText.contains(it) }
        } ?: return

        if (UnlockManager.isUnlocked(targetPackage)) return

        lastInterceptAt = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        startActivity(
            Intent(this, VerificationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(VerificationActivity.EXTRA_PACKAGE, targetPackage)
                putExtra(VerificationActivity.EXTRA_ACTION, action.name)
            },
        )
    }

    override fun onInterrupt() = Unit

    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int) {
        if (node == null || depth > 25) return
        node.text?.let { out.append(it).append(' ') }
        node.contentDescription?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), out, depth + 1)
        }
    }

    private fun appLabel(packageName: String): String? =
        labelCache.getOrPut(packageName) {
            try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
        }
}
