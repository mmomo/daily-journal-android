package com.mmomo.dailyjournal.domain.entities

import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlin.time.Instant

/** Whether the app may post notifications. On Android 10–12 no runtime permission exists. */
enum class ReminderAuthorization { NOT_DETERMINED, ALLOWED, DENIED }

/** One reminder to schedule (BR-R2–R5). */
data class ScheduledReminder(
    val identifier: String,
    val day: CalendarDay,
    val fireAt: Instant,
    val title: String,
    val body: String,
    val deepLink: DeepLink,
)
