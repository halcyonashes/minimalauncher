package io.github.halcyonashes.minimala.ui.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.system.turnScreenOff
import io.github.halcyonashes.minimala.theme.LocalClockTextStyle
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.ui.LauncherUiState
import io.github.halcyonashes.minimala.ui.displayLabel
import io.github.halcyonashes.minimala.ui.startActivitySafely
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun HomePanel(
    uiState: LauncherUiState,
    onLaunchApp: (AppEntry) -> Unit,
) {
    val palette = LocalLauncherPalette.current
    val context = LocalContext.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPercent by remember { mutableIntStateOf(readBatteryPercent(context)) }

    // Re-align to each minute boundary rather than ticking on a fixed interval, so the
    // displayed minute never lags real time by most of an interval.
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            batteryPercent = readBatteryPercent(context)
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            // Double tap anywhere on the empty background turns the screen off. Nested
            // clickables (clock, date, favorites) consume their own taps first, so this
            // never fires on top of them.
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { turnScreenOff() })
            }
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.padding(top = 32.dp)) {
            // Long styles (worded, roman) shrink to fit one line instead of wrapping. The
            // configured clock size stays the ceiling; lineHeight is em so it shrinks too.
            val clockStyle = LocalClockTextStyle.current
            BasicText(
                text = formatClockText(nowMillis, uiState.settings.clockStyle, context),
                style = clockStyle.copy(color = palette.textPrimary, lineHeight = CLOCK_LINE_HEIGHT),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = CLOCK_MIN_FONT_SIZE,
                    maxFontSize = clockStyle.fontSize,
                ),
                modifier = Modifier.clickable {
                    context.startActivitySafely(
                        Intent(AlarmClock.ACTION_SHOW_ALARMS),
                        "No clock app available",
                    )
                },
            )
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        context.startActivitySafely(
                            Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_CALENDAR)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                            "No calendar app available",
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDateText(nowMillis, uiState.settings.dateStyle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (batteryPercent >= 0) {
                    Text(
                        text = "  ·  $batteryPercent%",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            DayProgressBar(
                dayStartMinute = uiState.settings.dayStartMinute,
                dayEndMinute = uiState.settings.dayEndMinute,
                nowMillis = nowMillis,
                accent = palette.accent,
                track = palette.surfaceVariant,
            )
        }

        // weight(1f) + centered content, not a fixed bottom block: this is what puts the
        // favorites list in the middle of the remaining space below the clock instead of
        // pinned to the screen edge.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                val favoriteStyle = MaterialTheme.typography.titleMedium.let {
                    it.copy(fontSize = it.fontSize * 1.15f, lineHeight = it.lineHeight * 1.15f)
                }
                uiState.favorites.forEach { entry ->
                    Text(
                        text = entry.displayLabel(uiState.settings.appRenames),
                        color = palette.textPrimary,
                        style = favoriteStyle,
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

private val CLOCK_MIN_FONT_SIZE = 20.sp

// Same 80/72 ratio as clockTextStyle, expressed relative to the (possibly shrunk) font size.
private val CLOCK_LINE_HEIGHT = (80f / 72f).em

private fun readBatteryPercent(context: Context): Int {
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
    val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) level * 100 / scale else -1
}

@Composable
private fun DayProgressBar(
    dayStartMinute: Int,
    dayEndMinute: Int,
    nowMillis: Long,
    accent: Color,
    track: Color,
) {
    val progress = remember(dayStartMinute, dayEndMinute, nowMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val span = (dayEndMinute - dayStartMinute).coerceAtLeast(1)
        ((nowMinute - dayStartMinute).toFloat() / span).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
    }
}
