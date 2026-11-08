package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** Computes the streak from the set of entry days (BR-S1–S8). Pure; never stored. */
object StreakCalculator {
    fun summary(entryDays: Set<CalendarDay>, now: Instant, timeZone: TimeZone): StreakSummary {
        val today = CalendarDay.containing(now, timeZone)
        val yesterday = today.adding(-1)
        val dayBefore = today.adding(-2)
        val noon = today.noonIn(timeZone)
        val beforeNoon = now < noon

        // Future-dated entries (after westward travel) count as today (BR-E7).
        val days = entryDays.filter { it <= today }.mapTo(HashSet()) { it.dayNumber }
        if (entryDays.any { it > today }) days.add(today.dayNumber)

        fun run(endingAt: CalendarDay): Int {
            var count = 0
            var cursor = endingAt.dayNumber
            while (cursor in days) {
                count++
                cursor--
            }
            return count
        }

        val todayDone = today.dayNumber in days
        val yesterdayDone = yesterday.dayNumber in days
        val current: Int
        val status: StreakStatus

        if (todayDone) {
            val restorable = run(dayBefore)
            if (!yesterdayDone && beforeNoon && restorable > 0) {
                current = 1 // BR-S2
                status = StreakStatus.AtRisk(restorableCount = restorable, until = noon)
            } else {
                current = run(today) // BR-S1
                status = StreakStatus.Active
            }
        } else if (yesterdayDone) {
            current = run(yesterday) // BR-S3
            status = StreakStatus.Active
        } else {
            val restorable = run(dayBefore)
            if (beforeNoon && restorable > 0) {
                current = restorable // BR-S4
                status = StreakStatus.AtRisk(restorableCount = restorable, until = noon)
            } else {
                current = 0 // BR-S5
                status = StreakStatus.None
            }
        }

        val best = maxOf(longestRun(days), current) // BR-S6
        return StreakSummary(current = current, best = best, status = status, isTodayDone = todayDone)
    }

    internal fun longestRun(dayNumbers: Set<Int>): Int {
        var best = 0
        var length = 0
        var previous: Int? = null
        for (number in dayNumbers.sorted()) {
            length = if (previous != null && number == previous + 1) length + 1 else 1
            best = maxOf(best, length)
            previous = number
        }
        return best
    }
}
