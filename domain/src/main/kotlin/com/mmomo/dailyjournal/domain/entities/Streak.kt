package com.mmomo.dailyjournal.domain.entities

import kotlin.time.Instant

sealed interface StreakStatus {
    /** No current streak. */
    data object None : StreakStatus

    /** The streak runs through today, or through yesterday with today still open. */
    data object Active : StreakStatus

    /** Yesterday is missing and today's 12:00 hasn't passed; a backfill restores [restorableCount] days. */
    data class AtRisk(val restorableCount: Int, val until: Instant) : StreakStatus
}

data class StreakSummary(val current: Int, val best: Int, val status: StreakStatus, val isTodayDone: Boolean) {
    companion object {
        val EMPTY = StreakSummary(current = 0, best = 0, status = StreakStatus.None, isTodayDone = false)
    }
}
