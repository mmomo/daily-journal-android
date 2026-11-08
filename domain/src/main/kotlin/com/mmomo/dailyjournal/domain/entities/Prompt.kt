package com.mmomo.dailyjournal.domain.entities

import com.mmomo.dailyjournal.domain.time.CalendarDay

/** The four prompt categories. [rawValue] is used in JSON, settings and the category key. */
enum class PromptCategory(val rawValue: String) {
    GRATITUDE("gratitude"),
    REFLECTION("reflection"),
    GOALS("goals"),
    CREATIVITY("creativity"),
    ;

    companion object {
        fun fromRawValue(raw: String): PromptCategory? = entries.firstOrNull { it.rawValue == raw }

        /** Sorted, comma-joined raw values; identical to iOS `PromptCategory.key(for:)`. */
        fun key(categories: Set<PromptCategory>): String =
            categories.map { it.rawValue }.sorted().joinToString(",")
    }
}

/** A library prompt (id `<category>-<NNN>`, 10–200 characters). */
data class Prompt(val id: String, val category: PromptCategory, val text: String)

/** Per-device state for one day's prompt (BR-P4–P6). Only today's and yesterday's are kept. */
data class DailyPromptState(
    val day: CalendarDay,
    val promptId: String,
    val refreshesUsed: Int,
    val shownPromptIds: List<String>,
    val categoriesKey: String,
)
