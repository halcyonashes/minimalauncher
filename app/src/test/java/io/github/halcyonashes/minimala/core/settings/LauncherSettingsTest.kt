package io.github.halcyonashes.minimala.core.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSettingsTest {

    // Same configuration SettingsRepository decodes the stored blob with.
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `calendar system is anno domini by default`() {
        assertEquals(CalendarSystem.AD, LauncherSettings().calendarSystem)
    }

    @Test
    fun `blob stored before the setting existed decodes as anno domini`() {
        val decoded = json.decodeFromString<LauncherSettings>("""{"paletteId":"light"}""")
        assertEquals("light", decoded.paletteId)
        assertEquals(CalendarSystem.AD, decoded.calendarSystem)
    }

    @Test
    fun `bikram sambat survives a round trip`() {
        val settings = LauncherSettings(calendarSystem = CalendarSystem.BS)
        assertEquals(settings, json.decodeFromString<LauncherSettings>(json.encodeToString(settings)))
    }

    @Test
    fun `blob stored before the scratch pad existed decodes with an empty note`() {
        val decoded = json.decodeFromString<LauncherSettings>("""{"paletteId":"light"}""")
        assertEquals("", decoded.scratchPad)
    }

    @Test
    fun `multi-line scratch pad survives a round trip`() {
        val settings = LauncherSettings(scratchPad = "milk\n\"eggs\"\n")
        assertEquals(settings, json.decodeFromString<LauncherSettings>(json.encodeToString(settings)))
    }
}
