package com.mmomo.dailyjournal.domain.entities

/**
 * The only errors use cases throw (domain-entities §6). Messages never contain journal text (BR-SEC5).
 * User-facing wording is owned by the App unit.
 */
sealed class JournalError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class EmptyText : JournalError("Entry text is empty")
    class TextTooLong : JournalError("Entry text is longer than ${JournalEntry.MAX_LENGTH} characters")
    class EntryLocked : JournalError("Entry is locked")
    class BackfillNotAvailable : JournalError("Backfill is not available")
    class NoRefreshesLeft : JournalError("No refreshes left today")
    class CategoriesEmpty : JournalError("At least one category is required")
    class NotificationsDenied : JournalError("Notifications are not allowed")
    class NothingToExport : JournalError("No entries to export")

    /** A storage port failed. [reason] is technical (an exception class name) and for logs only. */
    class StorageFailure(val reason: String, cause: Throwable? = null) : JournalError("Storage failure: $reason", cause)
}
