package io.github.halcyonashes.minimala.ui.left

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halcyonashes.minimala.R
import io.github.halcyonashes.minimala.core.calendar.CalendarEvent
import io.github.halcyonashes.minimala.core.calendar.nepali.BikramSambat
import io.github.halcyonashes.minimala.core.calendar.nepali.BsDate
import io.github.halcyonashes.minimala.core.calendar.nepali.NepaliWeekdayInitials
import io.github.halcyonashes.minimala.core.calendar.nepali.nepaliFullLabel
import io.github.halcyonashes.minimala.core.calendar.nepali.nepaliMonthName
import io.github.halcyonashes.minimala.core.calendar.nepali.toDevanagariDigits
import io.github.halcyonashes.minimala.core.calendar.nepali.toLocalDate
import io.github.halcyonashes.minimala.core.calendar.nepali.toNepaliMonthLabel
import io.github.halcyonashes.minimala.core.settings.CalendarSystem
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.ui.LauncherUiState
import io.github.halcyonashes.minimala.ui.common.PanelSpacing
import io.github.halcyonashes.minimala.ui.common.SectionCard
import io.github.halcyonashes.minimala.ui.common.SectionLabel
import io.github.halcyonashes.minimala.ui.displayLabel
import io.github.halcyonashes.minimala.ui.startActivitySafely
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private const val DaysPerWeek = 7
private const val ShortcutColumns = 4

/** Day cell diameter. Big enough to read, small enough that six weeks stay compact. */
private val DayCellSize = 38.dp

@Composable
fun LeftPanel(
    uiState: LauncherUiState,
    onOpenSettings: () -> Unit,
    onSelectWeek: (weekStart: LocalDate) -> Unit,
    onClearWeek: () -> Unit,
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            // The whole panel scrolls rather than the week card alone: squeezed between the
            // calendar and Pinned, the card only got a sliver and cut events off mid-row.
            .verticalScroll(rememberScrollState())
            .padding(
                start = PanelSpacing.edge,
                end = PanelSpacing.edge,
                top = PanelSpacing.edgeTop,
                bottom = PanelSpacing.edge,
            ),
    ) {
        SectionLabel("Shortcuts")
        QuickSettingsColumn(
            onWallpaper = {
                context.startActivitySafely(
                    Intent(Intent.ACTION_SET_WALLPAPER),
                    "No wallpaper picker available",
                )
            },
            onAppSettings = onOpenSettings,
            onSystemSettings = {
                context.startActivitySafely(Intent(Settings.ACTION_SETTINGS), "Settings unavailable")
            },
        )

        // Reserved weather slot: LauncherSettings.weatherWidgetEnabled exists and is wired
        // through, so a widget composable drops in right here without reshuffling this column.

        // Gregorian month for AD. For BS, the Bikram Sambat month, falling back to the Gregorian
        // one outside the BS table's range (BS 1975..2099). A wrong Nepali month is worse than an
        // honest English one.
        val calendarSystem = uiState.settings.calendarSystem
        val todayBs = remember(calendarSystem) {
            if (calendarSystem == CalendarSystem.BS) BikramSambat.fromGregorian(LocalDate.now()) else null
        }
        val monthLabel = remember(todayBs) {
            todayBs?.toNepaliMonthLabel()
                ?: SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        }
        val onDayClick: (LocalDate) -> Unit = { selectedDate = it }
        // Keyed on the week start, not the tapped date: a second tap in the same row needs no
        // refetch, and flipping AD/BS (which moves the first weekday) does.
        val weekStart = selectedDate?.let { weekStartOf(it, sundayFirst = todayBs != null) }
        LaunchedEffect(weekStart) {
            if (weekStart != null) onSelectWeek(weekStart) else onClearWeek()
        }
        SectionLabel(monthLabel, modifier = Modifier.padding(top = PanelSpacing.section))
        SectionCard {
            if (todayBs != null) {
                NepaliMonthCalendar(
                    today = todayBs,
                    events = uiState.weekEvents,
                    selectedDate = selectedDate,
                    onDayClick = onDayClick,
                )
            } else {
                MonthCalendar(events = uiState.weekEvents, selectedDate = selectedDate, onDayClick = onDayClick)
            }
        }

        if (weekStart != null) {
            WeekEventsSection(
                weekStart = weekStart,
                events = uiState.selectedWeekEvents,
                calendarSystem = calendarSystem,
                onDismiss = { selectedDate = null },
                modifier = Modifier.padding(top = PanelSpacing.section),
            )
        } else {
            SectionLabel("Steps", modifier = Modifier.padding(top = PanelSpacing.section))
            StepsCard(steps = uiState.stepsToday)
        }

        val shortcuts = remember(uiState.settings.shortcutGridKeys, uiState.allApps) {
            val byKey = uiState.allApps.associateBy { it.key }
            uiState.settings.shortcutGridKeys.mapNotNull(byKey::get)
        }
        if (shortcuts.isNotEmpty()) {
            SectionLabel("Pinned", modifier = Modifier.padding(top = PanelSpacing.section))
            ShortcutGrid(labels = shortcuts.map { it.displayLabel(uiState.settings.appRenames) })
        }
    }
}

