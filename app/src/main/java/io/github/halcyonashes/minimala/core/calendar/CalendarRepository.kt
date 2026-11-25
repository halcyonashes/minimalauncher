package io.github.halcyonashes.minimala.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val QueryMarginMillis = 24 * 60 * 60 * 1000L

/**
 * Thin wrapper over [CalendarContract]. No local DB — the system calendar provider
 * is already the source of truth, so mirroring it into Room would just be a cache
 * that can drift from the real thing.
 */
class CalendarRepository(private val context: Context) {

    private val permissionEpoch = MutableStateFlow(0)

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

            // All-day instances are stored at UTC midnight, so their raw span is off from the
            // local day by the zone offset. A day of margin on each side covers any offset
            // (max ±14 h) so the provider-side filter can't drop one; the exact overlap is
            // re-checked below, after the conversion to local days.
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath((startMillis - QueryMarginMillis).toString())
                .appendPath((endMillis + QueryMarginMillis).toString())
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

            // Hidden calendars, declined invites and cancelled events are not on the user's
            // agenda. NULL status columns are common (local calendars, events with no
            // attendees), and `NULL != x` is not true in SQL, so they are let through explicitly.
            val selection = "${CalendarContract.Instances.VISIBLE} = 1" +
                " AND (${CalendarContract.Instances.SELF_ATTENDEE_STATUS} IS NULL" +
                " OR ${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED})" +
                " AND (${CalendarContract.Instances.STATUS} IS NULL" +
                " OR ${CalendarContract.Instances.STATUS} != ${CalendarContract.Events.STATUS_CANCELED})"

            val zone = ZoneId.systemDefault()
            val events = mutableListOf<CalendarEvent>()
            runCatching {
                context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")
                    ?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val isAllDay = cursor.getInt(6) != 0
                            val begin = cursor.getLong(4)
                            val end = cursor.getLong(5)
                            events += CalendarEvent(
                                eventId = cursor.getLong(0),
                                calendarId = cursor.getLong(1),
                                title = cursor.getString(2) ?: "",
                                description = cursor.getString(3),
                                startMillis = if (isAllDay) utcMidnightToLocal(begin, zone) else begin,
                                endMillis = if (isAllDay) utcMidnightToLocal(end, zone) else end,
                                isAllDay = isAllDay,
                            )
                        }
                    }
            }
            events
                // The widened query window pulls in neighbours; keep only what overlaps the
                // caller's window. The second clause keeps zero-length events at its start.
                .filter { it.startMillis < endMillis && (it.endMillis > startMillis || it.startMillis >= startMillis) }
                // An invite shared across several synced accounts is stored once per calendar,
                // so collapse copies that match on what the user actually sees.
                .distinctBy { listOf(it.title, it.startMillis, it.endMillis, it.isAllDay) }
        }

    /** The same calendar date, moved from UTC midnight to local midnight. */
    private fun utcMidnightToLocal(utcMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Emits whenever anything in the calendar provider changes (sync, edits from other apps).
     * Silent without READ_CALENDAR — registering an observer on a provider you can't read
     * throws — and restarted by [onPermissionResult] so a grant takes effect without a resubscribe.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun changes(): Flow<Unit> = permissionEpoch.flatMapLatest {
        callbackFlow {
            if (!hasReadPermission()) {
                awaitClose {}
                return@callbackFlow
            }
            // Null handler: onChange arrives on a binder thread, and trySend is thread-safe.
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            val registered = runCatching {
                context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            }.isSuccess
            awaitClose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    /** Call when a calendar permission request returns, so [changes] re-checks the grant. */
    fun onPermissionResult() {
        permissionEpoch.update { it + 1 }
    }

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /**
     * First calendar the user can write into, preferring the primary account. Hidden or
     * unsynced calendars are skipped — an event written there would never be seen — and the
     * `_ID` tie-break keeps the choice stable when several calendars rank equally.
     */
    suspend fun defaultWritableCalendarId(): Long? = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext null
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}" +
                    " AND ${CalendarContract.Calendars.VISIBLE} = 1" +
                    " AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1",
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC",
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
