package com.argh.hang

/**
 * Actions that can be protected for a given app. Each action carries the
 * screen-text keywords used by the accessibility service to recognise that
 * the user is on a screen where this action can be performed.
 */
enum class ProtectedAction(val label: String, val keywords: List<String>) {
    UNINSTALL("Uninstall", listOf("uninstall")),
    DISABLE("Disable", listOf("disable app", "disable")),
    HIDE("Hide", listOf("hide app", "hide")),
    FORCE_STOP("Force stop", listOf("force stop")),
    DEVICE_ADMIN("Remove device admin", listOf("deactivate this device admin", "device admin")),
    PERMISSIONS("Modify permissions", listOf("app permissions", "permission manager")),
    OVERLAY("Display over other apps", listOf("display over other apps", "appear on top")),
    ACCESSIBILITY("Accessibility access", listOf("accessibility")),
    NOTIFICATIONS("Notification access", listOf("notification access", "app notifications")),
    BATTERY("Battery optimization", listOf("battery optimization", "optimize battery usage", "battery usage")),
    USAGE_ACCESS("Usage access", listOf("usage access", "usage data access"));

    companion object {
        fun fromName(name: String): ProtectedAction? = entries.firstOrNull { it.name == name }
    }
}