@Composable
private fun QuickSettingsColumn(
    onWallpaper: () -> Unit,
    onAppSettings: () -> Unit,
    onSystemSettings: () -> Unit,
) {
    val palette = LocalLauncherPalette.current
    val entries = listOf(
        "Wallpaper" to onWallpaper,
        "Minimala settings" to onAppSettings,
        "System settings" to onSystemSettings,
    )
    // Zero content padding: each row owns its own inset, so the whole row stays tappable out to
    // the card edge instead of leaving a dead gutter around the list.
    SectionCard(contentPadding = PaddingValues(0.dp)) {
        entries.forEachIndexed { index, (label, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = PanelSpacing.touchRow)
                    .clickable(onClick = action)
                    .padding(horizontal = PanelSpacing.card, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "›",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (index != entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = PanelSpacing.card)
                        .height(1.dp)
                        .background(palette.divider.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

/**
 * Hero-style stat card: a large accent-colored count carries the weight, so the eye lands on
 * the number instead of on the label. Reads 0 when the step sensor or its permission is
 * unavailable, same as an unread calendar.
 */
@Composable
private fun StepsCard(steps: Int, modifier: Modifier = Modifier) {
    val palette = LocalLauncherPalette.current
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "%,d".format(steps),
                color = palette.accent,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_walk),
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "steps",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * Bikram Sambat month grid. Same layout rules as [MonthCalendar], but the column a BS month
 * starts in has to be looked up rather than computed: BS month lengths vary year to year, so
 * the 1st is converted back to its Gregorian date and that date's weekday picks the column.
 *
 * Weeks run Sunday-first, the Nepali convention, not the locale's first day of week.
 */
@Composable
private fun NepaliMonthCalendar(
    today: BsDate,
    events: List<CalendarEvent>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current

    val daysInMonth = remember(today) { BikramSambat.daysInMonth(today.year, today.month) ?: 0 }
    val leadingBlanks = remember(today) {
        val first = BikramSambat.toGregorian(BsDate(today.year, today.month, 1))
        // DayOfWeek.SUNDAY.value is 7; % 7 folds it back to column 0.
        first?.dayOfWeek?.value?.rem(DaysPerWeek) ?: 0
    }
    val eventDays = remember(events, today) {
        events.mapNotNull { event ->
            BikramSambat.fromGregorian(event.startMillis.toLocalDate())
                ?.takeIf { it.year == today.year && it.month == today.month }
                ?.dayOfMonth
        }.toSet()
    }

    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()
    val paddedCells = cells + List((DaysPerWeek - cells.size % DaysPerWeek) % DaysPerWeek) { null }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            NepaliWeekdayInitials.forEach { label ->
                Text(
                    text = label,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp),
                )
            }
        }
        paddedCells.chunked(DaysPerWeek).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val date = day?.let { BikramSambat.toGregorian(BsDate(today.year, today.month, it)) }
                    DayCell(
                        label = day?.toString()?.toDevanagariDigits(),
                        isToday = day == today.dayOfMonth,
                        hasEvent = day != null && day in eventDays,
                        isSelected = date != null && date == selectedDate,
                        onClick = date?.let { { onDayClick(it) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Deliberately a plain Column of Rows rather than a LazyVerticalGrid: a month is at most six
 * rows, so laziness buys nothing, and a lazy grid is a scrollable container that would expand
 * to consume every remaining pixel of this non-scrolling column.
 */
@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    events: List<CalendarEvent>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
) {
    val palette = LocalLauncherPalette.current

    val today = remember { Calendar.getInstance() }
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = remember(today) {
        val first = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        (first.get(Calendar.DAY_OF_WEEK) - first.firstDayOfWeek + DaysPerWeek) % DaysPerWeek
    }
    val weekdayLabels = remember(today) {
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
        List(DaysPerWeek) {
            fmt.format(cal.time).take(1).also { cal.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }
    val eventDays = remember(events) {
        val cal = Calendar.getInstance()
        val thisMonth = today.get(Calendar.MONTH)
        val thisYear = today.get(Calendar.YEAR)
        events.mapNotNull { event ->
            cal.timeInMillis = event.startMillis
            if (cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear) {
                cal.get(Calendar.DAY_OF_MONTH)
            } else {
                null
            }
        }.toSet()
    }

    // Pad to whole weeks so every row has exactly seven cells and the grid stays aligned.
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()
    val paddedCells = cells + List((DaysPerWeek - cells.size % DaysPerWeek) % DaysPerWeek) { null }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp),
                )
            }
        }
        paddedCells.chunked(DaysPerWeek).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    // Calendar.MONTH is 0-based; LocalDate's month is 1-based.
                    val date = day?.let {
                        LocalDate.of(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, it)
                    }
                    DayCell(
                        label = day?.toString(),
                        isToday = day == todayDay,
                        hasEvent = day != null && day in eventDays,
                        isSelected = date != null && date == selectedDate,
                        onClick = date?.let { { onDayClick(it) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    label: String?,
    isToday: Boolean,
    hasEvent: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    Box(
        modifier = modifier.padding(vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label == null) {
            Text(text = " ", style = MaterialTheme.typography.labelMedium)
            return@Box
        }
        // Fixed square rather than aspectRatio(1f): a 1:1 cell at one seventh of the panel
        // width makes every week ~100dp tall and the month eats half the screen.
        Box(
            modifier = Modifier
                .size(DayCellSize)
                .clip(CircleShape)
                .background(if (isToday) palette.accent else Color.Transparent)
                // Today is already a filled accent disc, so a ring on it would be invisible.
                .then(if (isSelected && !isToday) Modifier.border(1.dp, palette.accent, CircleShape) else Modifier)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = if (isToday) palette.surface else palette.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Medium else null,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !hasEvent -> Color.Transparent
                                isToday -> palette.surface
                                else -> palette.accent
                            },
                        ),
                )
            }
        }
    }
}

/**
 * First day of the grid row [date] sits in, so the listed week is exactly the row tapped. BS
 * rows are Sunday-first; AD rows follow the locale's first weekday, same as [MonthCalendar].
 */
private fun weekStartOf(date: LocalDate, sundayFirst: Boolean): LocalDate {
    // java.util.Calendar numbering, SUNDAY = 1. DayOfWeek.SUNDAY.value is 7, so % 7 folds it to 0.
    val calendarDow = date.dayOfWeek.value % DaysPerWeek + 1
    val firstDow = if (sundayFirst) Calendar.SUNDAY else Calendar.getInstance().firstDayOfWeek
    return date.minusDays(((calendarDow - firstDow + DaysPerWeek) % DaysPerWeek).toLong())
}

@Composable
private fun WeekEventsSection(
    weekStart: LocalDate,
    events: List<CalendarEvent>,
    calendarSystem: CalendarSystem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    val context = LocalContext.current
    val timeFormat = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    val bs = calendarSystem == CalendarSystem.BS
    // The Instances query returns anything overlapping the week, so an event that began before
    // it is filed under the week's first day rather than under a header outside the week.
    val eventsByDay = remember(events, weekStart) {
        events.groupBy { event ->
            event.startMillis.toLocalDate().let { if (it.isBefore(weekStart)) weekStart else it }
        }
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                text = remember(weekStart, bs) { weekLabel(weekStart, bs) },
                modifier = Modifier.weight(1f),
            )
            // Same bottom inset as SectionLabel so the two centre on one line.
            Box(
                modifier = Modifier
                    .padding(bottom = PanelSpacing.label)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "×", color = palette.accent, style = MaterialTheme.typography.titleMedium)
            }
        }
        SectionCard {
            if (events.isEmpty()) {
                Text(
                    text = "No events this week",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                return@SectionCard
            }
            // No scroll of its own: a same-direction scroll nested in the panel's would be
            // measured with infinite height and throw.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eventsByDay.forEach { (day, dayEvents) ->
                    Text(
                        text = dayLabel(day, bs),
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    dayEvents.forEach { event -> WeekEventRow(event = event, timeFormat = timeFormat) }
                }
            }
        }
    }
}

/** RightPanel's EventRow minus the expand and the row surface — it already sits on a card. */
@Composable
private fun WeekEventRow(event: CalendarEvent, timeFormat: DateFormat) {
    val palette = LocalLauncherPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Nepali outside the BS table's range falls back to Gregorian, as everywhere else. */
private fun weekLabel(weekStart: LocalDate, bs: Boolean): String =
    (if (bs) BikramSambat.fromGregorian(weekStart) else null)
        ?.let { "${nepaliMonthName(it.month)} ${it.dayOfMonth.toString().toDevanagariDigits()} को हप्ता" }
        ?: "Week of ${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}"

private fun dayLabel(day: LocalDate, bs: Boolean): String =
    (if (bs) nepaliFullLabel(day) else null)
        ?: day.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))

@Composable
private fun ShortcutGrid(modifier: Modifier = Modifier, labels: List<String>) {
    val palette = LocalLauncherPalette.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.chunked(ShortcutColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { label ->
                    Text(
                        text = label,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                }
                // Keep the last row's columns the same width as full rows.
                repeat(ShortcutColumns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
