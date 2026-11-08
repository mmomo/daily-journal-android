package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.PromptLibrary
import com.mmomo.dailyjournal.domain.ports.PromptStateStore
import com.mmomo.dailyjournal.domain.ports.ReminderScheduler
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.ports.WidgetRefresher

/**
 * All 14 use cases, wired from the ports. Build one per process (the App container), so the single
 * write lock covers every caller (NFR design §3).
 *
 * @param onBackgroundFailure receives failures of best-effort work (widget refresh, reminder sync)
 *   so the caller can log the error type; journal text is never included.
 */
class JournalUseCases(
    time: TimeSource,
    entries: EntryRepository,
    prompts: PromptLibrary,
    promptState: PromptStateStore,
    settings: SettingsStore,
    reminders: ReminderScheduler,
    widgets: WidgetRefresher,
    onBackgroundFailure: (Throwable) -> Unit = {},
) {
    private val lock = JournalWriteLock()
    private val afterWrite = AfterWrite(widgets, onBackgroundFailure)
    private val resolver = PromptResolver(time, entries, prompts, promptState, settings)

    val syncReminderWithToday = SyncReminderWithTodayUseCase(time, entries, settings, reminders, afterWrite)
    val configureReminder = ConfigureReminderUseCase(settings, reminders, syncReminderWithToday)
    val getTodayState = GetTodayStateUseCase(time, entries, prompts, settings, resolver)
    val refreshPrompt = RefreshPromptUseCase(time, entries, settings, promptState, resolver, lock, afterWrite)
    val saveTodayEntry = SaveTodayEntryUseCase(time, entries, resolver, lock, afterWrite)
    val backfillYesterday = BackfillYesterdayUseCase(time, entries, resolver, lock, afterWrite)
    val getEntry = GetEntryUseCase(time, entries)
    val listEntries = ListEntriesUseCase(entries)
    val searchEntries = SearchEntriesUseCase(entries)
    val exportJournal = ExportJournalUseCase(time, entries)
    val getStreak = GetStreakUseCase(time, entries)
    val getSettings = GetSettingsUseCase(settings)
    val updatePromptCategories = UpdatePromptCategoriesUseCase(settings, afterWrite)
    val completeOnboarding = CompleteOnboardingUseCase(settings, configureReminder, afterWrite)

    init {
        afterWrite.syncReminder = { syncReminderWithToday.execute() }
    }
}
