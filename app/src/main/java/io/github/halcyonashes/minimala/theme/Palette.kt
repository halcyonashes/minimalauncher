package io.github.halcyonashes.minimala.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LauncherPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentMuted: Color,
    val divider: Color,
)

/**
 * Painted over the wallpaper instead of [background] so the wallpaper stays visible behind the
 * panels — the window is transparent with windowShowWallpaper=true, and a fully opaque fill
 * would defeat that.
 *
 * [opacity] is the user's Background setting: 0 leaves the wallpaper untouched, 1 covers it
 * completely. Coerced here rather than trusted, because it round-trips through stored JSON that
 * a hand-edited or older build could put out of range.
 */
fun LauncherPalette.wallpaperScrim(opacity: Float): Color =
    background.copy(alpha = opacity.coerceIn(0f, 1f))
