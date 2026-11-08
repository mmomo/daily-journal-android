package com.mmomo.dailyjournal.data.database

import android.util.Log
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/** `EntryRepository` on Room (data business rules DATA-E1–E9). Main-safe: all work on [io]. */
class DatabaseEntryRepository(
    private val database: JournalDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : EntryRepository {
    private val dao = database.entries()

    /** Emits after every write to the entries table, from any code path (Q1:A); the initial value is dropped. */
    override val changes: Flow<Unit> =
        database.invalidationTracker.createFlow(TABLE).drop(1).map { }

    override suspend fun entry(day: CalendarDay): JournalEntry? = withContext(io) {
        dao.byDayKey(day.key)?.let(::toDomainOrSkip)
    }

    override suspend fun entries(from: CalendarDay, through: CalendarDay): List<JournalEntry> = withContext(io) {
        dao.inRange(from.dayNumber, through.dayNumber).mapNotNull(::toDomainOrSkip)
    }

    override suspend fun entriesAfter(day: CalendarDay): List<JournalEntry> = withContext(io) {
        dao.after(day.dayNumber).mapNotNull(::toDomainOrSkip)
    }

    override suspend fun allEntries(): List<JournalEntry> = withContext(io) {
        dao.all().mapNotNull(::toDomainOrSkip)
    }

    override suspend fun entryDays(): Set<CalendarDay> = allEntries().mapTo(HashSet()) { it.day }

    /** Loads all entries and filters with the domain matcher, so results always equal the domain rules (DATA-E4). */
    override suspend fun search(query: String): List<JournalEntry> =
        allEntries().filter { EntrySearchMatcher.matches(it, query) }

    /** Inserts, or updates the row with the same id. A different entry on the same day fails (DATA-E1). */
    override suspend fun save(entry: JournalEntry) = withContext(io) {
        val row = toRow(entry)
        if (dao.byId(row.id) != null) dao.update(row) else dao.insert(row)
    }

    private fun toDomainOrSkip(row: EntryEntity): JournalEntry? {
        val day = CalendarDay.fromKey(row.dayKey)
        if (day == null) {
            // Ids and error types only, never entry text (DATA-SEC-2).
            Log.w(TAG, "Skipping unreadable entry ${row.id}: InvalidDayKey")
            return null
        }
        return JournalEntry(
            id = row.id,
            day = day,
            text = row.text,
            promptId = row.promptId,
            promptText = row.promptText,
            createdAt = Instant.fromEpochMilliseconds(row.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(row.updatedAt),
        )
    }

    companion object {
        private const val TABLE = "entries"
        private const val TAG = "DailyJournal"

        fun toRow(entry: JournalEntry) = EntryEntity(
            id = entry.id,
            dayKey = entry.day.key,
            dayNumber = entry.day.dayNumber,
            text = entry.text,
            promptId = entry.promptId,
            promptText = entry.promptText,
            createdAt = entry.createdAt.toEpochMilliseconds(),
            updatedAt = entry.updatedAt.toEpochMilliseconds(),
        )
    }
}
