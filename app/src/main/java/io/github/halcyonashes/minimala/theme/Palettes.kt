package io.github.halcyonashes.minimala.theme

import androidx.compose.ui.graphics.Color

private val Dark = LauncherPalette(
    id = "dark",
    displayName = "Dark",
    isDark = true,
    background = Color(0xFF0B0C0E),
    surface = Color(0xFF16181B),
    surfaceVariant = Color(0xFF222529),
    textPrimary = Color(0xFFEDEDEF),
    textSecondary = Color(0xFF9A9DA3),
    accent = Color(0xFFB9BEC7),
    accentMuted = Color(0xFF5C6066),
    divider = Color(0xFF2A2D31),
)

private val Light = LauncherPalette(
    id = "light",
    displayName = "Light",
    isDark = false,
    background = Color(0xFFF7F5F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEAE2),
    textPrimary = Color(0xFF1C1B19),
    textSecondary = Color(0xFF6B6862),
    accent = Color(0xFF44494F),
    accentMuted = Color(0xFFAEB2B8),
    divider = Color(0xFFE2DED4),
)

private val Dracula = LauncherPalette(
    id = "dracula",
    displayName = "Dracula",
    isDark = true,
    background = Color(0xFF282A36),
    surface = Color(0xFF2F3140),
    surfaceVariant = Color(0xFF3B3D4F),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFF9294A8),
    accent = Color(0xFFBD93F9),
    accentMuted = Color(0xFF6E5A94),
    divider = Color(0xFF44475A),
)

private val Strawberry = LauncherPalette(
    id = "strawberry",
    displayName = "Strawberry",
    isDark = true,
    background = Color(0xFF23121A),
    surface = Color(0xFF2E1821),
    surfaceVariant = Color(0xFF3D202B),
    textPrimary = Color(0xFFF7E8EC),
    textSecondary = Color(0xFFB58A94),
    accent = Color(0xFFE87A94),
    accentMuted = Color(0xFF8A4B5A),
    divider = Color(0xFF44252F),
)

private val Blueberry = LauncherPalette(
    id = "blueberry",
    displayName = "Blueberry",
    isDark = true,
    background = Color(0xFF13152A),
    surface = Color(0xFF191C36),
    surfaceVariant = Color(0xFF232748),
    textPrimary = Color(0xFFE9EAF7),
    textSecondary = Color(0xFF8D91B8),
    accent = Color(0xFF8C9EFF),
    accentMuted = Color(0xFF4E5896),
    divider = Color(0xFF272B4E),
)

private val Lemon = LauncherPalette(
    id = "lemon",
    displayName = "Lemon",
    isDark = false,
    background = Color(0xFFFDF8E3),
    surface = Color(0xFFFFFFF4),
    surfaceVariant = Color(0xFFF5EBB8),
    textPrimary = Color(0xFF3A3417),
    textSecondary = Color(0xFF8A7F42),
    accent = Color(0xFFE0B400),
    accentMuted = Color(0xFFCBBE7A),
    divider = Color(0xFFECE0A0),
)

val bundledPalettes: List<LauncherPalette> = listOf(Dark, Light, Dracula, Strawberry, Blueberry, Lemon)

fun paletteById(id: String): LauncherPalette =
    bundledPalettes.find { it.id == id } ?: Dark
