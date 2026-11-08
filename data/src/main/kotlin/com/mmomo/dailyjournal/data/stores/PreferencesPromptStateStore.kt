package com.mmomo.dailyjournal.data.stores

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.ports.PromptStateStore
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** `PromptStateStore` on DataStore: per-day states, atomic save and prune (DATA-S1, S3–S6). */
class PreferencesPromptStateStore(
    private val store: DataStore<Preferences>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PromptStateStore {
    override val changes: Flow<Unit> = JournalPreferences.safeData(store)
        .map { it[JournalPreferences.PROMPT_STATE] }
        .distinctUntilChanged()
        .drop(1)
        .map { }

    override suspend fun state(day: CalendarDay): DailyPromptState? = withContext(io) {
        decode(JournalPreferences.safeData(store).first()[JournalPreferences.PROMPT_STATE])[day]
    }

    /** Read, add, prune and write in one DataStore transaction, so concurrent saves never lose an update. */
    override suspend fun save(state: DailyPromptState, keepingFrom: CalendarDay) {
        withContext(io) {
            store.edit { preferences ->
                val all = decode(preferences[JournalPreferences.PROMPT_STATE]).toMutableMap()
                all[state.day] = state
                preferences[JournalPreferences.PROMPT_STATE] = PromptStateJson.encode(all.filterKeys { it >= keepingFrom })
            }
        }
    }

    private fun decode(json: String?): Map<CalendarDay, DailyPromptState> {
        if (json == null) return emptyMap()
        return PromptStateJson.decodeOrNull(json) ?: run {
            Log.w("DailyJournal", "Stored prompt state is unreadable; starting fresh")
            emptyMap()
        }
    }
}
