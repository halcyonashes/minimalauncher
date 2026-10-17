package com.aegwyn.minimala.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Best-effort implicit-intent launch.
 *
 * Every intent this launcher fires is implicit and may have no resolver on a given device —
 * no clock app, no calendar, a stripped-down ROM with no wallpaper picker. An unhandled
 * ActivityNotFoundException in the home activity takes down the home screen itself, leaving
 * the user with nowhere to land, so these are all non-fatal by construction.
 */
fun Context.startActivitySafely(intent: Intent, missingMessage: String) {
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, missingMessage, Toast.LENGTH_SHORT).show()
    }
}
