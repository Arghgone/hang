package com.argh.hang

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Simplified protection engine — Hang only.
 *
 * Layer 1 (app launch) is removed. Detection focuses on management surfaces:
 *
 * Layer 2 — Settings-context: known management packages + activity class names.
 * Layer 3 — Hang visibility: Hang's package name or label must appear on the
 *            current screen before any interception fires.
 * Layer 4 — Keyword fallback: broad OEM/version keyword net, applied only
 *            when Hang is the visible target and no class-name match was found.
 *
 * Loop prevention: PromptState ensures only one prompt is active at a time.
 * Session: AuthorizationSession.isActive() grants a time-limited pass.
 */
class ProtectionAccessibilityService : AccessibilityService() {

    companion object {
        private const val CONFIG_CACHE_MS = 2_000L
    }

    private var currentScreenClass: String? = null
    private val labelCache = mutableMapOf<String, String?>()

    private var cachedConfig: ProtectionConfig? = null
    private var cachedConfigAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val sourcePackage = event.packageName?.toString() ?: return
        val isStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (isStateChange) currentScreenClass = event.className?.toString()

        // Never self-trigger
        if (sourcePackage == packageName) return

        // One prompt at a time
        if (PromptState.isPromptActive()) return

        // Only care about management surfaces
        if (!SettingsContext.isManagementContext(sourcePackage)) return

        val root = rootInActiveWindow ?: return
        val screenText = StringBuilder().also { collectText(root, it, 0) }.toString()
        val lowerText = screenText.lowercase()

        // Hang must be the visible target on this screen
        val hangLabel = appLabel(packageName)
        val hangVisible = lowerText.contains(packageName.lowercase()) ||
            (hangLabel != null && lowerText.contains(hangLabel.lowercase()))

        if (!hangVisible) return

        DiagnosticLog.screen(sourcePackage, currentScreenClass, findScreenTitle(root), lowerText, packageName)

        // Log XOS management surfaces for diagnostics
        if (sourcePackage.contains("transsion", ignoreCase = true) ||
            sourcePackage.contains("xui", ignoreCase = true)
        ) {
            DiagnosticLog.xosSurface(sourcePackage, currentScreenClass, "management-screen-detected")
        }

        // Session active → allow through
        if (AuthorizationSession.isActive()) {
            DiagnosticLog.decision(packageName, null, "n/a", false, "session active")
            return
        }

        // Layer 2: intent-based (class name → action)
        var triggerSource = "settings-context"
        var action: ProtectedAction? = SettingsContext.actionsForClassName(currentScreenClass)
            .firstOrNull()

        // Layer 4: keyword fallback
        if (action == null) {
            action = ProtectedAction.entries.firstOrNull { a ->
                a.keywords.any { lowerText.contains(it) }
            }
            triggerSource = "keyword-fallback"
        }

        if (action == null) return

        DiagnosticLog.decision(packageName, action, triggerSource, true, "protected action on Hang")
        performGlobalAction(GLOBAL_ACTION_BACK)
        launchVerification(action)
    }

    private fun launchVerification(action: ProtectedAction) {
        startActivity(
            Intent(this, VerificationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(VerificationActivity.EXTRA_PACKAGE, packageName)
                putExtra(VerificationActivity.EXTRA_ACTION, action.name)
            },
        )
    }

    override fun onInterrupt() = Unit

    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int) {
        if (node == null || depth > 25) return
        node.text?.let { out.append(it).append(' ') }
        node.contentDescription?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out, depth + 1)
    }

    private fun findScreenTitle(root: AccessibilityNodeInfo): String? {
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val text = child.text
            if (!text.isNullOrBlank()) return text.toString()
        }
        return null
    }

    private fun appLabel(pkg: String): String? =
        labelCache.getOrPut(pkg) {
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
}
