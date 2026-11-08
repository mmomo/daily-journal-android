package com.mmomo.dailyjournal.data.composition

import com.mmomo.dailyjournal.data.reminders.ReminderNotifier
import com.mmomo.dailyjournal.data.stores.NotificationPermissionFlag
import com.mmomo.dailyjournal.data.system.ClockEvents
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/** Everything a process needs: the use cases plus change and clock signals. One per process. */
class JournalEnvironment(
    val useCases: JournalUseCases,
    val clockEvents: ClockEvents?,
    val entryChanges: Flow<Unit>,
    val settingsChanges: Flow<Unit>,
    val promptStateChanges: Flow<Unit>,
    val reminderNotifier: ReminderNotifier?,
    val notificationPermission: NotificationPermissionFlag,
) {
    /** Any data change or day boundary: screens re-run their use cases when this emits. */
    val refreshSignals: Flow<Unit>
        get() = listOfNotNull(entryChanges, settingsChanges, promptStateChanges, clockEvents?.boundaries).merge()
}

/**
 * Implemented by the Application, so the widget and receivers reach the same environment without
 * depending on `:app`. The environment is created on a background thread at process start, so callers
 * wait for it (App NFR design §1); it throws if creation failed.
 */
interface JournalEnvironmentProvider {
    suspend fun awaitEnvironment(): JournalEnvironment
}
