package com.aegwyn.minimala

import android.app.Application
import com.aegwyn.minimala.core.apps.AppRepository
import com.aegwyn.minimala.core.calendar.CalendarRepository
import com.aegwyn.minimala.core.pedometer.StepCounterRepository
import com.aegwyn.minimala.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MinimalaApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob())

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var calendarRepository: CalendarRepository
        private set
    lateinit var stepCounterRepository: StepCounterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        appRepository = AppRepository(this, appScope)
        calendarRepository = CalendarRepository(this)
        stepCounterRepository = StepCounterRepository(this)
    }
}
