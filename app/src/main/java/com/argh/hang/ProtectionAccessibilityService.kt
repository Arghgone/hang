package com.argh.hang

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Layered protection engine.
 *
 * Layer 1 - launch interception: opening a protected app requires
 *           verification unless a per-app unlock session is active.
 * Layer 2 - settings-context detection: known management surfaces
 *           (Settings, OEM security centers, installers, launchers) are
 *           classified by package + activity class name (intent-based).
 * Layer 3 - app-context tracking: once a protected app's management screen
 *           is identified, that app stays the active target across nested
 *           pages where its name is no longer visible.
 * Layer 4 - keyword fallback: broad OEM/version keyword net, applied only
 *           inside management contexts and only when a target app context
 *           exists, so generic words alone never trigger verification.
 *
 * Loop prevention: our own UI is never intercepted (except launch protection
 * of the setup screen when Hang itself is protected), no prompt is launched
 * while one is active, and dismissed prompts have a reprompt cooldown.
 */
class ProtectionAccessibilityService : AccessibilityService() {

    companion object {
        /** Minimum time between two interceptions, to avoid launch loops. */
        private const val INTERCEPT_DEBOUNCE_MS = 1_500L

        /** Config snapshot cache TTL, keeps runBlocking off the event hot path. */
        private const val CONFIG_CACHE_MS = 2_000L
    }

    private var lastInterceptAt = 0L
    private val labelCache = mutableMapOf<String, String>()
    private val tracker = TargetContextTracker()

    /** Activity class of the current window (state-changed events only). */
    private var currentScreenClass: String? = null

    private var cachedConfig: ProtectionConfig? = null
    private var cachedConfigAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val sourcePackage = event.packageName?.toString() ?: return
        val isStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (isStateChange) currentScreenClass = event.className?.toString()

        // ---- Self handling: never self-trigger -----------------------------
        if (sourcePackage == packageName) {
            handleOwnWindow(isStateChange, event.className?.toString())
            return
        }

        if (UnlockManager.isPromptActive()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastInterceptAt < INTERCEPT_DEBOUNCE_MS) return

        val config = config()
        if (config.protectedPackages.isEmpty()) return

        // ---- Layer 1: protected app launch ---------------------------------
        if (isStateChange &&
            sourcePackage in config.protectedPackages &&
            ProtectedAction.APP_LAUNCH in config.protectedActions
        ) {
            handleProtectedAppForeground(sourcePackage, now, goHome = true)
            return
        }

        // ---- Layers 2-4: management surfaces --------------------------------
        val isManagement = SettingsContext.isManagementContext(sourcePackage)
        val stickyTarget = tracker.update(isManagement)
        if (!isManagement) return

        val root = rootInActiveWindow ?: return
        val screenText = StringBuilder().also { collectText(root, it, 0) }.toString()
        val lowerText = screenText.lowercase()

        // Identify the target app explicitly visible on this screen...
        val explicitTarget = config.protectedPackages.firstOrNull { pkg ->
            lowerText.contains(pkg.lowercase()) ||
                appLabel(pkg)?.lowercase()?.let { lowerText.contains(it) } == true
        }
        // ...or fall back to the sticky context from a previous screen.
        val target = explicitTarget ?: stickyTarget
        if (explicitTarget != null) tracker.confirm(explicitTarget)

        DiagnosticLog.screen(
            currentPackage = sourcePackage,
            className = currentScreenClass,
            screenTitle = findScreenTitle(root),
            visibleTextPreview = lowerText,
            activeTarget = target,
        )

        if (target == null) return
        if (UnlockManager.isUnlocked(target)) {
            DiagnosticLog.decision(target, null, "session", false, "unlock session active")
            return
        }

        // Intent-based detection first (settings-context layer)...
        var triggerSource = "settings-context"
        var action = SettingsContext.actionsForClassName(currentScreenClass)
            .firstOrNull { it in config.protectedActions }

        // ...keyword net as a fallback safety layer only.
        if (action == null) {
            action = config.protectedActions.firstOrNull { a ->
                a.keywords.any { lowerText.contains(it) }
            }
            triggerSource = "keyword-fallback"
        }
        if (action == null || action == ProtectedAction.APP_LAUNCH) return

        if (UnlockManager.isInRepromptCooldown(target, action.name)) {
            DiagnosticLog.decision(target, action, triggerSource, false, "reprompt cooldown")
            return
        }

        lastInterceptAt = now
        DiagnosticLog.decision(target, action, triggerSource, true, "protected action in app context")
        performGlobalAction(GLOBAL_ACTION_BACK)
        launchVerification(target, action)
    }

    /**
     * Hang itself can be protected: opening the setup screen then requires
     * verification, while the verification screen never self-triggers.
     */
    private fun handleOwnWindow(isStateChange: Boolean, className: String?) {
        if (!isStateChange) return
        if (className?.endsWith(".MainActivity") != true) return
        if (UnlockManager.isPromptActive()) return
        val config = config()
        if (packageName in config.protectedPackages &&
            ProtectedAction.APP_LAUNCH in config.protectedActions &&
            !UnlockManager.isUnlocked(packageName)
        ) {
            handleProtectedAppForeground(packageName, SystemClock.elapsedRealtime(), goHome = true)
        }
    }

    private fun handleProtectedAppForeground(pkg: String, now: Long, goHome: Boolean) {
        if (now - lastInterceptAt < INTERCEPT_DEBOUNCE_MS) return
        if (UnlockManager.isUnlocked(pkg)) {
            DiagnosticLog.decision(pkg, ProtectedAction.APP_LAUNCH, "launch", false, "unlock session active")
            return
        }
        if (UnlockManager.isInRepromptCooldown(pkg, ProtectedAction.APP_LAUNCH.name)) {
            DiagnosticLog.decision(pkg, ProtectedAction.APP_LAUNCH, "launch", false, "reprompt cooldown")
            return
        }
        lastInterceptAt = now
        DiagnosticLog.decision(pkg, ProtectedAction.APP_LAUNCH, "launch", true, "protected app opened")
        if (goHome) performGlobalAction(GLOBAL_ACTION_HOME)
        launchVerification(pkg, ProtectedAction.APP_LAUNCH)
    }

    private fun launchVerification(targetPackage: String, action: ProtectedAction) {
        startActivity(
            Intent(this, VerificationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(VerificationActivity.EXTRA_PACKAGE, targetPackage)
                putExtra(VerificationActivity.EXTRA_ACTION, action.name)
            },
        )
    }

    private fun config(): ProtectionConfig {
        val now = SystemClock.elapsedRealtime()
        val cached = cachedConfig
        if (cached != null && now - cachedConfigAt < CONFIG_CACHE_MS) return cached
        return ConfigRepository(this).snapshot().also {
            cachedConfig = it
            cachedConfigAt = now
        }
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

    private fun findScreenTitle(root: AccessibilityNodeInfo): String? {
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val text = child.text
            if (!text.isNullOrBlank()) return text.toString()
        }
        return null
    }

    private fun appLabel(packageName: String): String? =
        labelCache.getOrPut(packageName) {
            try {
                val info = getPackageManager().getApplicationInfo(packageName, 0)
                getPackageManager().getApplicationLabel(info).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
        }
}
