package com.mmomo.dailyjournal.data.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mmomo.dailyjournal.data.stores.NotificationPermissionFlag
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.ports.ReminderScheduler

/**
 * `ReminderScheduler` on `AlarmManager` (DATA-R1, DATA-R2): one inexact alarm at a time, for the first
 * planned reminder (Q4:A, Q5:A). The alarm targets [receiverClass], the App unit's non-exported
 * receiver, through an explicit, immutable intent.
 */
class AndroidReminderScheduler(
    private val context: Context,
    private val permissionFlag: NotificationPermissionFlag,
    private val receiverClass: Class<out BroadcastReceiver>,
) : ReminderScheduler {

    override suspend fun authorization(): ReminderAuthorization {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return if (permissionFlag.wasAsked()) ReminderAuthorization.DENIED else ReminderAuthorization.NOT_DETERMINED
            }
        }
        return if (enabled) ReminderAuthorization.ALLOWED else ReminderAuthorization.DENIED
    }

    override suspend fun replacePending(reminders: List<ScheduledReminder>) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val next = reminders.minByOrNull { it.fireAt }
        if (next == null) {
            existingIntent()?.let { alarms.cancel(it); it.cancel() }
            return
        }
        // Same request code and explicit target, so this replaces any previously registered reminder.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.fireAt.toEpochMilliseconds(), pendingIntent(next))
    }

    private fun alarmIntent() = Intent(context, receiverClass).setAction(ReminderIntents.ACTION_REMINDER)

    private fun pendingIntent(reminder: ScheduledReminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        ReminderIntents.ALARM_REQUEST_CODE,
        ReminderIntents.withReminder(alarmIntent(), reminder),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun existingIntent(): PendingIntent? = PendingIntent.getBroadcast(
        context,
        ReminderIntents.ALARM_REQUEST_CODE,
        alarmIntent(),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
}
