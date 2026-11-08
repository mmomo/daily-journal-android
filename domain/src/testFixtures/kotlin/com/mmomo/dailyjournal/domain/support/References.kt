package com.mmomo.dailyjournal.domain.support

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import java.text.Normalizer

/** Simple reference implementations used as oracles (PBT-05). */
object References {
    /** Longest run of consecutive days, counted the slow obvious way. */
    fun longestRun(dayNumbers: Set<Int>): Int {
        var best = 0
        for (start in dayNumbers) {
            var length = 0
            while (start + length in dayNumbers) length++
            best = maxOf(best, length)
        }
        return best
    }

    /** Accent- and case-insensitive substring search, written independently of EntrySearchMatcher. */
    fun matches(entry: JournalEntry, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .lowercase()
        return fold(entry.text).contains(fold(q)) || fold(entry.promptText).contains(fold(q))
    }
}
