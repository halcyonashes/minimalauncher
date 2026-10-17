package com.aegwyn.minimala.core.settings

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SettingsRepository"

private val Context.settingsDataStore by preferencesDataStore(name = "minimala_settings")

/** Reads/writes the whole [LauncherSettings] snapshot as one JSON blob in Preferences DataStore. */
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val settingsKey = stringPreferencesKey("launcher_settings_json")

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data.map { prefs ->
        decodeOrDefault(prefs[settingsKey])
    }

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[settingsKey])
            prefs[settingsKey] = json.encodeToString(transform(current))
        }
    }

    /**
     * Falls back to defaults on a decode failure so the launcher still renders, but logs it —
     * this runs before every [update], so a silent failure here would overwrite the stored blob
     * with defaults on the very next settings change.
     */
    private fun decodeOrDefault(raw: String?): LauncherSettings {
        if (raw == null) return LauncherSettings()
        return runCatching { json.decodeFromString<LauncherSettings>(raw) }
            .onFailure { Log.w(TAG, "Failed to decode stored settings, falling back to defaults", it) }
            .getOrDefault(LauncherSettings())
    }
}
