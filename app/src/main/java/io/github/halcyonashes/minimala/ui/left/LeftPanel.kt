package io.github.halcyonashes.minimala.ui.left

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
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
import io.github.halcyonashes.minimala.ui.common.CompactTouchTarget
import io.github.halcyonashes.minimala.ui.common.PanelSpacing
import io.github.halcyonashes.minimala.ui.common.SectionCard
import io.github.halcyonashes.minimala.ui.common.SectionLabel
import io.github.halcyonashes.minimala.ui.common.rememberToday
import io.github.halcyonashes.minimala.ui.displayLabel
import io.github.halcyonashes.minimala.ui.startActivitySafely
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

private const val DaysPerWeek = 7
private const val ShortcutColumns = 4
private const val MonthsPerYear = 12

/** Day cell diameter. Big enough to read, small enough that six weeks stay compact. */
private val DayCellSize = 38.dp

@Composable
fun LeftPanel(
    uiState: LauncherUiState,
    onOpenSettings: () -> Unit,
    onSelectWeek: (weekStart: LocalDate) -> Unit,
    onClearWeek: () -> Unit,
    onSelectMonth: (start: LocalDate, endExclusive: LocalDate) -> Unit,
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
        // Rolls over at midnight (and on clock/zone changes) while this page stays on screen.
        val today = rememberToday()
        val todayBs = remember(calendarSystem, today) {
            if (calendarSystem == CalendarSystem.BS) BikramSambat.fromGregorian(today) else null
        }
        // Months away from the current one. Reset when AD/BS flips: the two calendars' months
        // do not line up, so the same offset would land on a different stretch of days.
        var monthOffset by remember(calendarSystem) { mutableStateOf(0) }
        val adMonth = remember(today, monthOffset) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }
        // Non-null whenever todayBs is: the arrows below refuse to step outside the BS table.
        val bsMonth = remember(todayBs, monthOffset) { todayBs?.monthStart(monthOffset) }
        val monthLabel = remember(bsMonth, adMonth) {
            bsMonth?.toNepaliMonthLabel()
                ?: adMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        }
        // The shown month as a Gregorian day range, so the event dots can be fetched for it.
        val monthStart = remember(bsMonth, adMonth) { bsMonth?.let(BikramSambat::toGregorian) ?: adMonth.atDay(1) }
        val monthDays = remember(bsMonth, adMonth) {
            bsMonth?.let { BikramSambat.daysInMonth(it.year, it.month) } ?: adMonth.lengthOfMonth()
        }
        LaunchedEffect(monthStart, monthDays) {
            onSelectMonth(monthStart, monthStart.plusDays(monthDays.toLong()))
        }
        val eventDates = remember(uiState.monthEvents) {
            uiState.monthEvents.map { it.startMillis.toLocalDate() }.toSet()
        }
        val canGoBack = todayBs == null || todayBs.monthStart(monthOffset - 1) != null
        val canGoForward = todayBs == null || todayBs.monthStart(monthOffset + 1) != null
        val onDayClick: (LocalDate) -> Unit = { selectedDate = it }
        // Keyed on the week start, not the tapped date: a second tap in the same row needs no
        // refetch, and flipping AD/BS (which moves the first weekday) does.
        val weekStart = selectedDate?.let { weekStartOf(it, sundayFirst = todayBs != null) }
        LaunchedEffect(weekStart) {
            if (weekStart != null) onSelectWeek(weekStart) else onClearWeek()
        }
        MonthHeader(
            label = monthLabel,
            onPrevious = if (canGoBack) ({ monthOffset -= 1 }) else null,
            onNext = if (canGoForward) ({ monthOffset += 1 }) else null,
            onReset = if (monthOffset != 0) ({ monthOffset = 0 }) else null,
            modifier = Modifier.padding(top = PanelSpacing.section),
        )
        SectionCard {
            if (bsMonth != null) {
                NepaliMonthCalendar(
                    month = bsMonth,
                    today = today,
                    eventDates = eventDates,
                    selectedDate = selectedDate,
                    onDayClick = onDayClick,
                )
            } else {
                MonthCalendar(
                    month = adMonth,
                    today = today,
                    eventDates = eventDates,
                    selectedDate = selectedDate,
                    onDayClick = onDayClick,
                )
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
 * First day of the BS month [offset] months away from this date's, or null once that leaves
 * the BS lookup table.
 */
private fun BsDate.monthStart(offset: Int): BsDate? {
    val index = year * MonthsPerYear + (month - 1) + offset
    return BsDate(index.floorDiv(MonthsPerYear), index.mod(MonthsPerYear) + 1, 1)
        .takeIf { BikramSambat.daysInMonth(it.year, it.month) != null }
}

/**
 * Month title with previous/next arrows. A null callback disables its control; [onReset] is
 * only set away from the current month, where tapping the title jumps back to it.
 */
@Composable
private fun MonthHeader(
    label: String,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(
            text = label,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onReset != null) {
                        Modifier.clickable(onClickLabel = "Back to current month", onClick = onReset)
                    } else {
                        Modifier
                    },
                ),
        )
        MonthArrow(glyph = "‹", description = "Previous month", onClick = onPrevious)
        MonthArrow(
            glyph = "›",
            description = "Next month",
            onClick = onNext,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Same disc as the week list's dismiss button, dimmed when there is no month to step to. */
@Composable
private fun MonthArrow(
    glyph: String,
    description: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    val enabled = onClick != null
    CompactTouchTarget(
        onClick = onClick,
        description = description,
        // Same bottom inset as SectionLabel so the two centre on one line.
        modifier = modifier.padding(bottom = PanelSpacing.label),
    ) { visual ->
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = if (enabled) 0.16f else 0.06f))
                .then(visual),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                color = palette.accent.copy(alpha = if (enabled) 1f else 0.3f),
                style = MaterialTheme.typography.titleMedium,
            )
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
    month: BsDate,
    today: LocalDate,
    eventDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current

    val daysInMonth = remember(month) { BikramSambat.daysInMonth(month.year, month.month) ?: 0 }
    val first = remember(month) { BikramSambat.toGregorian(BsDate(month.year, month.month, 1)) }
    // DayOfWeek.SUNDAY.value is 7; % 7 folds it back to column 0.
    val leadingBlanks = first?.dayOfWeek?.value?.rem(DaysPerWeek) ?: 0

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
                    val date = day?.let { first?.plusDays(it - 1L) }
                    DayCell(
                        label = day?.toString()?.toDevanagariDigits(),
                        isToday = date != null && date == today,
                        hasEvent = date != null && date in eventDates,
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
    month: YearMonth,
    today: LocalDate,
    eventDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
) {
    val palette = LocalLauncherPalette.current

    val daysInMonth = month.lengthOfMonth()
    // Same first weekday weekStartOf uses, so a tapped row and its listed week agree.
    val leadingBlanks = remember(month) {
        val first = month.atDay(1)
        (first.toEpochDay() - weekStartOf(first, sundayFirst = false).toEpochDay()).toInt()
    }
    // NARROW, not the first letter of the short name: in Chinese every short name starts with
    // 周, so all seven headers read the same. Calendar's SUNDAY is 1, hence the minus one.
    val weekdayLabels = remember {
        val first = DayOfWeek.SUNDAY.plus(Calendar.getInstance().firstDayOfWeek - 1L)
        List(DaysPerWeek) {
            first.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
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
                    val date = day?.let(month::atDay)
                    DayCell(
                        label = day?.toString(),
                        isToday = date != null && date == today,
                        hasEvent = date != null && date in eventDates,
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        // The whole weighted cell takes the tap, not just the disc, and is at least a full touch
        // target tall (5dp above and below the disc, was 3dp). The ripple stays on the disc.
        modifier = modifier
            .heightIn(min = PanelSpacing.touchTarget)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            ),
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
                .indication(interactionSource, LocalIndication.current),
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
            CompactTouchTarget(
                onClick = onDismiss,
                description = "Close week",
                // Same bottom inset as SectionLabel so the two centre on one line.
                modifier = Modifier.padding(bottom = PanelSpacing.label),
            ) { visual ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.16f))
                        .then(visual),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "×", color = palette.accent, style = MaterialTheme.typography.titleMedium)
                }
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
