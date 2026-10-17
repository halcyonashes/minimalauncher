package io.github.halcyonashes.minimala.ui.home

import android.content.Context
import android.text.format.DateFormat
import io.github.halcyonashes.minimala.core.settings.ClockStyle
import io.github.halcyonashes.minimala.core.settings.DateStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Formats [nowMillis] as clock text per [style], honouring the device's 12h/24h preference. */
fun formatClockText(nowMillis: Long, style: ClockStyle, context: Context): String {
    if (style == ClockStyle.NUMERIC) {
        return DateFormat.getTimeFormat(context).format(nowMillis)
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val minute = cal.get(Calendar.MINUTE)
    val hour = if (is24Hour) {
        cal.get(Calendar.HOUR_OF_DAY)
    } else {
        cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    }
    val isAm = cal.get(Calendar.AM_PM) == Calendar.AM

    return when (style) {
        ClockStyle.ROMAN_NUMERAL -> {
            val amPmSuffix = if (is24Hour) "" else if (isAm) " AM" else " PM"
            "${toRomanNumeral(hour)}:${toRomanNumeral(minute)}$amPmSuffix"
        }
        ClockStyle.WORDED -> {
            val amPmSuffix = if (is24Hour) "" else if (isAm) " AM" else " PM"
            val minuteWords = if (minute == 0) "o'clock" else numberToWords(minute)
            "${numberToWords(hour)} $minuteWords$amPmSuffix".replaceFirstChar { it.uppercase() }
        }
        ClockStyle.JAPANESE -> {
            // 午前 (gozen) / 午後 (gogo) precede the hour in Japanese, unlike the English AM/PM
            // suffix, and a whole hour omits "〇分" entirely rather than spelling out zero minutes.
            val amPmPrefix = if (is24Hour) "" else if (isAm) "午前" else "午後"
            val minutePart = if (minute == 0) "" else "${toKanjiNumeral(minute)}分"
            "$amPmPrefix${toKanjiNumeral(hour)}時$minutePart"
        }
        ClockStyle.NUMERIC -> error("handled above")
    }
}

// Roman numerals have no digit for zero; "N" (from "nulla") is the historical convention used
// for a zero minute, e.g. on the Ulm astronomical clock's roman-numeral inscriptions.
private val romanValues = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
    100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
    10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)

fun toRomanNumeral(number: Int): String {
    if (number == 0) return "N"
    var remaining = number
    val sb = StringBuilder()
    for ((value, symbol) in romanValues) {
        while (remaining >= value) {
            sb.append(symbol)
            remaining -= value
        }
    }
    return sb.toString()
}

private val onesWords = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
)
private val tensWords = listOf(
    "", "", "twenty", "thirty", "forty", "fifty",
)

/** Spells out any number from 0 to 59 — the full range a clock's hour or minute field needs. */
fun numberToWords(number: Int): String {
    if (number < 20) return onesWords[number]
    val tens = number / 10
    val ones = number % 10
    return if (ones == 0) tensWords[tens] else "${tensWords[tens]}-${onesWords[ones]}"
}

private val englishDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH)
private val englishWeekdayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
private val japaneseWeekdayFormat = SimpleDateFormat("EEEE", Locale.JAPANESE)

/**
 * Weekday/month order and punctuation vary by style — English reads "Monday, Aug 24", Japanese
 * reads "八月二十四日(月)", and Roman numerals read "MONDAY, VIII.XXIV". SimpleDateFormat's M/d
 * fields only ever emit arabic digits, so the Japanese and Roman month/day are built by hand from
 * [toKanjiNumeral]/[toRomanNumeral] instead of a format pattern. Keyed off the user's explicit
 * setting, not the phone's system locale, since only these styles are supported.
 */
fun formatDateText(nowMillis: Long, style: DateStyle): String = when (style) {
    DateStyle.ENGLISH -> englishDateFormat.format(nowMillis)
    DateStyle.JAPANESE -> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val month = toKanjiNumeral(cal.get(Calendar.MONTH) + 1)
        val day = toKanjiNumeral(cal.get(Calendar.DAY_OF_MONTH))
        val weekday = japaneseWeekdayFormat.format(nowMillis)
        "${month}月${day}日(${weekday})"
    }
    DateStyle.ROMAN_NUMERAL -> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val month = toRomanNumeral(cal.get(Calendar.MONTH) + 1)
        val day = toRomanNumeral(cal.get(Calendar.DAY_OF_MONTH))
        val weekday = englishWeekdayFormat.format(nowMillis).uppercase()
        "$weekday, $month.$day"
    }
}

private val kanjiDigits = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")

/** Kansuji (漢数字) numerals — covers 0–59, since a 24-hour clock's midnight hour is 0. */
fun toKanjiNumeral(number: Int): String {
    if (number == 0) return "零"
    val tens = number / 10
    val ones = number % 10
    val tensPart = when (tens) {
        0 -> ""
        1 -> "十"
        else -> "${kanjiDigits[tens]}十"
    }
    return tensPart + kanjiDigits[ones]
}
