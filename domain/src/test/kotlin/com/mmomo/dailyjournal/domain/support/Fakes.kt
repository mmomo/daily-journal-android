package com.mmomo.dailyjournal.domain.support

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.PromptLibrary
import com.mmomo.dailyjournal.domain.ports.PromptStateStore
import com.mmomo.dailyjournal.domain.ports.ReminderScheduler
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import com.mmomo.dailyjournal.domain.ports.WidgetRefresher
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.IOException

class FakeEntryRepository : EntryRepository {
    val stored = LinkedHashMap<CalendarDay, JournalEntry>()
    var failing = false
    var saves = 0
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    private fun checkAvailable() {
        if (failing) throw IOException("simulated disk failure")
    }

    override suspend fun entry(day: CalendarDay): JournalEntry? = checkAvailable().let { stored[day] }
    override suspend fun entries(from: CalendarDay, through: CalendarDay) =
        checkAvailable().let { stored.values.filter { it.day in from..through } }
    override suspend fun entriesAfter(day: CalendarDay) = checkAvailable().let { stored.values.filter { it.day > day } }
    override suspend fun allEntries() = checkAvailable().let { stored.values.toList() }
    override suspend fun entryDays() = checkAvailable().let { stored.keys.toSet() }
    override suspend fun search(query: String) =
        checkAvailable().let { stored.values.filter { EntrySearchMatcher.matches(it, query) } }

    override suspend fun save(entry: JournalEntry) {
        checkAvailable()
        stored[entry.day] = entry
        saves++
        changes.tryEmit(Unit)
    }

    fun put(vararg entries: JournalEntry) = entries.forEach { stored[it.day] = it }
}

class FakePromptLibrary(private val prompts: List<Prompt>) : PromptLibrary {
    private val byId = prompts.associateBy { it.id }
    override fun allPrompts() = prompts
    override fun prompt(id: String) = byId[id]
}

class FakePromptStateStore : PromptStateStore {
    val states = HashMap<CalendarDay, DailyPromptState>()
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    override suspend fun state(day: CalendarDay) = states[day]
    override suspend fun save(state: DailyPromptState, keepingFrom: CalendarDay) {
        states[state.day] = state
        states.keys.removeAll { it < keepingFrom }
        changes.tryEmit(Unit)
    }
}

class FakeSettingsStore(var settings: AppSettings = AppSettings()) : SettingsStore {
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    override suspend fun load() = settings
    override suspend fun save(settings: AppSettings) {
        this.settings = settings
        changes.tryEmit(Unit)
    }
}

class FakeReminderScheduler(var authorization: ReminderAuthorization = ReminderAuthorization.ALLOWED) : ReminderScheduler {
    var pending: List<ScheduledReminder> = emptyList()
    var replaceCalls = 0
    var failing = false
    override suspend fun authorization() = authorization
    override suspend fun replacePending(reminders: List<ScheduledReminder>) {
        if (failing) throw IOException("simulated scheduler failure")
        pending = reminders
        replaceCalls++
    }
}

class FakeWidgetRefresher : WidgetRefresher {
    var reloads = 0
    var failing = false
    override suspend fun reloadAll() {
        if (failing) throw IllegalStateException("simulated widget failure")
        reloads++
    }
}
