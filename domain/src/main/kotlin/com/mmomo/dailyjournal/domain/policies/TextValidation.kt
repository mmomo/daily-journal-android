package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError

/** Entry text checks run before any write (BR-E2, DOM-SEC-3). */
object TextValidation {
    fun validate(text: String) {
        if (!JournalEntry.isValidText(text)) throw JournalError.EmptyText()
        if (!JournalEntry.isWithinLimit(text)) throw JournalError.TextTooLong()
    }
}
