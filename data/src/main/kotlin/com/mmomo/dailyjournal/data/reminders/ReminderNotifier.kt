package com.mmomo.dailyjournal.data.reminders

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder

/**
 * Shows a reminder notification (DATA-R4): "Daily reminder" channel, one notification per day, and a
 * tap that opens [activityClass] through an explicit, immutable intent carrying the deep link.
 */
class ReminderNotifier(
    private val context: Context,
    private val activityClass: Class<out Activity>,
    private val smallIcon: Int = android.R.drawable.ic_popup_reminder,
) {
    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "One reminder a day to write your journal entry" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Posts [reminder]; does nothing if notifications aren't allowed. */
    fun post(reminder: ScheduledReminder) {
        if (!canPost()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setAutoCancel(true)
            .setContentIntent(tapIntent(reminder))
            .build()
        @Suppress("MissingPermission") // checked in canPost()
        NotificationManagerCompat.from(context).notify(notificationId(reminder), notification)
    }

    internal fun tapIntent(reminder: ScheduledReminder): PendingIntent {
        val intent = Intent(context, activityClass)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(reminder.deepLink.toUri()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, notificationId(reminder), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"

        /** One notification per day; a newer one for the same day replaces the older. */
        fun notificationId(reminder: ScheduledReminder): Int = reminder.day.dayNumber
    }
}
