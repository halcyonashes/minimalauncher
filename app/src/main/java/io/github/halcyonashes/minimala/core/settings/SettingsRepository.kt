package io.github.halcyonashes.minimala.core.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SettingsRepository"

// A corrupt file would otherwise throw on every read, and the launcher can't be escaped:
// losing the settings beats a home screen that crashes on every start.
private val Context.settingsDataStore by preferencesDataStore(
    name = "minimala_settings",
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.w(TAG, "Settings file corrupt, replacing with defaults", it)
        emptyPreferences()
    },
)

/** Reads/writes the whole [LauncherSettings] snapshot as one JSON blob in Preferences DataStore. */
class SettingsRepository(private val context: Context) {

    // coerceInputValues: an enum value this build doesn't know (e.g. written by a newer
    // version) falls back to that field's default instead of failing the whole blob.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val settingsKey = stringPreferencesKey("launcher_settings_json")

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data
        .catch { e ->
            // An unreadable file must degrade to defaults, not tear down the collector.
            if (e !is IOException) throw e
            Log.w(TAG, "Failed to read settings, using defaults", e)
            emit(emptyPreferences())
        }
        .map { prefs -> prefs[settingsKey]?.let { decodeOrNull(it) } ?: LauncherSettings() }

    /** A failed write (e.g. disk full) is logged and dropped; callers run in viewModelScope, where a throw would crash. */
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        try {
            context.settingsDataStore.edit { prefs ->
                val raw = prefs[settingsKey]
                // A stored blob that won't decode is left untouched rather than overwritten with
                // defaults-plus-this-change: a later build may still read it. Leaving prefs
                // unchanged makes DataStore skip the write.
                val current = if (raw == null) LauncherSettings() else decodeOrNull(raw) ?: return@edit
                prefs[settingsKey] = json.encodeToString(transform(current))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write settings", e)
        }
    }

    /** Logged because a decode failure means the UI is showing defaults instead of the user's settings. */
    private fun decodeOrNull(raw: String): LauncherSettings? =
        runCatching { json.decodeFromString<LauncherSettings>(raw) }
            .onFailure { Log.w(TAG, "Failed to decode stored settings", it) }
            .getOrNull()
}
