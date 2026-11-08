package com.mmomo.dailyjournal.domain.ports

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

// Contract for every port: implementations are main-safe (they move storage work off the main
// thread themselves) and report I/O failures by throwing; use cases turn those into
// JournalError.StorageFailure (NFR design §5).

/** Supplies "now" and the device's current time zone. Injected everywhere so tests are deterministic (BR-T1). */
interface TimeSource {
    val now: Instant
    val timeZone: TimeZone
}

/** Journal entries. At most one entry per day. */
interface EntryRepository {
    /** Emits after any write. */
    val changes: Flow<Unit>

    suspend fun entry(day: CalendarDay): JournalEntry?

    /** Entries with `from <= day <= through`, any order. */
    suspend fun entries(from: CalendarDay, through: CalendarDay): List<JournalEntry>

    /** Entries dated after [day] (future-dated after westward travel). */
    suspend fun entriesAfter(day: CalendarDay): List<JournalEntry>

    /** All entries, any order. */
    suspend fun allEntries(): List<JournalEntry>

    /** The days that have an entry. */
    suspend fun entryDays(): Set<CalendarDay>

    /** Entries whose text or prompt text may contain [query]; the domain filters with EntrySearchMatcher. */
    suspend fun search(query: String): List<JournalEntry>

    /** Inserts or updates by id; one entry per day. */
    suspend fun save(entry: JournalEntry)
}

/** Read-only bundled prompts. */
interface PromptLibrary {
    fun allPrompts(): List<Prompt>
    fun prompt(id: String): Prompt?
}

/** Per-day prompt state on this device. */
interface PromptStateStore {
    val changes: Flow<Unit>
    suspend fun state(day: CalendarDay): DailyPromptState?

    /** Saves [state] and discards states for days before [keepingFrom]. */
    suspend fun save(state: DailyPromptState, keepingFrom: CalendarDay)
}

interface SettingsStore {
    val changes: Flow<Unit>
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

interface ReminderScheduler {
    /** Whether notifications may be posted. The permission dialog itself is shown by the App unit (BR-R7). */
    suspend fun authorization(): ReminderAuthorization

    /** Replaces every pending reminder this app scheduled with [reminders]. */
    suspend fun replacePending(reminders: List<ScheduledReminder>)
}

interface WidgetRefresher {
    suspend fun reloadAll()
}
