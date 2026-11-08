package com.mmomo.dailyjournal.app.ui.components

import com.mmomo.dailyjournal.domain.entities.JournalError

/** Generic, user-facing wording for domain errors (App NFR design §10). Never includes technical detail. */
fun userMessage(error: Throwable): String = when (error) {
    is JournalError.EmptyText -> "Write something first."
    is JournalError.TextTooLong -> "You've reached the maximum length for one entry."
    is JournalError.EntryLocked -> "This entry is locked."
    is JournalError.BackfillNotAvailable -> "Yesterday can no longer be filled in."
    is JournalError.NoRefreshesLeft -> "No refreshes left today."
    is JournalError.CategoriesEmpty -> "Choose at least one category."
    is JournalError.NotificationsDenied -> "Notifications are off for this app."
    is JournalError.NothingToExport -> "No entries to export yet."
    else -> "Couldn't save. Try again."
}
