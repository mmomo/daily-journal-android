package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Decides what may be done with a day's entry at a given instant (BR-E4–E6). */
object EntryEditPolicy {
    /** How long after midnight a save of text edited before midnight is still accepted (BR-E4, Android). */
    val LATE_SAVE_GRACE = 5.seconds

    fun editability(day: CalendarDay, hasEntry: Boolean, now: Instant, timeZone: TimeZone): EntryEditability {
        val today = CalendarDay.containing(now, timeZone)
        if (day == today) return EntryEditability.Editable(until = today.adding(1).startIn(timeZone))
        if (day == today.adding(-1) && !hasEntry) {
            val deadline = backfillDeadline(today, timeZone)
            if (now < deadline) return EntryEditability.Backfillable(until = deadline)
        }
        return EntryEditability.Locked
    }

    /** 12:00 local on [today] (BR-E5, FR-2.4). */
    fun backfillDeadline(today: CalendarDay, timeZone: TimeZone): Instant = today.noonIn(timeZone)

    /**
     * BR-E4 (Android): a save for [day] arriving after its midnight is accepted only if the text was
     * edited before midnight and the save arrives within [LATE_SAVE_GRACE]. An [editedAt] later than
     * [now] (clock moved back) counts as [now] (BR-SEC3).
     */
    fun lateSaveAllowed(day: CalendarDay, editedAt: Instant, now: Instant, timeZone: TimeZone): Boolean {
        val midnight = day.adding(1).startIn(timeZone)
        val edited = minOf(editedAt, now)
        return now >= midnight && edited < midnight && now < midnight + LATE_SAVE_GRACE
    }
}
