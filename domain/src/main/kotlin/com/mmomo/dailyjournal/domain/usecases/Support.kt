package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.policies.PromptSelector
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.PromptLibrary
import com.mmomo.dailyjournal.domain.ports.PromptStateStore
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.ports.WidgetRefresher
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a port call. `JournalError` and cancellation pass through; anything else becomes
 * `StorageFailure` carrying only the exception class name (NFR design §1).
 */
internal suspend fun <T> withStorage(operation: suspend () -> T): T = try {
    operation()
} catch (error: CancellationException) {
    throw error
} catch (error: JournalError) {
    throw error
} catch (error: Exception) {
    throw JournalError.StorageFailure(reason = error::class.java.name, cause = error)
}

/**
 * Runs saves, backfills and refreshes one at a time, in arrival order, whoever calls them
 * (NFR design §3). One instance per `JournalUseCases`.
 */
class JournalWriteLock {
    private val mutex = Mutex()
    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }
}

/**
 * Best-effort side effects after a successful write: refresh the widget and, where reminders depend on
 * it, recompute them. Failures never undo or fail the write; they go to [onBackgroundFailure] so the
 * caller can log the error type (NFR design §2, DOM-REL-5).
 */
class AfterWrite internal constructor(
    private val widgets: WidgetRefresher,
    private val onBackgroundFailure: (Throwable) -> Unit,
) {
    internal var syncReminder: (suspend () -> Unit)? = null

    internal suspend fun refreshWidget() = bestEffort { widgets.reloadAll() }

    internal suspend fun refreshWidgetAndReminders() {
        syncReminder?.invoke()
        refreshWidget()
    }

    internal suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            onBackgroundFailure(error)
        }
    }
}

internal val TimeSource.today: CalendarDay get() = CalendarDay.containing(now, timeZone)

/** The prompt an entry answered: the library version if it still exists, otherwise its snapshot. */
internal fun answeredPrompt(entry: JournalEntry, library: PromptLibrary): Prompt =
    library.prompt(entry.promptId) ?: Prompt(
        id = entry.promptId,
        category = PromptCategory.fromRawValue(entry.promptId.substringBefore('-')) ?: PromptCategory.REFLECTION,
        text = entry.promptText,
    )

/** Newest day first, ties by newest creation (same order as iOS). */
internal fun List<JournalEntry>.sortedNewestFirst(): List<JournalEntry> =
    sortedWith(compareByDescending<JournalEntry> { it.day }.thenByDescending { it.createdAt })

/** Resolves "the prompt for day d on this device" (business-logic-model §1.4). */
class PromptResolver internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val library: PromptLibrary,
    private val promptState: PromptStateStore,
    private val settings: SettingsStore,
) {
    internal fun selector(): PromptSelector = PromptSelector(library.allPrompts())

    /** Prompt ids answered in the 30 days before [day] (BR-P3). */
    internal suspend fun answeredRecently(day: CalendarDay): Set<String> {
        val recent = withStorage {
            entries.entries(from = day.adding(-PromptSelector.NO_REPEAT_WINDOW_DAYS), through = day.adding(-1))
        }
        return recent.mapTo(HashSet()) { it.promptId }
    }

    /** BR-P4, BR-P8: the stored prompt for [day], or a fresh deterministic pick that is then stored. */
    internal suspend fun resolve(day: CalendarDay): Pair<Prompt, DailyPromptState> {
        val stored = withStorage { promptState.state(day) }
        val storedPrompt = stored?.let { library.prompt(it.promptId) }
        if (stored != null && storedPrompt != null) return storedPrompt to stored

        val categories = withStorage { settings.load() }.categories
        val prompt = selector().dailyPrompt(day, categories, answeredRecently(day))
        val state = DailyPromptState(
            day = day,
            promptId = prompt.id,
            refreshesUsed = 0,
            shownPromptIds = listOf(prompt.id),
            categoriesKey = PromptCategory.key(categories),
        )
        withStorage { promptState.save(state, keepingFrom = time.today.adding(-1)) }
        return prompt to state
    }
}
