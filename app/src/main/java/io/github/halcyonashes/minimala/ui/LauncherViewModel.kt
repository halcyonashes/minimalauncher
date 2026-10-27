package io.github.halcyonashes.minimala.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.apps.AppRepository
import io.github.halcyonashes.minimala.core.calendar.CalendarEvent
import io.github.halcyonashes.minimala.core.calendar.CalendarRepository
import io.github.halcyonashes.minimala.core.pedometer.StepCounterRepository
import io.github.halcyonashes.minimala.core.settings.CalendarSystem
import io.github.halcyonashes.minimala.core.settings.ClockStyle
import io.github.halcyonashes.minimala.core.settings.DateStyle
import io.github.halcyonashes.minimala.core.settings.FontWeightOption
import io.github.halcyonashes.minimala.core.settings.LauncherSettings
import io.github.halcyonashes.minimala.core.settings.SettingsRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MinutesPerDay = 24 * 60
private const val MinDayWindowMinutes = 60
private const val DaysInWeek = 7

/**
 * [visibleApps] and [favorites] are resolved eagerly when the state is built, not as `get()`
 * properties — favorites is an O(favorites × apps) lookup, and a computed property would
 * re-run it on every recomposition, including every frame of a palette cross-fade.
 */
data class LauncherUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val allApps: List<AppEntry> = emptyList(),
    val visibleApps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val frequentApps: List<AppEntry> = emptyList(),
    val weekEvents: List<CalendarEvent> = emptyList(),
    /** Events for the week the user tapped on the left panel's month grid. */
    val selectedWeekEvents: List<CalendarEvent> = emptyList(),
    val stepsToday: Int = 0,
)

/** Resolves the user's per-app rename, falling back to the system label. */
fun AppEntry.displayLabel(renames: Map<String, String>): String = renames[key] ?: label

class LauncherViewModel(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    private val calendarRepository: CalendarRepository,
    private val stepCounterRepository: StepCounterRepository,
) : ViewModel() {

    private val _weekEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _selectedWeekEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var selectedWeekJob: Job? = null

    val uiState: StateFlow<LauncherUiState> = combine(
        settingsRepository.settings,
        appRepository.apps,
        _weekEvents,
        _selectedWeekEvents,
        stepCounterRepository.stepsToday(),
    ) { settings, apps, events, selectedWeekEvents, steps ->
        val byKey = apps.associateBy { it.key }
        LauncherUiState(
            settings = settings,
            allApps = apps,
            visibleApps = apps.filter { it.key !in settings.hiddenAppKeys },
            favorites = settings.favoritePackageKeys.mapNotNull(byKey::get),
            frequentApps = settings.frequentAppKeys.mapNotNull(byKey::get),
            weekEvents = events,
            selectedWeekEvents = selectedWeekEvents,
            stepsToday = steps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    /** Safe to call unconditionally — the repository yields an empty list without permission. */
    fun refreshWeekEvents() {
        viewModelScope.launch {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + DaysInWeek * MinutesPerDay * 60 * 1000L
            _weekEvents.value = calendarRepository.eventsBetween(start, end)
        }
    }

    /**
     * [weekStart] is chosen by the caller because only the grid knows its own first weekday.
     * The previous fetch is cancelled so a slow query for an older tap can't land last and
     * overwrite the week the user is actually looking at.
     */
    fun loadWeekEvents(weekStart: LocalDate) {
        selectedWeekJob?.cancel()
        selectedWeekJob = viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            // Day arithmetic in LocalDate, not +7×24h millis: a DST week is not 168 hours.
            val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = weekStart.plusDays(DaysInWeek.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            _selectedWeekEvents.value = calendarRepository.eventsBetween(start, end)
        }
    }

    fun clearSelectedWeek() {
        selectedWeekJob?.cancel()
        _selectedWeekEvents.value = emptyList()
    }

    fun launchApp(entry: AppEntry) = appRepository.launch(entry)

    fun openAppInfo(entry: AppEntry) = appRepository.openAppInfo(entry)

    fun createTodayEvent(title: String) {
        viewModelScope.launch {
            val calendarId = calendarRepository.defaultWritableCalendarId() ?: return@launch
            val start = Calendar.getInstance().apply {
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            calendarRepository.insertEvent(calendarId, title, null, start, start + 60 * 60 * 1000L)
            refreshWeekEvents()
        }
    }

    fun toggleFavorite(entry: AppEntry) = updateSettings { settings ->
        val current = settings.favoritePackageKeys
        val next = if (entry.key in current) current - entry.key else current + entry.key
        settings.copy(favoritePackageKeys = next)
    }

    fun toggleFrequent(entry: AppEntry) = updateSettings { settings ->
        val current = settings.frequentAppKeys
        val next = if (entry.key in current) current - entry.key else current + entry.key
        settings.copy(frequentAppKeys = next)
    }

    fun toggleHidden(entry: AppEntry) = updateSettings { settings ->
        val current = settings.hiddenAppKeys
        val next = if (entry.key in current) current - entry.key else current + entry.key
        settings.copy(hiddenAppKeys = next)
    }

    fun renameApp(entry: AppEntry, newLabel: String) = updateSettings { settings ->
        val trimmed = newLabel.trim()
        val next = if (trimmed.isEmpty() || trimmed == entry.label) {
            settings.appRenames - entry.key
        } else {
            settings.appRenames + (entry.key to trimmed)
        }
        settings.copy(appRenames = next)
    }

    fun setPalette(paletteId: String) = updateSettings { it.copy(paletteId = paletteId) }
    fun setFont(fontId: String) = updateSettings { it.copy(fontId = fontId) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(fontScale = scale) }
    fun setFontWeight(weight: FontWeightOption) = updateSettings { it.copy(fontWeight = weight) }
    fun setClockScale(scale: Float) = updateSettings { it.copy(clockScale = scale) }
    fun setClockStyle(style: ClockStyle) = updateSettings { it.copy(clockStyle = style) }
    fun setDateStyle(style: DateStyle) = updateSettings { it.copy(dateStyle = style) }
    fun setCalendarSystem(system: CalendarSystem) = updateSettings { it.copy(calendarSystem = system) }
    fun setScratchPad(text: String) = updateSettings { it.copy(scratchPad = text) }

    /** 0 = wallpaper fully visible, 1 = wallpaper fully covered by the palette background. */
    fun setBackgroundOpacity(opacity: Float) =
        updateSettings { it.copy(backgroundOpacity = opacity.coerceIn(0f, 1f)) }

    /**
     * Start and end come from two independent sliders, so they can cross. Clamping here keeps
     * the day-progress bar's span positive instead of letting the UI hand it a negative window.
     */
    fun setDayWindow(startMinute: Int, endMinute: Int) = updateSettings { settings ->
        val start = startMinute.coerceIn(0, MinutesPerDay - MinDayWindowMinutes)
        val end = endMinute.coerceIn(start + MinDayWindowMinutes, MinutesPerDay)
        settings.copy(dayStartMinute = start, dayEndMinute = end)
    }

    private fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val appRepository: AppRepository,
        private val calendarRepository: CalendarRepository,
        private val stepCounterRepository: StepCounterRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LauncherViewModel(settingsRepository, appRepository, calendarRepository, stepCounterRepository) as T
    }
}
