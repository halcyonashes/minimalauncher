package io.github.halcyonashes.minimala

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.halcyonashes.minimala.theme.MinimalaTheme
import io.github.halcyonashes.minimala.theme.paletteById
import io.github.halcyonashes.minimala.ui.CreateEventResult
import io.github.halcyonashes.minimala.ui.LauncherViewModel
import io.github.halcyonashes.minimala.ui.settings.SettingsScreen
import io.github.halcyonashes.minimala.ui.shell.LauncherShell

class MainActivity : ComponentActivity() {

    private var homeResetSignal by mutableIntStateOf(0)

    /**
     * One launch for every permission: Android drops a second request made while the first
     * dialog is still pending, so separate back-to-back launches lost the step prompt.
     */
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // The grants themselves are not inspected: the ON_RESUME effect below re-reads the
        // events either way. The repositories are told so their long-lived flows (sensor,
        // calendar observer), which bailed out without permission, re-check and start.
        val app = application as MinimalaApp
        app.calendarRepository.onPermissionResult()
        app.stepCounterRepository.onPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MinimalaApp

        // Final fallback only. Surface-specific BackHandlers inside the composition are
        // registered later and therefore run first, so this consumes back solely on the
        // home panel — where a launcher must never finish() itself into a black screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val missingPermissions = buildList {
            if (!hasCalendarPermission()) {
                add(Manifest.permission.READ_CALENDAR)
                add(Manifest.permission.WRITE_CALENDAR)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !app.stepCounterRepository.hasPermission()) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissions.launch(missingPermissions.toTypedArray())
        }

        setContent {
            val viewModel: LauncherViewModel = viewModel(
                factory = LauncherViewModel.Factory(
                    app.settingsRepository,
                    app.appRepository,
                    app.calendarRepository,
                    app.stepCounterRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showSettings by remember { mutableStateOf(false) }

            // The launcher process outlives any single permission decision: the user can grant
            // or revoke calendar access from system Settings and come straight back via HOME.
            // Re-reading on every resume is what keeps this from showing stale events forever.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWeekEvents()
            }

            // HOME returns to the home panel from anywhere, settings included.
            LaunchedEffect(homeResetSignal) {
                if (homeResetSignal > 0) showSettings = false
            }

            // Light palettes need dark system-bar icons, or they vanish against the scrim.
            val isDarkPalette = paletteById(uiState.settings.paletteId).isDark
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !isDarkPalette
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightNavigationBars = !isDarkPalette
            }

            MinimalaTheme(
                paletteId = uiState.settings.paletteId,
                fontId = uiState.settings.fontId,
                fontScale = uiState.settings.fontScale,
                fontWeight = uiState.settings.fontWeight,
                clockScale = uiState.settings.clockScale,
            ) {
                // Transparent: the window sets windowShowWallpaper, and an opaque root would
                // paint over the wallpaper before any panel got the chance to scrim it.
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    if (showSettings) {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            settings = uiState.settings,
                            allApps = uiState.allApps,
                            onBack = { showSettings = false },
                            onSelectPalette = viewModel::setPalette,
                            onSelectFont = viewModel::setFont,
                            onFontScaleChange = viewModel::setFontScale,
                            onFontWeightChange = viewModel::setFontWeight,
                            onClockScaleChange = viewModel::setClockScale,
                            onClockStyleChange = viewModel::setClockStyle,
                            onDateStyleChange = viewModel::setDateStyle,
                            onCalendarSystemChange = viewModel::setCalendarSystem,
                            onBackgroundOpacityChange = viewModel::setBackgroundOpacity,
                            onDayWindowChange = viewModel::setDayWindow,
                            onToggleHidden = viewModel::toggleHidden,
                        )
                    } else {
                        LauncherShell(
                            uiState = uiState,
                            resetSignal = homeResetSignal,
                            onLaunchApp = viewModel::launchApp,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onToggleFrequent = viewModel::toggleFrequent,
                            onToggleHidden = viewModel::toggleHidden,
                            onRenameApp = viewModel::renameApp,
                            onOpenAppInfo = viewModel::openAppInfo,
                            onUninstallApp = viewModel::uninstallApp,
                            onOpenSettings = { showSettings = true },
                            onCreateEvent = { title ->
                                // The application context, not the activity: the callback runs
                                // in viewModelScope and can outlive a recreated activity.
                                viewModel.createTodayEvent(title) { result ->
                                    Toast.makeText(app, createEventMessage(result), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onScratchPadChange = viewModel::setScratchPad,
                            onReorderFrequent = viewModel::reorderFrequent,
                            onSelectWeek = viewModel::loadWeekEvents,
                            onClearWeek = viewModel::clearSelectedWeek,
                            onSelectMonth = viewModel::loadMonthEvents,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeResetSignal++
        }
    }

    private fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}

private fun createEventMessage(result: CreateEventResult): String = when (result) {
    CreateEventResult.Added -> "Event added"
    CreateEventResult.NoPermission -> "Calendar permission needed"
    CreateEventResult.NoWritableCalendar -> "No writable calendar"
    CreateEventResult.Failed -> "Could not add event"
}
