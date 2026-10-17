package io.github.halcyonashes.minimala.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.halcyonashes.minimala.R

data class LauncherFont(val id: String, val displayName: String, val family: FontFamily)

/**
 * Each bundled family ships as a single variable-weight (`wght` axis) TrueType file rather than
 * three separate static files per weight. Android has instanced variable-font axes natively
 * since API 26 — this project's minSdk — so Light/Regular/Medium below are three views of the
 * same file, not three files, and the settings screen's weight picker (see
 * `LauncherSettings.fontWeight`) actually changes the rendered glyphs instead of being a no-op.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int): FontFamily = FontFamily(
    Font(resId, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(resId, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

val bundledFonts: List<LauncherFont> = listOf(
    LauncherFont("system", "System", FontFamily.Default),
    LauncherFont("inter", "Inter", variableFont(R.font.inter)),
    LauncherFont("jetbrains_mono", "JetBrains Mono", variableFont(R.font.jetbrains_mono)),
    LauncherFont("outfit", "Outfit", variableFont(R.font.outfit)),
    LauncherFont("space_grotesk", "Space Grotesk", variableFont(R.font.space_grotesk)),
    LauncherFont("lora", "Lora", variableFont(R.font.lora)),
)

fun fontById(id: String): LauncherFont =
    bundledFonts.find { it.id == id } ?: bundledFonts.first()
