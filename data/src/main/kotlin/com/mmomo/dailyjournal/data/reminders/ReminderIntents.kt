package com.mmomo.dailyjournal.data.reminders

import android.content.Intent
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.time.CalendarDay

/** Intent contents for reminder alarms. Only the day key travels; the prompt is read fresh when it fires. */
object ReminderIntents {
    const val ACTION_REMINDER = "com.mmomo.dailyjournal.action.REMINDER"
    const val ALARM_REQUEST_CODE = 1
    private const val EXTRA_DAY = "com.mmomo.dailyjournal.extra.DAY"

    fun withReminder(intent: Intent, reminder: ScheduledReminder): Intent = intent.putExtra(EXTRA_DAY, reminder.day.key)

    /** The reminder's day, or `null` if the extra is missing or malformed. */
    fun day(intent: Intent?): CalendarDay? = CalendarDay.fromKey(intent?.getStringExtra(EXTRA_DAY))
}
