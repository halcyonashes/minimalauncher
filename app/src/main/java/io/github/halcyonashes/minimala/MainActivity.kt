package io.github.halcyonashes.minimala

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import io.github.halcyonashes.minimala.ui.LauncherViewModel
import io.github.halcyonashes.minimala.ui.settings.SettingsScreen
import io.github.halcyonashes.minimala.ui.shell.LauncherShell

class MainActivity : ComponentActivity() {

    private var homeResetSignal by mutableIntStateOf(0)

    private val requestCalendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Result is intentionally ignored: the ON_RESUME effect below re-reads the events
        // either way, and the repository yields an empty list when permission is absent.
    }

    private val requestActivityRecognitionPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Result is intentionally ignored: StepCounterRepository re-checks the permission on
        // every sensor registration, so a grant/deny is picked up the next time the flow
        // collects rather than needing an explicit refresh here.
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

        if (!hasCalendarPermission()) {
            requestCalendarPermission.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !app.stepCounterRepository.hasPermission()) {
            requestActivityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
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
                            onOpenSettings = { showSettings = true },
                            onCreateEvent = viewModel::createTodayEvent,
                            onScratchPadChange = viewModel::setScratchPad,
                            onSelectWeek = viewModel::loadWeekEvents,
                            onClearWeek = viewModel::clearSelectedWeek,
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
