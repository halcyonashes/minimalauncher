package io.github.halcyonashes.minimala.core.calendar.nepali

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BikramSambatTest {

    @Test
    fun `epoch maps to first day of BS 1975`() {
        assertEquals(BsDate(1975, 1, 1), BikramSambat.fromGregorian(LocalDate.of(1918, 4, 13)))
    }

    @Test
    fun `day after epoch advances the BS day`() {
        assertEquals(BsDate(1975, 1, 2), BikramSambat.fromGregorian(LocalDate.of(1918, 4, 14)))
    }

    @Test
    fun `millennium maps to Poush 17 of BS 2056`() {
        assertEquals(BsDate(2056, 9, 17), BikramSambat.fromGregorian(LocalDate.of(2000, 1, 1)))
    }

    @Test
    fun `2024 new year maps to Poush 16 of BS 2080`() {
        assertEquals(BsDate(2080, 9, 16), BikramSambat.fromGregorian(LocalDate.of(2024, 1, 1)))
    }

    @Test
    fun `mid Bhadra 2083 round trips`() {
        assertEquals(BsDate(2083, 5, 24), BikramSambat.fromGregorian(LocalDate.of(2026, 9, 9)))
    }

    @Test
    fun `dates before the epoch are unsupported`() {
        assertNull(BikramSambat.fromGregorian(LocalDate.of(1918, 4, 12)))
    }

    @Test
    fun `last day of the table converts`() {
        assertEquals(BsDate(2099, 12, 30), BikramSambat.fromGregorian(LocalDate.of(2043, 4, 13)))
    }

    @Test
    fun `dates past the table are unsupported`() {
        assertNull(BikramSambat.fromGregorian(LocalDate.of(2043, 4, 14)))
    }

    @Test
    fun `label renders Devanagari digits and month name`() {
        val label = nepaliFullLabel(LocalDate.of(2026, 9, 9))
        assertEquals("बुधबार, भदौ २४, २०८३", label)
    }

    @Test
    fun `label is null outside the supported range`() {
        assertNull(nepaliFullLabel(LocalDate.of(2043, 4, 14)))
    }
}
