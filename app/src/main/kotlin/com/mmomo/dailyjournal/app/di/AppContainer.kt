package com.mmomo.dailyjournal.app.di

import android.content.Context
import android.util.Log
import com.mmomo.dailyjournal.app.JournalApplication
import com.mmomo.dailyjournal.app.MainActivity
import com.mmomo.dailyjournal.app.reminders.ReminderReceiver
import com.mmomo.dailyjournal.data.composition.JournalEnvironment
import com.mmomo.dailyjournal.data.composition.JournalEnvironmentFactory
import com.mmomo.dailyjournal.data.reminders.AndroidReminderScheduler
import com.mmomo.dailyjournal.data.system.SystemTimeSource
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.ports.WidgetRefresher
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Until the Widget unit, widget refreshes do nothing (unit-of-work: Unit 3). */
object NoopWidgetRefresher : WidgetRefresher {
    override suspend fun reloadAll() = Unit
}

/**
 * Everything screens need, built once per process (application design Q4: manual injection).
 * The constructor takes plain values so tests can build it over fakes.
 */
class AppContainer(
    val useCases: JournalUseCases,
    val time: TimeSource,
    val refreshSignals: Flow<Unit>,
    val clockBoundaries: Flow<Unit>,
    val authorization: suspend () -> ReminderAuthorization,
    val markPermissionAsked: suspend () -> Unit,
    val environment: JournalEnvironment,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val environment = JournalEnvironmentFactory.create(
                context = context,
                widgets = NoopWidgetRefresher,
                onBackgroundFailure = { Log.w(JournalApplication.TAG, "Background update failed: ${it::class.java.name}") },
                reminderReceiver = ReminderReceiver::class.java,
                mainActivity = MainActivity::class.java,
            )
            // Stateless; used to read the notification permission state for Settings and onboarding.
            val scheduler = AndroidReminderScheduler(context, environment.notificationPermission, ReminderReceiver::class.java)
            return AppContainer(
                useCases = environment.useCases,
                time = SystemTimeSource(),
                refreshSignals = environment.refreshSignals,
                clockBoundaries = environment.clockEvents?.boundaries ?: emptyFlow(),
                authorization = { scheduler.authorization() },
                markPermissionAsked = { environment.notificationPermission.markAsked() },
                environment = environment,
            )
        }
    }
}
