package io.github.halcyonashes.minimala.core.calendar.nepali

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val NepaliMonths = arrayOf(
    "बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
    "कार्तिक", "मंसिर", "पुष", "माघ", "फागुन", "चैत",
)

private val NepaliWeekdays = arrayOf(
    "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार", "आइतबार",
)

/**
 * Column headers for a month grid, Sunday first — the Nepali calendar convention, unlike
 * the locale-dependent first day of week java.util.Calendar would hand us. Hardcoded rather
 * than truncating the full names: "मंगलबार" abbreviates to two code points, not one.
 */
val NepaliWeekdayInitials = listOf("आ", "सो", "मं", "बु", "बि", "शु", "श")

private const val DevanagariZero = '०'

/** ASCII digits -> Devanagari. Anything else passes through unchanged. */
fun String.toDevanagariDigits(): String = map { c ->
    if (c in '0'..'9') DevanagariZero + (c - '0') else c
}.joinToString("")

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun nepaliWeekdayName(dayOfWeek: DayOfWeek): String = NepaliWeekdays[dayOfWeek.ordinal]

fun nepaliMonthName(month: Int): String = NepaliMonths[month - 1]

/** "भदौ २०८३" — the month grid's heading. */
fun BsDate.toNepaliMonthLabel(): String =
    "${nepaliMonthName(month)} ${year.toString().toDevanagariDigits()}"

/** "भदौ २४, २०८३" */
fun BsDate.toNepaliLabel(): String =
    "${NepaliMonths[month - 1]} ${dayOfMonth.toString().toDevanagariDigits()}, " +
        year.toString().toDevanagariDigits()

/**
 * "बुधबार, भदौ २४, २०८३", or null when [date] falls outside the BS lookup table so the
 * caller can fall back to a Gregorian label instead of showing a wrong Nepali one.
 */
fun nepaliFullLabel(date: LocalDate): String? {
    val bs = BikramSambat.fromGregorian(date) ?: return null
    return "${nepaliWeekdayName(date.dayOfWeek)}, ${bs.toNepaliLabel()}"
}
