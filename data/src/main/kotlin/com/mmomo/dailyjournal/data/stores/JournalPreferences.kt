package com.mmomo.dailyjournal.data.stores

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

private const val TAG = "DailyJournal"

/**
 * The app's single preferences DataStore (`journal_preferences`), in private storage. A corrupted file
 * is replaced with empty preferences and the error type logged (NFR design §3, Q3:A).
 */
private val Context.journalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "journal_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { error ->
        Log.e(TAG, "Preferences file was corrupted and has been reset: ${error::class.java.name}")
        emptyPreferences()
    },
)

object JournalPreferences {
    val SETTINGS = stringPreferencesKey("settings.v1")
    val PROMPT_STATE = stringPreferencesKey("promptState.v1")
    val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notificationPermissionAsked.v1")

    /** The one DataStore for this process (DataStore forbids two instances on one file). */
    fun dataStore(context: Context): DataStore<Preferences> = context.applicationContext.journalDataStore

    /** DataStore data where read I/O errors are logged and treated as empty preferences. */
    fun safeData(store: DataStore<Preferences>): Flow<Preferences> = store.data.catch { error ->
        if (error is IOException) {
            Log.e(TAG, "Preferences read failed: ${error::class.java.name}")
            emit(emptyPreferences())
        } else {
            throw error
        }
    }
}
