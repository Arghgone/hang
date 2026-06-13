package com.argh.hang

/**
 * Actions that can be performed on Hang itself and require authorization.
 *
 * Keywords are the FALLBACK detection layer. Primary detection combines:
 *  1. Settings-context classification (management packages + activity class names).
 *  2. These keywords as a safety net across Android versions and OEM skins.
 *
 * APP_LAUNCH is removed — Hang's settings screen can be opened freely.
 * Only destructive/weakening actions require authorization.
 */
enum class ProtectedAction(val label: String, val keywords: List<String>) {

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
            // XOS / XHide additions
            "xhide", "hidden apps", "hide icon", "app vault",
            "private space", "privacy space", "app visibility",
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
            // XOS additions
            "xos freezer", "app freezer", "freeze app", "frozen", "freezing",
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
            "adaptive battery", "background usage limits", "never sleeping apps",
            "aggressive battery", "high power",
            // XOS additions
            "ultra power", "extreme battery", "super saving", "autostart",
            "background kill", "power center", "allowed apps",
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
