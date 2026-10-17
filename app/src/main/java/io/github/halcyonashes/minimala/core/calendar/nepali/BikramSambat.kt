package io.github.halcyonashes.minimala.core.calendar.nepali

import java.time.LocalDate

/** A date in the Bikram Sambat calendar. [month] is 1..12, Baisakh..Chaitra. */
data class BsDate(val year: Int, val month: Int, val dayOfMonth: Int)

/**
 * Gregorian -> Bikram Sambat conversion.
 *
 * BS month lengths are not derivable from a formula - they are published per year by the
 * Nepali Panchanga committee, so every implementation carries the same lookup table. This
 * one is ported from remotemerge/nepali-date-converter (MIT), covering BS 1975..2099 and
 * anchored at BS 1975-01-01 == 1918-04-13 AD, i.e. 1918-04-13 .. 2043-04-13 Gregorian.
 */
object BikramSambat {

    private const val FIRST_BS_YEAR = 1975
    private val EPOCH: LocalDate = LocalDate.of(1918, 4, 13)

    /** Returns null outside the table's range rather than guessing month lengths. */
    fun fromGregorian(date: LocalDate): BsDate? {
        var remaining = date.toEpochDay() - EPOCH.toEpochDay()
        if (remaining < 0) return null

        var yearIndex = 0
        while (yearIndex < MONTH_LENGTHS.size) {
            val yearLength = MONTH_LENGTHS[yearIndex].sum()
            if (remaining < yearLength) break
            remaining -= yearLength
            yearIndex++
        }
        if (yearIndex == MONTH_LENGTHS.size) return null

        val months = MONTH_LENGTHS[yearIndex]
        var monthIndex = 0
        while (remaining >= months[monthIndex]) {
            remaining -= months[monthIndex]
            monthIndex++
        }
        return BsDate(
            year = FIRST_BS_YEAR + yearIndex,
            month = monthIndex + 1,
            dayOfMonth = remaining.toInt() + 1,
        )
    }

    /** Length of a BS month, or null if the year/month is outside the table. */
    fun daysInMonth(year: Int, month: Int): Int? {
        val months = monthsOf(year) ?: return null
        return if (month in 1..12) months[month - 1] else null
    }

    /**
     * Inverse of [fromGregorian]. Needed by the month grid: a BS month has no fixed weekday
     * offset, so the only way to know which column its 1st falls in is to ask the Gregorian
     * calendar what day of the week that date actually was.
     */
    fun toGregorian(date: BsDate): LocalDate? {
        val months = monthsOf(date.year) ?: return null
        if (date.month !in 1..12) return null
        if (date.dayOfMonth !in 1..months[date.month - 1]) return null

        var days = 0L
        for (i in 0 until date.year - FIRST_BS_YEAR) days += MONTH_LENGTHS[i].sum()
        for (m in 0 until date.month - 1) days += months[m]
        return EPOCH.plusDays(days + date.dayOfMonth - 1)
    }

    private fun monthsOf(year: Int): IntArray? =
        MONTH_LENGTHS.getOrNull(year - FIRST_BS_YEAR)

    // BS 1975 .. 2099, twelve month lengths each.
    private val MONTH_LENGTHS = arrayOf(
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 1975
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 1980
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 1985
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 1990
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 1995
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2000
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2005
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2010
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2015
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2020
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2025
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2030
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2035
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2040
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2045
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2050
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2055
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2060
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2065
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2070
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2075
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2080
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2085
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2090
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2095
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2099
    )
}
