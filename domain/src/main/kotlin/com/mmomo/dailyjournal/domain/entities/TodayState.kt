package com.mmomo.dailyjournal.domain.entities

import com.mmomo.dailyjournal.domain.time.CalendarDay

/** Everything the Today screen and the widget need. */
data class TodayState(
    val day: CalendarDay,
    val prompt: Prompt,
    val entry: JournalEntry?,
    val entryEditability: EntryEditability,
    val refreshesRemaining: Int,
    val backfillOffer: BackfillOffer?,
    val streak: StreakSummary,
    val onboardingCompleted: Boolean,
) {
    val isTodayDone: Boolean get() = streak.isTodayDone
}
