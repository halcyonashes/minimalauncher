package io.github.halcyonashes.minimala.core.calendar

data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
)
