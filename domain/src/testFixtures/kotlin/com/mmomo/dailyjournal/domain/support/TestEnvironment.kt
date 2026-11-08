package com.mmomo.dailyjournal.domain.support

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlin.time.Instant

/** The real library's shape: 150 prompts per category with ids `<category>-001` … `-150`. */
val fullLibrary: List<Prompt> = libraryOf(perCategory = 150)

fun libraryOf(perCategory: Int): List<Prompt> = PromptCategory.entries.flatMap { category ->
    (1..perCategory).map { n ->
        val id = "${category.rawValue}-${n.toString().padStart(3, '0')}"
        Prompt(id = id, category = category, text = "Prompt text for $id, long enough")
    }
}

/** Fakes plus `JournalUseCases`, wired like production. */
class TestEnvironment(
    now: Instant = instant("2026-10-08 09:00"),
    prompts: List<Prompt> = fullLibrary,
    settings: AppSettings = AppSettings(onboardingCompleted = true),
) {
    val time = FixedTimeSource(now)
    val entries = FakeEntryRepository()
    val library = FakePromptLibrary(prompts)
    val promptState = FakePromptStateStore()
    val settings = FakeSettingsStore(settings)
    val reminders = FakeReminderScheduler()
    val widgets = FakeWidgetRefresher()
    val backgroundFailures = mutableListOf<Throwable>()
    val useCases = JournalUseCases(
        time, entries, library, promptState, this.settings, reminders, widgets,
        onBackgroundFailure = { backgroundFailures += it },
    )

    /** An existing entry for [dayKey], created at 21:00 that day. */
    fun entry(dayKey: String, text: String = "Some thoughts", promptId: String = "gratitude-001"): JournalEntry {
        val created = instant("$dayKey 21:00", time.timeZone)
        return JournalEntry(
            id = "id-$dayKey", day = day(dayKey), text = text, promptId = promptId,
            promptText = "Prompt text for $promptId, long enough", createdAt = created, updatedAt = created,
        )
    }

    fun withEntries(vararg dayKeys: String) = apply { dayKeys.forEach { entries.put(entry(it)) } }
}
