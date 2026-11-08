package com.mmomo.dailyjournal.domain.entities

import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlin.time.Instant

/** One day's entry. At most one per day; the prompt is frozen at creation (BR-E1, BR-E3). */
data class JournalEntry(
    val id: String,
    val day: CalendarDay,
    val text: String,
    val promptId: String,
    val promptText: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        /** Maximum entry length in Unicode code points (BR-E2, Android). */
        const val MAX_LENGTH = 20_000

        /** At least one non-whitespace character (BR-E2). */
        fun isValidText(text: String): Boolean {
            var index = 0
            while (index < text.length) {
                val codePoint = text.codePointAt(index)
                if (!isUnicodeWhitespace(codePoint)) return true
                index += Character.charCount(codePoint)
            }
            return false
        }

        fun isWithinLimit(text: String): Boolean = text.codePointCount(0, text.length) <= MAX_LENGTH

        /** Characters with the Unicode White_Space property (matches Swift `.whitespacesAndNewlines`). */
        private fun isUnicodeWhitespace(codePoint: Int): Boolean = when (codePoint) {
            0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x0085, 0x00A0, 0x1680,
            in 0x2000..0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
            else -> false
        }
    }
}

/** What may be done to a day's entry right now (BR-E5, BR-E6). */
sealed interface EntryEditability {
    data class Editable(val until: Instant) : EntryEditability
    data class Backfillable(val until: Instant) : EntryEditability
    data object Locked : EntryEditability

    val isEditable: Boolean get() = this is Editable
    val isBackfillable: Boolean get() = this is Backfillable
}

/** Yesterday can still be written until [until] (12:00 today). */
data class BackfillOffer(val day: CalendarDay, val prompt: Prompt, val until: Instant)

/** An entry together with what may be done to it now (US-2.4). */
data class EntryDetail(val entry: JournalEntry, val editability: EntryEditability)
