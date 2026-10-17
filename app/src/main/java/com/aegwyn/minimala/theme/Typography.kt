package com.aegwyn.minimala.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aegwyn.minimala.core.settings.FontWeightOption

fun FontWeightOption.toComposeWeight(): FontWeight = when (this) {
    FontWeightOption.LIGHT -> FontWeight.Light
    FontWeightOption.REGULAR -> FontWeight.Normal
    FontWeightOption.MEDIUM -> FontWeight.Medium
}

/** Base type scale (Material3's default sizes) that [scale] multiplies uniformly. */
private data class ScaleSpec(val size: Int, val lineHeight: Int)

private val baseScales = mapOf(
    "displayLarge" to ScaleSpec(57, 64),
    "displayMedium" to ScaleSpec(45, 52),
    "displaySmall" to ScaleSpec(36, 44),
    "headlineLarge" to ScaleSpec(32, 40),
    "headlineMedium" to ScaleSpec(28, 36),
    "headlineSmall" to ScaleSpec(24, 32),
    "titleLarge" to ScaleSpec(22, 28),
    "titleMedium" to ScaleSpec(16, 24),
    "titleSmall" to ScaleSpec(14, 20),
    "bodyLarge" to ScaleSpec(16, 24),
    "bodyMedium" to ScaleSpec(14, 20),
    "bodySmall" to ScaleSpec(12, 16),
    "labelLarge" to ScaleSpec(14, 20),
    "labelMedium" to ScaleSpec(12, 16),
    "labelSmall" to ScaleSpec(11, 16),
)

/** Builds a full [Typography] from the user's chosen family/scale/weight in one place. */
fun buildTypography(family: FontFamily, scale: Float, weight: FontWeightOption): Typography {
    val composeWeight = weight.toComposeWeight()
    fun style(key: String) = baseScales.getValue(key).let {
        TextStyle(
            fontFamily = family,
            fontWeight = composeWeight,
            fontSize = (it.size * scale).sp,
            lineHeight = (it.lineHeight * scale).sp,
        )
    }
    return Typography(
        displayLarge = style("displayLarge"),
        displayMedium = style("displayMedium"),
        displaySmall = style("displaySmall"),
        headlineLarge = style("headlineLarge"),
        headlineMedium = style("headlineMedium"),
        headlineSmall = style("headlineSmall"),
        titleLarge = style("titleLarge"),
        titleMedium = style("titleMedium"),
        titleSmall = style("titleSmall"),
        bodyLarge = style("bodyLarge"),
        bodyMedium = style("bodyMedium"),
        bodySmall = style("bodySmall"),
        labelLarge = style("labelLarge"),
        labelMedium = style("labelMedium"),
        labelSmall = style("labelSmall"),
    )
}

/** The home clock gets its own scale — it's the single dominant element on screen. */
fun clockTextStyle(family: FontFamily, clockScale: Float, weight: FontWeightOption): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight.toComposeWeight(),
    fontSize = (72 * clockScale).sp,
    lineHeight = (80 * clockScale).sp,
)
