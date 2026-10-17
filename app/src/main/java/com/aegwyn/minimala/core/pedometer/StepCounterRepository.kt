package com.aegwyn.minimala.core.pedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFS_NAME = "minimala_pedometer"
private const val KEY_BASELINE = "baseline_steps"
private const val KEY_BASELINE_DAY = "baseline_epoch_day"

/**
 * Wraps [Sensor.TYPE_STEP_COUNTER], which reports a running total since the last reboot rather
 * than a daily count. A local baseline (today's first reading) is persisted so "steps today" can
 * be derived as `cumulative - baseline`, the same trick every pedometer app uses.
 */
class StepCounterRepository(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSensor(): Boolean = stepCounterSensor != null

    /** Below API 29 the sensor was ungated, so there is nothing to check. */
    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Emits the step count since local midnight, starting with an immediate `0`.
     *
     * TYPE_STEP_COUNTER is on-change: some OEM HALs only redeliver the last value once a step
     * is actually taken after registration, not on registration itself. Without the eager `0`
     * this flow could stay silent indefinitely on a stationary device, and since it's one leg
     * of a `combine()` upstream in the ViewModel, that silence blocks every other emission too
     * (settings, app list) — not just the step count.
     */
    fun stepsToday(): Flow<Int> = callbackFlow {
        trySend(0)

        val sensor = stepCounterSensor
        if (sensor == null || !hasPermission()) {
            awaitClose {}
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(stepsSinceBaseline(event.values[0].toInt()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /**
     * Resets the baseline when the day rolls over, or when the cumulative reading drops below
     * the stored baseline — the only way that happens is a device reboot zeroing the sensor.
     */
    private fun stepsSinceBaseline(cumulative: Int): Int {
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(KEY_BASELINE_DAY, -1)
        var baseline = prefs.getInt(KEY_BASELINE, -1)

        if (storedDay != today || baseline == -1 || cumulative < baseline) {
            baseline = cumulative
            prefs.edit()
                .putLong(KEY_BASELINE_DAY, today)
                .putInt(KEY_BASELINE, baseline)
                .apply()
        }

        return (cumulative - baseline).coerceAtLeast(0)
    }
}
