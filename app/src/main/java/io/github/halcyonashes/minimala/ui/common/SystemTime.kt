package io.github.halcyonashes.minimala.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate

/**
 * What can move the minute on the clock. TIME_TICK lands on each minute boundary but only while
 * the screen is on, which is the only time the clock can be seen; a delay() loop instead stalls in
 * deep sleep. TIME_CHANGED also covers a 12/24-hour switch, which Settings announces with it.
 */
private val ClockActions = listOf(
    Intent.ACTION_TIME_TICK,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

/** What can move "today": midnight, plus manual clock and zone changes, which skip DATE_CHANGED. */
private val DateActions = listOf(
    Intent.ACTION_DATE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

/** Wall-clock time, re-read on every minute tick, clock or zone change, and resume. */
@Composable
fun rememberCurrentTimeMillis(): Long = rememberRefreshedOn(ClockActions) { System.currentTimeMillis() }

/** Today's date, rolling over at midnight even while the panel reading it stays on screen. */
@Composable
fun rememberToday(): LocalDate = rememberRefreshedOn(DateActions) { LocalDate.now() }

/**
 * [read] again whenever one of [actions] is broadcast and on every resume, since nothing is
 * delivered while this is out of composition or the activity is stopped. Structural equality on
 * the state means an unchanged date does not recompose the reader.
 */
@Composable
private fun <T> rememberRefreshedOn(actions: List<String>, read: () -> T): T {
    val context = LocalContext.current
    var value by remember { mutableStateOf(read()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { value = read() }
    DisposableEffect(context, actions) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = read()
            }
        }
        val filter = IntentFilter().apply { actions.forEach(::addAction) }
        // All of these are protected system broadcasts, which a not-exported receiver still gets.
        // The explicit flag is what targetSdk 34+ requires once any non-system action is added,
        // and ContextCompat maps it onto the pre-33 API.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Catch up on anything broadcast while unregistered, e.g. while this page was swiped away.
        value = read()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return value
}
