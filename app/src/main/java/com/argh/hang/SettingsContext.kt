package com.argh.hang

/**
 * Intent-based classification of system-management surfaces.
 *
 * Instead of relying on the exact wording visible on screen, this layer
 * recognises WHERE the user is (which management package and which settings
 * activity class) and maps that location to the protected actions it can
 * perform. Keyword matching is only applied afterwards as a fallback.
 */
object SettingsContext {

    /** Packages that host app-management / settings UI across OEM skins. */
    val MANAGEMENT_PACKAGES: Set<String> = setOf(
        // AOSP / Pixel
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        // Package installers / stores (uninstall flows)
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.android.vending",
        // Samsung (Device care / Smart manager)
        "com.samsung.android.lool",
        "com.samsung.android.sm",
        "com.samsung.android.sm_cn",
        // Xiaomi / MIUI / HyperOS
        "com.miui.securitycenter",
        "com.miui.securitycore",
        "com.miui.powerkeeper",
        // Huawei / Honor
        "com.huawei.systemmanager",
        "com.hihonor.systemmanager",
        // Oppo / Realme / OnePlus (ColorOS / OxygenOS)
        "com.coloros.safecenter",
        "com.coloros.oppoguardelf",
        "com.oneplus.security",
        "com.oplus.battery",
        // Vivo / iQOO
        "com.iqoo.secure",
        "com.vivo.permissionmanager",
        "com.vivo.abe",
    )

    /** Launchers, monitored for hide-app / app-drawer visibility flows. */
    val LAUNCHER_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "net.oneplus.launcher",
        "com.oppo.launcher",
        "com.bbk.launcher2",
        "com.microsoft.launcher",
        "com.teslacoilsw.launcher",
    )

    /**
     * Maps fragments of settings activity class names to the protected
     * actions reachable from those screens. Matching is case-insensitive
     * `contains`, so it is robust against OEM subclassing.
     */
    private val CLASS_NAME_ACTIONS: List<Pair<String, ProtectedAction>> = listOf(
        // App info / uninstall / disable / force stop hubs
        "appinfo" to ProtectedAction.UNINSTALL,
        "installedappdetails" to ProtectedAction.UNINSTALL,
        "uninstall" to ProtectedAction.UNINSTALL,
        "manageapplications" to ProtectedAction.UNINSTALL,
        "appmanagement" to ProtectedAction.UNINSTALL,
        // Notifications: app pages, channels/categories, access, status bar
        "notificationsettings" to ProtectedAction.NOTIFICATIONS,
        "channelnotification" to ProtectedAction.NOTIFICATIONS,
        "appnotification" to ProtectedAction.NOTIFICATIONS,
        "notificationaccess" to ProtectedAction.NOTIFICATIONS,
        "notificationassistant" to ProtectedAction.NOTIFICATIONS,
        "conversationsettings" to ProtectedAction.NOTIFICATIONS,
        "zenmode" to ProtectedAction.NOTIFICATIONS,
        "statusbar" to ProtectedAction.NOTIFICATIONS,
        // Accessibility service management
        "accessibilitysettings" to ProtectedAction.ACCESSIBILITY,
        "accessibilitydetails" to ProtectedAction.ACCESSIBILITY,
        "toggleaccessibilityservice" to ProtectedAction.ACCESSIBILITY,
        // Device admin
        "deviceadmin" to ProtectedAction.DEVICE_ADMIN,
        // Permissions
        "permission" to ProtectedAction.PERMISSIONS,
        // Overlay
        "drawoverlay" to ProtectedAction.OVERLAY,
        "manageoverlay" to ProtectedAction.OVERLAY,
        "appdrawoverlay" to ProtectedAction.OVERLAY,
        // Battery / background / power saving
        "highpower" to ProtectedAction.BATTERY,
        "batteryoptim" to ProtectedAction.BATTERY,
        "batterysaver" to ProtectedAction.BATTERY,
        "powersav" to ProtectedAction.BATTERY,
        "powerusage" to ProtectedAction.BATTERY,
        "batteryusage" to ProtectedAction.BATTERY,
        "backgroundrestrict" to ProtectedAction.BATTERY,
        // Hibernation / freezing / archiving
        "hibernation" to ProtectedAction.FREEZE,
        "unusedapps" to ProtectedAction.FREEZE,
        "sleepingapps" to ProtectedAction.FREEZE,
        "appfreezer" to ProtectedAction.FREEZE,
        // Hide app / launcher visibility
        "hideapps" to ProtectedAction.HIDE,
        "hiddenapps" to ProtectedAction.HIDE,
        "homescreensettings" to ProtectedAction.HIDE,
        // Special app access / usage access
        "specialappaccess" to ProtectedAction.SPECIAL_ACCESS,
        "specialaccess" to ProtectedAction.SPECIAL_ACCESS,
        "usageaccess" to ProtectedAction.USAGE_ACCESS,
    )

    fun isManagementContext(packageName: String): Boolean =
        packageName in MANAGEMENT_PACKAGES || packageName in LAUNCHER_PACKAGES

    /** Actions reachable from the given settings activity class, if known. */
    fun actionsForClassName(className: String?): Set<ProtectedAction> {
        if (className.isNullOrBlank()) return emptySet()
        val lower = className.lowercase()
        return CLASS_NAME_ACTIONS
            .filter { (fragment, _) -> lower.contains(fragment) }
            .map { it.second }
            .toSet()
    }
}
