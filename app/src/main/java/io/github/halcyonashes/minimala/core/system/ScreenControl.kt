package io.github.halcyonashes.minimala.core.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build

/**
 * Turns the screen off via the accessibility service, if the user has enabled it under
 * Settings > Accessibility. GLOBAL_ACTION_LOCK_SCREEN only exists from API 28, below the app's
 * minSdk 26, so older devices and an unconnected service both fall through to a silent no-op —
 * there's nothing actionable to tell the user beyond "go enable the service".
 */
fun turnScreenOff() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    ScreenOffAccessibilityService.instance?.performGlobalAction(
        AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
    )
}

/**
 * Expands the notification shade via the hidden StatusBarManager API. There is no public API for
 * this; EXPAND_STATUS_BAR is a normal permission any app can hold, and reflection here is the
 * long-standing workaround every third-party launcher uses for swipe-down-for-notifications.
 */
fun Context.expandStatusBar() {
    runCatching {
        val statusBarService = getSystemService("statusbar")
        val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
        statusBarManagerClass.getMethod("expandNotificationsPanel").invoke(statusBarService)
    }
}
