package com.mmomo.dailyjournal.data

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mmomo.dailyjournal.data.reminders.AndroidReminderScheduler
import com.mmomo.dailyjournal.data.reminders.ReminderIntents
import com.mmomo.dailyjournal.data.reminders.ReminderNotifier
import com.mmomo.dailyjournal.data.stores.NotificationPermissionFlag
import com.mmomo.dailyjournal.data.support.TempPreferences
import com.mmomo.dailyjournal.data.support.TestMainActivity
import com.mmomo.dailyjournal.data.support.TestReminderReceiver
import com.mmomo.dailyjournal.data.support.appContext
import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ScheduledReminder
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** US-6.1, US-6.2, US-6.3; DATA-R1, R2, R4; DATA-SEC-3, SEC-4. */
@RunWith(RobolectricTestRunner::class)
class RemindersTest {
    private val prefs = TempPreferences()
    private val context = appContext()
    private val flag = NotificationPermissionFlag(prefs.store)
    private val scheduler = AndroidReminderScheduler(context, flag, TestReminderReceiver::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    @After fun tearDown() = prefs.close()

    private fun reminder(dayKey: String, at: String) = ScheduledReminder(
        "reminder-$dayKey", CalendarDay.fromKey(dayKey)!!, instant(at), "Time to journal",
        "Your daily prompt is waiting.", DeepLink.TODAY_EDITOR,
    )

    @Test
    @Config(sdk = [33])
    fun `Android 13+ - never asked, denied, allowed (DATA-R1, FR-6_3)`() = runBlocking<Unit> {
        scheduler.authorization() shouldBe ReminderAuthorization.NOT_DETERMINED
        flag.markAsked()
        scheduler.authorization() shouldBe ReminderAuthorization.DENIED
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        scheduler.authorization() shouldBe ReminderAuthorization.ALLOWED
        shadowOf(notifications).setNotificationsEnabled(false)
        scheduler.authorization() shouldBe ReminderAuthorization.DENIED
    }

    @Test
    @Config(sdk = [29])
    fun `Android 10 - allowed unless notifications are off for the app`() = runBlocking<Unit> {
        scheduler.authorization() shouldBe ReminderAuthorization.ALLOWED
        shadowOf(notifications).setNotificationsEnabled(false)
        scheduler.authorization() shouldBe ReminderAuthorization.DENIED
    }

    @Test
    fun `only the earliest reminder is registered, explicitly and immutably (DATA-R2, Q5 A)`() = runBlocking<Unit> {
        scheduler.replacePending(listOf(reminder("2026-10-09", "2026-10-09 20:00"), reminder("2026-10-08", "2026-10-08 20:00")))
        val scheduled = shadowOf(alarms).scheduledAlarms
        scheduled.size shouldBe 1
        scheduled.single().getTriggerAtMs() shouldBe instant("2026-10-08 20:00").toEpochMilliseconds()
        scheduled.single().isAllowWhileIdle shouldBe true
        // The registered pending intent, looked up the way Android does (explicit receiver, same action).
        val registered = PendingIntent.getBroadcast(
            context, ReminderIntents.ALARM_REQUEST_CODE,
            Intent(context, TestReminderReceiver::class.java).setAction(ReminderIntents.ACTION_REMINDER),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        val pending = shadowOf(registered)
        pending.isBroadcast shouldBe true
        pending.savedIntent.component?.className shouldBe TestReminderReceiver::class.java.name
        ReminderIntents.day(pending.savedIntent)?.key shouldBe "2026-10-08"
        pending.isImmutable shouldBe true
    }

    @Test
    fun `re-registering replaces the alarm and an empty plan cancels it (DATA-R2)`() = runBlocking<Unit> {
        scheduler.replacePending(listOf(reminder("2026-10-08", "2026-10-08 20:00")))
        scheduler.replacePending(listOf(reminder("2026-10-09", "2026-10-09 07:30")))
        shadowOf(alarms).scheduledAlarms.single().getTriggerAtMs() shouldBe instant("2026-10-09 07:30").toEpochMilliseconds()
        scheduler.replacePending(emptyList())
        shadowOf(alarms).scheduledAlarms.shouldBeEmpty()
    }

    @Test
    @Config(sdk = [33])
    fun `notification shows the reminder and opens the app's activity with the deep link (DATA-R4, US-6_3)`() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = ReminderNotifier(context, TestMainActivity::class.java)
        val r = reminder("2026-10-08", "2026-10-08 20:00")
        notifier.post(r)
        val posted = shadowOf(notifications).getNotification(ReminderNotifier.notificationId(r))
        posted.extras.getString(NotificationCompat.EXTRA_TITLE) shouldBe "Time to journal"
        posted.extras.getString(NotificationCompat.EXTRA_TEXT) shouldBe "Your daily prompt is waiting."
        posted.channelId shouldBe ReminderNotifier.CHANNEL_ID
        val tap = shadowOf(posted.contentIntent)
        tap.isActivity shouldBe true
        tap.savedIntent.component?.className shouldBe TestMainActivity::class.java.name
        tap.savedIntent.dataString shouldBe "dailyjournal://today"
        tap.isImmutable shouldBe true
    }

    @Test
    @Config(sdk = [33])
    fun `no notification without permission`() {
        ReminderNotifier(context, TestMainActivity::class.java).post(reminder("2026-10-08", "2026-10-08 20:00"))
        shadowOf(notifications).allNotifications.shouldBeEmpty()
    }
}
