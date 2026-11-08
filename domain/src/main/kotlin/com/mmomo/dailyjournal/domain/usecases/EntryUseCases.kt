package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.EntryDetail
import com.mmomo.dailyjournal.domain.entities.ExportDocument
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.policies.EntryEditPolicy
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.policies.JournalExporter
import com.mmomo.dailyjournal.domain.policies.StreakCalculator
import com.mmomo.dailyjournal.domain.policies.TextValidation
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.time.CalendarDay
import java.util.UUID
import kotlin.time.Instant

/**
 * Creates or updates the entry for [day] (use case flow 3, BR-E1–E4, BR-E7).
 * [editedAt] is when the text last changed; it lets a save that arrives just after midnight keep
 * text typed before midnight (BR-E4, Android).
 */
class SaveTodayEntryUseCase internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val resolver: PromptResolver,
    private val lock: JournalWriteLock,
    private val afterWrite: AfterWrite,
) {
    suspend fun execute(text: String, day: CalendarDay, editedAt: Instant): JournalEntry = lock.run {
        TextValidation.validate(text)
        val now = time.now
        val timeZone = time.timeZone
        val today = CalendarDay.containing(now, timeZone)

        val allowed = when {
            day == today -> true
            day < today -> EntryEditPolicy.lateSaveAllowed(day, editedAt, now, timeZone)
            else -> false
        }
        if (!allowed) throw JournalError.EntryLocked()
        if (withStorage { entries.entriesAfter(today) }.isNotEmpty()) throw JournalError.EntryLocked() // BR-E7

        val existing = withStorage { entries.entry(day) }
        val entry = existing?.copy(text = text, updatedAt = now) ?: run {
            val prompt = resolver.resolve(day).first
            JournalEntry(
                id = UUID.randomUUID().toString(),
                day = day,
                text = text,
                promptId = prompt.id,
                promptText = prompt.text,
                createdAt = now,
                updatedAt = now,
            )
        }
        withStorage { entries.save(entry) }
        afterWrite.refreshWidgetAndReminders()
        entry
    }
}

/** Writes yesterday's missing entry before today's 12:00 (use case flow 4, BR-E8, BR-E9). */
class BackfillYesterdayUseCase internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val resolver: PromptResolver,
    private val lock: JournalWriteLock,
    private val afterWrite: AfterWrite,
) {
    suspend fun execute(text: String, day: CalendarDay): JournalEntry = lock.run {
        TextValidation.validate(text)
        val now = time.now
        val yesterday = CalendarDay.containing(now, time.timeZone).adding(-1)
        if (day != yesterday) throw JournalError.BackfillNotAvailable()
        val existing = withStorage { entries.entry(yesterday) }
        val editability = EntryEditPolicy.editability(yesterday, existing != null, now, time.timeZone)
        if (!editability.isBackfillable) throw JournalError.BackfillNotAvailable()

        val prompt = resolver.resolve(yesterday).first
        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            day = yesterday,
            text = text,
            promptId = prompt.id,
            promptText = prompt.text,
            createdAt = now,
            updatedAt = now,
        )
        withStorage { entries.save(entry) }
        afterWrite.refreshWidgetAndReminders()
        entry
    }
}

/** One entry with what may be done to it now (use case flow 5, US-2.4). */
class GetEntryUseCase internal constructor(private val time: TimeSource, private val entries: EntryRepository) {
    suspend fun execute(day: CalendarDay): EntryDetail? {
        val entry = withStorage { entries.entry(day) } ?: return null
        return EntryDetail(entry, EntryEditPolicy.editability(day, hasEntry = true, now = time.now, timeZone = time.timeZone))
    }
}

/** History list, newest day first (use case flow 6, US-3.1). */
class ListEntriesUseCase internal constructor(private val entries: EntryRepository) {
    suspend fun execute(): List<JournalEntry> = withStorage { entries.allEntries() }.sortedNewestFirst()
}

/** Whole-query substring search (use case flow 7, BR-Q1–Q3). */
class SearchEntriesUseCase internal constructor(private val entries: EntryRepository) {
    suspend fun execute(query: String): List<JournalEntry> {
        val q = EntrySearchMatcher.normalizedQuery(query)
        if (q.isEmpty()) return withStorage { entries.allEntries() }.sortedNewestFirst()
        return withStorage { entries.search(q) }
            .filter { EntrySearchMatcher.matches(it, q) }
            .sortedNewestFirst()
    }
}

/** Markdown or plain-text export (use case flow 8, BR-X1–X4). */
class ExportJournalUseCase internal constructor(private val time: TimeSource, private val entries: EntryRepository) {
    suspend fun execute(format: ExportFormat): ExportDocument {
        val all = withStorage { entries.allEntries() }
        if (all.isEmpty()) throw JournalError.NothingToExport()
        return JournalExporter.export(all, format, exportDay = time.today)
    }
}

/** Current and best streak (use case flow 9, US-4.1–4.4). */
class GetStreakUseCase internal constructor(private val time: TimeSource, private val entries: EntryRepository) {
    suspend fun execute(): StreakSummary =
        StreakCalculator.summary(withStorage { entries.entryDays() }, time.now, time.timeZone)
}
