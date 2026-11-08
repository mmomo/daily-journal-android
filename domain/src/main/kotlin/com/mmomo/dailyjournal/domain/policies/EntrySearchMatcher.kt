package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import java.text.Normalizer
import java.util.Locale

/** Whole-query substring match on entry or prompt text, ignoring case and accents (BR-Q1–Q3). */
object EntrySearchMatcher {
    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalizedQuery(query: String): String = query.trim()

    /** NFD decomposition, combining marks removed, case-folded (root locale). */
    fun normalize(text: String): String =
        combiningMarks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    fun matches(entry: JournalEntry, query: String): Boolean {
        val q = normalizedQuery(query)
        if (q.isEmpty()) return true
        val needle = normalize(q)
        return normalize(entry.text).contains(needle) || normalize(entry.promptText).contains(needle)
    }
}
