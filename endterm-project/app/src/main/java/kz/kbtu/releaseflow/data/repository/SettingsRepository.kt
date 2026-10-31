package kz.kbtu.releaseflow.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The app's settings as one immutable value; defaults apply until the user changes something. */
data class Settings(
    val syncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 6 * 60,
    val notificationsEnabled: Boolean = true,
    val storeCountry: String = "kz",
    val lastSyncAt: Long? = null,
    val lastSyncSummary: String? = null,
) {
    companion object {
        /** The choices offered on the Settings screen (WorkManager's minimum period is 15 minutes). */
        val intervals: List<Int> = listOf(15, 60, 6 * 60, 24 * 60)
        val countries: List<String> = listOf("kz", "us", "ru")
    }
}

/** One DataStore per process; the delegate creates the file "settings.preferences_pb" lazily. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferences DataStore: the second kind of persistent data in the app. Unlike Room it holds
 * a handful of typed key-value pairs, is read as a `Flow` and written in atomic `edit` blocks.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_minutes")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val COUNTRY = stringPreferencesKey("store_country")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_SUMMARY = stringPreferencesKey("last_sync_summary")
    }

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            syncEnabled = prefs[Keys.SYNC_ENABLED] ?: true,
            syncIntervalMinutes = prefs[Keys.SYNC_INTERVAL] ?: 6 * 60,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            storeCountry = prefs[Keys.COUNTRY] ?: "kz",
            lastSyncAt = prefs[Keys.LAST_SYNC_AT],
            lastSyncSummary = prefs[Keys.LAST_SYNC_SUMMARY],
        )
    }

    suspend fun setSyncEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SYNC_ENABLED] = enabled }

    suspend fun setSyncInterval(minutes: Int) = dataStore.edit { it[Keys.SYNC_INTERVAL] = minutes }

    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun setStoreCountry(country: String) = dataStore.edit { it[Keys.COUNTRY] = country }

    suspend fun recordSync(at: Long, summary: String) = dataStore.edit {
        it[Keys.LAST_SYNC_AT] = at
        it[Keys.LAST_SYNC_SUMMARY] = summary
    }
}
