package io.github.halcyonashes.minimala.core.pedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

private const val PREFS_NAME = "minimala_pedometer"
private const val KEY_LAST_DAY = "last_reading_epoch_day"
private const val KEY_LAST_READING = "last_reading_steps"
private const val KEY_LAST_STEPS_TODAY = "last_reading_steps_today"
private const val KEY_LAST_BOOT_TIME = "last_reading_boot_time"

/**
 * Boot time derived from wall clock minus uptime drifts with NTP corrections, so it is
 * compared with slack. Two real boots are always further apart than this: the gap includes
 * the whole previous uptime plus the shutdown and boot themselves.
 */
private const val BOOT_TIME_TOLERANCE_MILLIS = 30_000L

/**
 * Wraps [Sensor.TYPE_STEP_COUNTER], which reports a running total since the last reboot rather
 * than a daily count. Each reading is persisted with its day, its boot and today's total at that
 * moment, so "steps today" is the stored total plus the sensor delta since — which survives a
 * reboot (the delta restarts from zero) instead of losing the day's earlier steps.
 */
class StepCounterRepository(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Bumped by [onPermissionResult]; [stepsToday] restarts on it so a grant registers the sensor. */
    private val permissionEpoch = MutableStateFlow(0)

    fun hasSensor(): Boolean = stepCounterSensor != null

    /** Below API 29 the sensor was ungated, so there is nothing to check. */
    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

    /** Call when the ACTIVITY_RECOGNITION request returns, granted or not. */
    fun onPermissionResult() {
        permissionEpoch.update { it + 1 }
    }

    /**
     * Emits the step count since local midnight, starting immediately with the last persisted
     * count for today (or `0`).
     *
     * TYPE_STEP_COUNTER is on-change: some OEM HALs only redeliver the last value once a step
     * is actually taken after registration, not on registration itself. Without the eager value
     * this flow could stay silent indefinitely on a stationary device, and since it's one leg
     * of a `combine()` upstream in the ViewModel, that silence blocks every other emission too
     * (settings, app list) — not just the step count. The persisted value, rather than `0`, is
     * what keeps the count from flashing to zero on every return home.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stepsToday(): Flow<Int> = permissionEpoch.flatMapLatest { sensorSteps() }

    private fun sensorSteps(): Flow<Int> = callbackFlow {
        trySend(persistedStepsToday())

        val sensor = stepCounterSensor
        if (sensor == null || !hasPermission()) {
            awaitClose {}
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(recordReading(event.values[0].toInt()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun persistedStepsToday(): Int =
        if (prefs.getLong(KEY_LAST_DAY, -1) == LocalDate.now().toEpochDay()) {
            prefs.getInt(KEY_LAST_STEPS_TODAY, 0)
        } else {
            0
        }

    /**
     * Derives today's steps from the previous persisted reading, then persists this one.
     *
     * A reboot is detected by the boot time moving (or the counter going backwards): the
     * counter restarted from zero at boot, so everything it reports was walked after the
     * previous reading and is added on top of the stored total.
     *
     * Day rollover: the counter's value at local midnight is unknown without background
     * sampling, so the last reading of yesterday (if from the same boot) stands in for it.
     * Trade-off: steps taken between that reading and midnight are counted as today's, a
     * slight over-count of late-evening steps — preferred over dropping every step taken
     * before the launcher is first seen in the morning.
     */
    private fun recordReading(cumulative: Int): Int {
        val today = LocalDate.now().toEpochDay()
        val bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1)
        val lastReading = prefs.getInt(KEY_LAST_READING, -1)
        val lastStepsToday = prefs.getInt(KEY_LAST_STEPS_TODAY, 0)
        val lastBootTime = prefs.getLong(KEY_LAST_BOOT_TIME, -1)

        val hasPrevious = lastDay != -1L && lastReading != -1
        val sameBoot = hasPrevious &&
            abs(bootTime - lastBootTime) <= BOOT_TIME_TOLERANCE_MILLIS &&
            cumulative >= lastReading

        val stepsToday = when {
            !hasPrevious -> 0
            lastDay == today && sameBoot -> lastStepsToday + (cumulative - lastReading)
            lastDay == today -> lastStepsToday + cumulative
            lastDay == today - 1 && sameBoot -> cumulative - lastReading
            // Older than yesterday, or rebooted since: no usable baseline, start from here.
            else -> 0
        }

        prefs.edit()
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_LAST_READING, cumulative)
            .putInt(KEY_LAST_STEPS_TODAY, stepsToday)
            .putLong(KEY_LAST_BOOT_TIME, bootTime)
            .apply()

        return stepsToday
    }
}
