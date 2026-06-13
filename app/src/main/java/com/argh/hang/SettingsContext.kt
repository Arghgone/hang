package com.argh.hang

/**
 * Intent-based classification of system-management surfaces.
 *
 * Recognises WHERE the user is (management package + activity class) and maps
 * that to the protected actions reachable from those screens.
 * Keyword matching is applied afterwards as a fallback.
 */
object SettingsContext {

    /** Packages that host app-management / settings UI across OEM skins. */
    val MANAGEMENT_PACKAGES: Set<String> = setOf(
        // AOSP / Pixel
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        // Package installers / stores
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.android.vending",
        // Samsung
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
        // Oppo / Realme / OnePlus
        "com.coloros.safecenter",
        "com.coloros.oppoguardelf",
        "com.oneplus.security",
        "com.oplus.battery",
        // Vivo / iQOO
        "com.iqoo.secure",
        "com.vivo.permissionmanager",
        "com.vivo.abe",
        // Transsion / XOS (Infinix, Tecno, iTel)
        "com.transsion.phonemanager",      // Phone Manager (battery, cleanup, permissions)
        "com.transsion.batterylab",        // Battery Lab / battery optimisation
        "com.transsion.applock",           // App Lock
        "com.scorpio.securitycom",         // Security (HiOS/XOS variants)
        "com.transsion.powercenter",       // Power Center / Ultra Power Saver
        "com.transsion.ossettingsext",     // OEM Settings extensions
    )

    /** Launchers monitored for hide-app / app-drawer visibility flows. */
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
        "com.transsion.XOSLauncher",       // XOS Launcher (Freezer, XHide built-in)
    )

    /** Packages whose sole purpose is hiding apps from the launcher. */
    val HIDE_APP_PACKAGES: Set<String> = setOf(
        "com.xui.xhide",                   // XHide — Infinix/XOS hide-app system
    )

    /**
     * Maps fragments of settings activity class names to the protected
     * actions reachable from those screens. Case-insensitive `contains`.
     */
    private val CLASS_NAME_ACTIONS: List<Pair<String, ProtectedAction>> = listOf(
        // App info / uninstall / disable / force stop hubs
        "appinfo"                   to ProtectedAction.UNINSTALL,
        "installedappdetails"       to ProtectedAction.UNINSTALL,
        "uninstall"                 to ProtectedAction.UNINSTALL,
        "manageapplications"        to ProtectedAction.UNINSTALL,
        "appmanagement"             to ProtectedAction.UNINSTALL,
        // Notifications
        "notificationsettings"      to ProtectedAction.NOTIFICATIONS,
        "channelnotification"       to ProtectedAction.NOTIFICATIONS,
        "appnotification"           to ProtectedAction.NOTIFICATIONS,
        "notificationaccess"        to ProtectedAction.NOTIFICATIONS,
        "notificationassistant"     to ProtectedAction.NOTIFICATIONS,
        "conversationsettings"      to ProtectedAction.NOTIFICATIONS,
        "zenmode"                   to ProtectedAction.NOTIFICATIONS,
        "statusbar"                 to ProtectedAction.NOTIFICATIONS,
        // Accessibility
        "accessibilitysettings"     to ProtectedAction.ACCESSIBILITY,
        "accessibilitydetails"      to ProtectedAction.ACCESSIBILITY,
        "toggleaccessibilityservice" to ProtectedAction.ACCESSIBILITY,
        // Device admin
        "deviceadmin"               to ProtectedAction.DEVICE_ADMIN,
        // Permissions
        "permission"                to ProtectedAction.PERMISSIONS,
        // Overlay
        "drawoverlay"               to ProtectedAction.OVERLAY,
        "manageoverlay"             to ProtectedAction.OVERLAY,
        "appdrawoverlay"            to ProtectedAction.OVERLAY,
        // Battery / background / power saving
        "highpower"                 to ProtectedAction.BATTERY,
        "batteryoptim"              to ProtectedAction.BATTERY,
        "batterysaver"              to ProtectedAction.BATTERY,
        "powersav"                  to ProtectedAction.BATTERY,
        "powerusage"                to ProtectedAction.BATTERY,
        "batteryusage"              to ProtectedAction.BATTERY,
        "backgroundrestrict"        to ProtectedAction.BATTERY,
        // Hibernation / freezing / archiving
        "hibernation"               to ProtectedAction.FREEZE,
        "unusedapps"                to ProtectedAction.FREEZE,
        "sleepingapps"              to ProtectedAction.FREEZE,
        "appfreezer"                to ProtectedAction.FREEZE,
        // Hide app / launcher visibility
        "hideapps"                  to ProtectedAction.HIDE,
        "hiddenapps"                to ProtectedAction.HIDE,
        "homescreensettings"        to ProtectedAction.HIDE,
        // Special access
        "specialappaccess"          to ProtectedAction.SPECIAL_ACCESS,
        "specialaccess"             to ProtectedAction.SPECIAL_ACCESS,
        "usageaccess"               to ProtectedAction.USAGE_ACCESS,
        // XOS Phone Manager / Battery Lab
        "powersavemanage"           to ProtectedAction.BATTERY,
        "appmanage"                 to ProtectedAction.UNINSTALL,
        "permmanage"                to ProtectedAction.PERMISSIONS,
        "freezemanage"              to ProtectedAction.FREEZE,
        "appfreeze"                 to ProtectedAction.FREEZE,
        "frozenapps"                to ProtectedAction.FREEZE,
        // XHide
        "xhide"                     to ProtectedAction.HIDE,
        "hiddenapplication"         to ProtectedAction.HIDE,
        "hidesetting"               to ProtectedAction.HIDE,
        // XOS Launcher long-press / drawer management
        "drawersettings"            to ProtectedAction.HIDE,
        "appvisibility"             to ProtectedAction.HIDE,
        // Power / Ultra power saver
        "ultrapowersaving"          to ProtectedAction.BATTERY,
        "extremebatterysaver"       to ProtectedAction.BATTERY,
        "superpowersave"            to ProtectedAction.BATTERY,
        // OEM Security / Permission controller
        "securitycenter"            to ProtectedAction.PERMISSIONS,
        "autostart"                 to ProtectedAction.BATTERY,
        // XOS Autostart / Startup manager
        "startupmanager"            to ProtectedAction.BATTERY,
        "bootmanage"                to ProtectedAction.BATTERY,
        // XOS Launcher Freezer
        "freezemanager"             to ProtectedAction.FREEZE,
        "frozenappsactivity"        to ProtectedAction.FREEZE,
        "freezer"                   to ProtectedAction.FREEZE,
        // XHide list
        "hidelist"                  to ProtectedAction.HIDE,
        // UPS allowed apps
        "allowedapps"               to ProtectedAction.BATTERY,
        "powermanage"               to ProtectedAction.BATTERY,
    )

    fun isManagementContext(packageName: String): Boolean =
        packageName in MANAGEMENT_PACKAGES ||
            packageName in LAUNCHER_PACKAGES ||
            packageName in HIDE_APP_PACKAGES

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
