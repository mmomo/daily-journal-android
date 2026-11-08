package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.BackfillOffer
import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.TodayState
import com.mmomo.dailyjournal.domain.policies.EntryEditPolicy
import com.mmomo.dailyjournal.domain.policies.PromptSelector
import com.mmomo.dailyjournal.domain.policies.StreakCalculator
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.PromptLibrary
import com.mmomo.dailyjournal.domain.ports.PromptStateStore
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.time.CalendarDay

/** Everything the Today screen and the widget show (use case flow 1). */
class GetTodayStateUseCase internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val library: PromptLibrary,
    private val settings: SettingsStore,
    private val resolver: PromptResolver,
) {
    suspend fun execute(): TodayState {
        val now = time.now
        val timeZone = time.timeZone
        val today = CalendarDay.containing(now, timeZone)
        val yesterday = today.adding(-1)
        val appSettings = withStorage { settings.load() }

        val todayEntry = withStorage { entries.entry(today) }
        val futureEntry = if (todayEntry == null) {
            withStorage { entries.entriesAfter(today) }.minByOrNull { it.day }
        } else {
            null
        }
        val shownEntry = todayEntry ?: futureEntry

        val prompt: Prompt
        val refreshesRemaining: Int
        if (shownEntry != null) {
            prompt = answeredPrompt(shownEntry, library)
            refreshesRemaining = 0
        } else {
            val (resolved, state) = resolver.resolve(today)
            prompt = resolved
            refreshesRemaining = maxOf(0, PromptSelector.MAX_REFRESHES_PER_DAY - state.refreshesUsed)
        }

        val editability = if (futureEntry != null) {
            EntryEditability.Locked
        } else {
            EntryEditPolicy.editability(today, hasEntry = todayEntry != null, now = now, timeZone = timeZone)
        }

        val hasYesterday = withStorage { entries.entry(yesterday) } != null
        val yesterdayEditability = EntryEditPolicy.editability(yesterday, hasYesterday, now, timeZone)
        val backfillOffer = if (yesterdayEditability is EntryEditability.Backfillable) {
            BackfillOffer(day = yesterday, prompt = resolver.resolve(yesterday).first, until = yesterdayEditability.until)
        } else {
            null
        }

        val streak = StreakCalculator.summary(withStorage { entries.entryDays() }, now, timeZone)
        return TodayState(
            day = today,
            prompt = prompt,
            entry = shownEntry,
            entryEditability = editability,
            refreshesRemaining = refreshesRemaining,
            backfillOffer = backfillOffer,
            streak = streak,
            onboardingCompleted = appSettings.onboardingCompleted,
        )
    }
}

/** Swaps today's prompt, at most 3 times a day (use case flow 2, BR-P5–P7). */
class RefreshPromptUseCase internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val settings: SettingsStore,
    private val promptState: PromptStateStore,
    private val resolver: PromptResolver,
    private val lock: JournalWriteLock,
    private val afterWrite: AfterWrite,
) {
    suspend fun execute(): Prompt = lock.run {
        val today = time.today
        val todayEntry = withStorage { entries.entry(today) }
        val futureEntries = withStorage { entries.entriesAfter(today) }
        if (todayEntry != null || futureEntries.isNotEmpty()) throw JournalError.EntryLocked()

        val state = resolver.resolve(today).second
        if (state.refreshesUsed >= PromptSelector.MAX_REFRESHES_PER_DAY) throw JournalError.NoRefreshesLeft()

        val next = resolver.selector().refreshedPrompt(
            day = today,
            categories = withStorage { settings.load() }.categories,
            refreshNumber = state.refreshesUsed + 1,
            shownToday = state.shownPromptIds,
            answeredRecently = resolver.answeredRecently(today),
        )
        val updated = state.copy(
            promptId = next.id,
            refreshesUsed = state.refreshesUsed + 1,
            shownPromptIds = state.shownPromptIds + next.id,
        )
        withStorage { promptState.save(updated, keepingFrom = today.adding(-1)) }
        afterWrite.refreshWidget()
        next
    }
}
