package com.aegwyn.minimala.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import com.aegwyn.minimala.core.settings.FontWeightOption

private const val PaletteCrossfadeMillis = 260

val LocalLauncherPalette = compositionLocalOf { paletteById("dark") }
val LocalClockTextStyle = compositionLocalOf<TextStyle> { error("MinimalaTheme not applied") }

@Composable
fun MinimalaTheme(
    paletteId: String,
    fontId: String,
    fontScale: Float,
    fontWeight: FontWeightOption,
    clockScale: Float,
    content: @Composable () -> Unit,
) {
    val palette = paletteById(paletteId)
    val font = fontById(fontId)
    val animSpec = tween<androidx.compose.ui.graphics.Color>(PaletteCrossfadeMillis)

    val background by animateColorAsState(palette.background, animSpec, label = "background")
    val surface by animateColorAsState(palette.surface, animSpec, label = "surface")
    val surfaceVariant by animateColorAsState(palette.surfaceVariant, animSpec, label = "surfaceVariant")
    val textPrimary by animateColorAsState(palette.textPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(palette.textSecondary, animSpec, label = "textSecondary")
    val accent by animateColorAsState(palette.accent, animSpec, label = "accent")
    val accentMuted by animateColorAsState(palette.accentMuted, animSpec, label = "accentMuted")
    val divider by animateColorAsState(palette.divider, animSpec, label = "divider")

    val animatedPalette = palette.copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        accent = accent,
        accentMuted = accentMuted,
        divider = divider,
    )

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            primary = accent,
            onPrimary = surface,
            secondary = accentMuted,
            outline = divider,
        ).withPaletteContainers(palette = animatedPalette)
    } else {
        lightColorScheme(
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            primary = accent,
            onPrimary = surface,
            secondary = accentMuted,
            outline = divider,
        ).withPaletteContainers(palette = animatedPalette)
    }

    val typography = buildTypography(font.family, fontScale, fontWeight)
    val clockStyle = clockTextStyle(font.family, clockScale, fontWeight)

    CompositionLocalProvider(
        LocalLauncherPalette provides animatedPalette,
        LocalClockTextStyle provides clockStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/**
 * Overrides every remaining container slot with palette colours.
 *
 * The base [darkColorScheme]/[lightColorScheme] factories fill anything unspecified with
 * Material's baseline purple, and stock components reach for those slots — a Slider's inactive
 * track is `secondaryContainer`, not `surfaceVariant` — so a partial mapping leaks lavender into
 * a themed launcher. Mapping the containers explicitly is what makes "no hardcoded colour
 * survives a palette switch" actually true.
 */
private fun ColorScheme.withPaletteContainers(palette: LauncherPalette): ColorScheme = copy(
    primaryContainer = palette.accentMuted,
    onPrimaryContainer = palette.textPrimary,
    inversePrimary = palette.accentMuted,
    onSecondary = palette.surface,
    secondaryContainer = palette.surfaceVariant,
    onSecondaryContainer = palette.textPrimary,
    tertiary = palette.accent,
    onTertiary = palette.surface,
    tertiaryContainer = palette.surfaceVariant,
    onTertiaryContainer = palette.textPrimary,
    surfaceContainerLowest = palette.background,
    surfaceContainerLow = palette.surface,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surfaceVariant,
    surfaceContainerHighest = palette.surfaceVariant,
    surfaceBright = palette.surface,
    surfaceDim = palette.background,
    surfaceTint = palette.accent,
    outlineVariant = palette.divider,
    scrim = palette.background,
)
