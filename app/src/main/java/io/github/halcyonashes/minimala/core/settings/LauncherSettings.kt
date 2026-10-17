package io.github.halcyonashes.minimala.core.settings

import kotlinx.serialization.Serializable

enum class FontWeightOption { LIGHT, REGULAR, MEDIUM }

enum class ClockStyle(val displayName: String) {
    NUMERIC("Numeric"),
    ROMAN_NUMERAL("Roman numerals"),
    WORDED("Worded"),
    JAPANESE("Japanese"),
}

/** Styling of the day-of-week/month text — independent of the phone's system locale. */
enum class DateStyle(val displayName: String) {
    ENGLISH("English"),
    JAPANESE("Japanese"),
    ROMAN_NUMERAL("Roman numerals"),
}

/** Calendar for the left panel's month grid and the right panel's day labels. */
enum class CalendarSystem(val displayName: String) {
    AD("Anno Domini"),
    BS("Bikram Sambat"),
}

/** Immutable snapshot of every user-configurable launcher preference. */
@Serializable
data class LauncherSettings(
    val paletteId: String = "dark",
    val fontId: String = "system",
    val fontScale: Float = 1.0f,
    val fontWeight: FontWeightOption = FontWeightOption.REGULAR,
    val clockScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.NUMERIC,
    val dateStyle: DateStyle = DateStyle.ENGLISH,
    val calendarSystem: CalendarSystem = CalendarSystem.AD,
    val dayStartMinute: Int = 6 * 60,
    val dayEndMinute: Int = 22 * 60,
    val favoritePackageKeys: List<String> = emptyList(),
    val frequentAppKeys: List<String> = emptyList(),
    val hiddenAppKeys: Set<String> = emptySet(),
    val shortcutGridKeys: List<String> = emptyList(),
    val appRenames: Map<String, String> = emptyMap(),
    val weatherWidgetEnabled: Boolean = false,
    /**
     * Alpha of the scrim painted over the wallpaper: 0 = wallpaper untouched, 1 = fully
     * covered by the palette background, so the wallpaper is not visible at all.
     */
    val backgroundOpacity: Float = 0.45f,
)
