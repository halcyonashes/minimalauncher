package io.github.halcyonashes.minimala.ui.right

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.calendar.CalendarEvent
import io.github.halcyonashes.minimala.core.calendar.nepali.nepaliFullLabel
import io.github.halcyonashes.minimala.core.calendar.nepali.toLocalDate
import io.github.halcyonashes.minimala.core.settings.CalendarSystem
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.ui.LauncherUiState
import io.github.halcyonashes.minimala.ui.common.PanelSpacing
import io.github.halcyonashes.minimala.ui.common.SectionCard
import io.github.halcyonashes.minimala.ui.common.SectionLabel
import io.github.halcyonashes.minimala.ui.displayLabel
import androidx.compose.ui.text.style.TextOverflow
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** The panel is a glance surface, not an agenda — anything past three is noise. */
private const val MaxUpcomingEvents = 2

@Composable
fun RightPanel(
    uiState: LauncherUiState,
    onLaunchApp: (AppEntry) -> Unit,
    onCreateEvent: (title: String) -> Unit = {},
) {
    val palette = LocalLauncherPalette.current
    var expandedEventId by remember { mutableStateOf<Long?>(null) }
    var composingTitle by remember { mutableStateOf<String?>(null) }
    // Respects the user's 12h/24h system preference rather than forcing one.
    val context = LocalContext.current
    val timeFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    val calendarSystem = uiState.settings.calendarSystem
    val todayLabel = remember(calendarSystem) { dayLabel(System.currentTimeMillis(), calendarSystem) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(
                start = PanelSpacing.edge,
                end = PanelSpacing.edge,
                top = PanelSpacing.edgeTop,
                bottom = PanelSpacing.edge,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = todayLabel,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Circular affordance instead of a bare glyph: a 40dp target is reliably hittable
            // and reads as a button rather than as stray text next to the heading.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f))
                    .clickable { composingTitle = if (composingTitle == null) "" else null },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (composingTitle == null) "+" else "×",
                    color = palette.accent,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (composingTitle != null) {
            OutlinedTextField(
                value = composingTitle.orEmpty(),
                onValueChange = { composingTitle = it },
                placeholder = { Text("New event title") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val title = composingTitle.orEmpty()
                    if (title.isNotBlank()) onCreateEvent(title)
                    composingTitle = null
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PanelSpacing.label),
            )
        }

        Spacer(modifier = Modifier.height(PanelSpacing.section))

        // Filtered here rather than in the ViewModel: the left panel's month grid still wants
        // every event this week for its day dots, so weekEvents stays the unfiltered source.
        // "Not past" means not yet ended - a meeting already in progress is still upcoming.
        val upcoming = remember(uiState.weekEvents) {
            val now = System.currentTimeMillis()
            uiState.weekEvents.filter { it.endMillis > now }
        }

        if (upcoming.isEmpty()) {
            SectionCard(contentPadding = PaddingValues(vertical = 28.dp, horizontal = PanelSpacing.card)) {
                Text(
                    text = "Nothing coming up this week",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Tap + to add an event",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        } else {
            // Grouped by day-start key rather than a plain flat list, so a week of events reads
            // as 7 days, not one undifferentiated scroll. groupBy preserves encounter order, and
            // the repository already returns events sorted by BEGIN ASC, so keys come out
            // chronological for free.
            val shown = remember(upcoming) { upcoming.take(MaxUpcomingEvents) }
            val eventsByDay = remember(shown) {
                shown.groupBy { it.startMillis.startOfDay() }
            }
            val count = upcoming.size
            SectionLabel(if (count == 1) "1 upcoming event" else "$count upcoming events")
            LazyColumn(
                // weight, not fillMaxSize: as an unweighted child this would request the whole
                // panel height and overflow past the bottom, stranding the frequent-apps section
                // below it. Compose measures that fixed section first and hands this whatever
                // height is left, so it holds regardless of how many days' events are in here.
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                eventsByDay.forEach { (dayStart, dayEvents) ->
                    item(key = "day-$dayStart") {
                        DayHeader(dayStartMillis = dayStart, calendarSystem = calendarSystem)
                    }
                    items(dayEvents, key = { it.eventId }) { event ->
                        EventRow(
                            event = event,
                            expanded = expandedEventId == event.eventId,
                            timeFormat = timeFormat,
                            onToggleExpand = {
                                expandedEventId = if (expandedEventId == event.eventId) null else event.eventId
                            },
                        )
                    }
                }
            }
        }

        // Fixed, not scrollable — same reasoning as HomePanel's favorites: this list is a
        // handful of user-picked apps, not a feed, so it doesn't earn a LazyColumn.
        if (uiState.frequentApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PanelSpacing.section))
            SectionLabel("Frequent")
            Column {
                uiState.frequentApps.forEach { entry ->
                    Text(
                        text = entry.displayLabel(uiState.settings.appRenames),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunchApp(entry) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: CalendarEvent,
    expanded: Boolean,
    timeFormat: DateFormat,
    onToggleExpand: () -> Unit,
) {
    val palette = LocalLauncherPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // IntrinsicSize.Min so the row hugs its text and the accent rail can fillMaxHeight
            // against a resolved height instead of stretching to the whole panel.
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceVariant.copy(alpha = if (expanded) 0.85f else 0.6f))
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Accent rail: gives every row a fixed left edge to scan down, and grows with the row
        // when it expands so the marker never floats detached from the description.
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(palette.accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (event.isAllDay) "ALL DAY" else timeFormat.format(event.startMillis),
                color = palette.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = event.title,
                color = palette.textPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (expanded && !event.description.isNullOrBlank()) {
                Text(
                    text = event.description,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DayHeader(dayStartMillis: Long, calendarSystem: CalendarSystem) {
    val palette = LocalLauncherPalette.current
    Text(
        text = remember(dayStartMillis, calendarSystem) { dayHeaderLabel(dayStartMillis, calendarSystem) },
        color = palette.textSecondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

private const val OneDayMillis = 24 * 60 * 60 * 1000L

private fun dayHeaderLabel(dayStartMillis: Long, calendarSystem: CalendarSystem): String {
    val today = System.currentTimeMillis().startOfDay()
    val bs = calendarSystem == CalendarSystem.BS
    return when (dayStartMillis) {
        today -> if (bs) "आज" else "Today"
        today + OneDayMillis -> if (bs) "भोलि" else "Tomorrow"
        else -> dayLabel(dayStartMillis, calendarSystem)
    }
}

/**
 * Bikram Sambat label for BS, falling back to the Gregorian one outside the BS table's
 * range (BS 1975..2099). A wrong Nepali date is worse than an honest English one.
 */
private fun dayLabel(millis: Long, calendarSystem: CalendarSystem): String =
    (if (calendarSystem == CalendarSystem.BS) nepaliFullLabel(millis.toLocalDate()) else null)
        ?: SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(millis))

private fun Long.startOfDay(): Long = Calendar.getInstance().apply {
    timeInMillis = this@startOfDay
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
