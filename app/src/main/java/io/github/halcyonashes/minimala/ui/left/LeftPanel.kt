package io.github.halcyonashes.minimala.ui.left

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

private const val DaysPerWeek = 7
private const val ShortcutColumns = 4

/** Day cell diameter. Big enough to read, small enough that six weeks stay compact. */
private val DayCellSize = 38.dp

@Composable
fun LeftPanel(uiState: LauncherUiState, onOpenSettings: () -> Unit) {
    val context = LocalContext.current

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
        SectionLabel(monthLabel, modifier = Modifier.padding(top = PanelSpacing.section))
        SectionCard {
            if (todayBs != null) {
                NepaliMonthCalendar(today = todayBs, events = uiState.weekEvents)
            } else {
                MonthCalendar(events = uiState.weekEvents)
            }
        }

        SectionLabel("Steps", modifier = Modifier.padding(top = PanelSpacing.section))
        StepsCard(steps = uiState.stepsToday)

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
                    DayCell(
                        label = day?.toString()?.toDevanagariDigits(),
                        isToday = day == today.dayOfMonth,
                        hasEvent = day != null && day in eventDays,
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
private fun MonthCalendar(modifier: Modifier = Modifier, events: List<CalendarEvent>) {
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
                    DayCell(
                        label = day?.toString(),
                        isToday = day == todayDay,
                        hasEvent = day != null && day in eventDays,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(label: String?, isToday: Boolean, hasEvent: Boolean, modifier: Modifier = Modifier) {
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
                .background(if (isToday) palette.accent else Color.Transparent),
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
