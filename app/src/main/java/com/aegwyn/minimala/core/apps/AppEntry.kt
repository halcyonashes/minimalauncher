package com.aegwyn.minimala.core.apps

import android.content.ComponentName
import android.os.UserHandle

/** Immutable snapshot of one launchable activity, keyed by [componentName] + [user]. */
data class AppEntry(
    val componentName: ComponentName,
    val user: UserHandle,
    val label: String,
) {
    val packageName: String get() = componentName.packageName

    /** Stable identity for persistence (favorites, hidden set) that survives process death. */
    val key: String get() = "${componentName.flattenToString()}|${user.hashCode()}"
}
