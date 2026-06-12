package com.argh.hang

/**
 * Intent-level actions that can be protected for an app.
 *
 * Keywords are only the FALLBACK detection layer. Primary detection combines:
 *  1. protected-app launch detection (foreground package),
 *  2. settings-context classification (management packages + activity class
 *     names, see [SettingsContext]),
 *  3. sticky protected-app context tracking across nested settings screens
 *     (see [TargetContextTracker]),
 *  4. these keywords as a safety net across Android versions and OEM skins.
 */
enum class ProtectedAction(val label: String, val keywords: List<String>) {

    APP_LAUNCH(
        "Opening the app",
        emptyList(),
    ),
    UNINSTALL(
        "Uninstall / remove",
        listOf("uninstall", "remove app", "delete app", "remove this app", "app info", "app management"),
    ),
    DISABLE(
        "Disable / turn off",
        listOf("disable app", "disable", "turn off app", "deactivate app"),
    ),
    HIDE(
        "Hide app / launcher visibility",
        listOf(
            "hide app", "hide apps", "hidden apps", "hide icon", "hide from home",
            "app visibility", "launcher visibility", "hide applications",
        ),
    ),
    FORCE_STOP(
        "Force stop",
        listOf("force stop", "force close"),
    ),
    FREEZE(
        "Freeze / hibernate / archive",
        listOf(
            "hibernat", "app freezer", "freeze", "frozen apps", "deep sleep",
            "sleeping apps", "put app to sleep", "archive app", "unused apps",
            "pause app activity",
        ),
    ),
    DEVICE_ADMIN(
        "Remove device admin",
        listOf("deactivate this device admin", "device admin", "device administrator"),
    ),
    PERMISSIONS(
        "Modify permissions",
        listOf("app permissions", "permission manager", "permissions", "revoke"),
    ),
    OVERLAY(
        "Display over other apps",
        listOf("display over other apps", "appear on top", "draw over", "overlay"),
    ),
    ACCESSIBILITY(
        "Accessibility access",
        listOf("accessibility", "installed services", "downloaded apps"),
    ),
    NOTIFICATIONS(
        "Notifications & badges",
        listOf(
            "notification access", "app notifications", "manage notifications",
            "allow notifications", "notification categories", "notification category",
            "status bar", "badge", "notification dot", "silent notifications",
            "block notifications", "notifications on lock screen", "app icon badges",
            "device & app notifications",
        ),
    ),
    BATTERY(
        "Battery & background restrictions",
        listOf(
            "battery optimization", "optimize battery usage", "battery usage",
            "unrestricted", "background activity", "background restriction",
            "restrict background", "battery saver", "power saving", "power saver",
            "ultra power", "extreme battery", "super power saving",
            "adaptive battery", "background usage limits", "never sleeping apps",
            "aggressive battery", "high power",
        ),
    ),
    SPECIAL_ACCESS(
        "Special app access",
        listOf("special app access", "special access", "modify system settings"),
    ),
    USAGE_ACCESS(
        "Usage access",
        listOf("usage access", "usage data access"),
    );

    companion object {
        fun fromName(name: String): ProtectedAction? = entries.firstOrNull { it.name == name }
    }
}
