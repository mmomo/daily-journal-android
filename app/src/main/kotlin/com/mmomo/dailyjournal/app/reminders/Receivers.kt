package com.mmomo.dailyjournal.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mmomo.dailyjournal.app.JournalApplication
import com.mmomo.dailyjournal.data.composition.JournalEnvironment
import com.mmomo.dailyjournal.data.composition.JournalEnvironmentProvider
import com.mmomo.dailyjournal.data.system.SystemTimeSource
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.policies.ReminderPlanner
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.time.DayMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Runs [work] off the main thread with a 9-second limit, then finishes the broadcast (App NFR design §8). */
private fun BroadcastReceiver.runAsync(context: Context, name: String, work: suspend (JournalEnvironment) -> Unit) {
    val pending = goAsync()
    val provider = context.applicationContext as JournalEnvironmentProvider
    receiverScope.launch {
        try {
            withTimeout(9.seconds) { work(provider.awaitEnvironment()) }
        } catch (error: Exception) {
            Log.w(JournalApplication.TAG, "$name failed: ${error::class.java.name}")
        } finally {
            pending.finish()
        }
    }
}

/** The daily alarm fired: show today's reminder if today isn't written yet, then register the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context, "Reminder") { environment ->
            todaysReminder(environment)?.let { environment.reminderNotifier?.post(it) }
            environment.useCases.syncReminderWithToday.execute()
        }
    }

    /** Today's reminder exactly as the planner would build it (BR-R2 skip, BR-R4 streak text), or `null`. */
    private suspend fun todaysReminder(environment: JournalEnvironment): ScheduledReminder? {
        val useCases = environment.useCases
        val settings = useCases.getSettings.execute()
        val state = useCases.getTodayState.execute()
        val zone = SystemTimeSource().timeZone
        val today: CalendarDay = state.day
        // Plan as of just before today's reminder time, so today's reminder is included if it applies.
        val justBefore = DayMath.instant(today, settings.reminder.hour, settings.reminder.minute, zone) - 1.milliseconds
        return ReminderPlanner.plan(settings.reminder, isAllowed = true, state.streak, justBefore, zone)
            .firstOrNull { it.day == today }
    }
}

/** Boot, clock or time-zone change, or app update: recompute the reminder (FR-6.5). */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        runAsync(context, "Reschedule") { it.useCases.syncReminderWithToday.execute() }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
