package com.aegwyn.minimala.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper over [CalendarContract]. No local DB — the system calendar provider
 * is already the source of truth, so mirroring it into Room would just be a cache
 * that can drift from the real thing.
 */
class CalendarRepository(private val context: Context) {

    /**
     * Expands recurring events too, via the Instances table.
     *
     * Returns an empty list rather than throwing when READ_CALENDAR is missing or revoked.
     * The launcher is a long-lived process the user cannot escape, so a revoked permission
     * has to degrade to an empty state, never a SecurityException.
     */
    suspend fun eventsBetween(startMillis: Long, endMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasReadPermission()) return@withContext emptyList()

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startMillis.toString())
                .appendPath(endMillis.toString())
                .build()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )

            val events = mutableListOf<CalendarEvent>()
            runCatching {
                context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            events += CalendarEvent(
                                eventId = cursor.getLong(0),
                                calendarId = cursor.getLong(1),
                                title = cursor.getString(2) ?: "",
                                description = cursor.getString(3),
                                startMillis = cursor.getLong(4),
                                endMillis = cursor.getLong(5),
                                isAllDay = cursor.getInt(6) != 0,
                            )
                        }
                    }
            }
            events
        }

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** First calendar the user can write into, preferring the primary account. */
    suspend fun defaultWritableCalendarId(): Long? = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext null
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}",
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC",
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }.getOrNull()
    }

    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        description: String?,
        startMillis: Long,
        endMillis: Long,
    ): Uri? = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) return@withContext null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
        }
        runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }.getOrNull()
    }

    suspend fun deleteEvent(eventId: Long): Int = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) return@withContext 0
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        runCatching { context.contentResolver.delete(uri, null, null) }.getOrDefault(0)
    }
}
