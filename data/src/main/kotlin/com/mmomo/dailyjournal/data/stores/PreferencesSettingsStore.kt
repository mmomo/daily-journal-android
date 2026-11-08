package com.mmomo.dailyjournal.data.stores

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** `SettingsStore` on DataStore (DATA-S1, S2, S6, S7). */
class PreferencesSettingsStore(
    private val store: DataStore<Preferences>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : SettingsStore {
    override val changes: Flow<Unit> = JournalPreferences.safeData(store)
        .map { it[JournalPreferences.SETTINGS] }
        .distinctUntilChanged()
        .drop(1)
        .map { }

    override suspend fun load(): AppSettings = withContext(io) {
        val json = JournalPreferences.safeData(store).first()[JournalPreferences.SETTINGS] ?: return@withContext AppSettings()
        SettingsJson.decodeOrNull(json) ?: run {
            Log.w("DailyJournal", "Stored settings are unreadable; using defaults")
            AppSettings()
        }
    }

    override suspend fun save(settings: AppSettings) {
        withContext(io) { store.edit { it[JournalPreferences.SETTINGS] = SettingsJson.encode(settings) } }
    }
}
