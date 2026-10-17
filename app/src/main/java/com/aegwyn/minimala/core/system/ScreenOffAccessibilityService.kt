package com.aegwyn.minimala.core.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Exists solely so double-tap-to-sleep can call GLOBAL_ACTION_LOCK_SCREEN — a launcher has no
 * other way to turn off the screen without being a device owner. The user enables this once
 * under Settings > Accessibility; until then [instance] stays null and the gesture is a no-op.
 */
class ScreenOffAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        var instance: ScreenOffAccessibilityService? = null
            private set
    }
}
