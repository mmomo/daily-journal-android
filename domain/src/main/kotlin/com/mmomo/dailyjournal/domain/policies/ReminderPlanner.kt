package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.time.DayMath
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** Builds the rolling 7-day reminder window (BR-R1–R5). Pure; scheduling happens through the port. */
object ReminderPlanner {
    const val WINDOW_DAYS = 7
    const val TITLE = "Time to journal"
    const val BASE_BODY = "Your daily prompt is waiting."

    fun plan(
        settings: ReminderSettings,
        isAllowed: Boolean,
        streak: StreakSummary,
        now: Instant,
        timeZone: TimeZone,
    ): List<ScheduledReminder> {
        if (!settings.isEnabled || !isAllowed) return emptyList() // BR-R1
        val today = CalendarDay.containing(now, timeZone)
        val reminders = mutableListOf<ScheduledReminder>()
        for (offset in 0 until WINDOW_DAYS) {
            val day = today.adding(offset)
            val fire = DayMath.instant(day, settings.hour, settings.minute, timeZone) // BR-R3
            if (fire <= now) continue
            if (offset == 0 && streak.isTodayDone) continue // BR-R2
            var body = BASE_BODY
            if (offset == 0 && streak.status == StreakStatus.Active && streak.current >= 1) { // BR-R4
                body += " Keep your ${streak.current}-day streak going."
            }
            reminders += ScheduledReminder(
                identifier = "reminder-${day.key}",
                day = day,
                fireAt = fire,
                title = TITLE,
                body = body,
                deepLink = DeepLink.TODAY_EDITOR, // BR-R5
            )
        }
        return reminders
    }
}
