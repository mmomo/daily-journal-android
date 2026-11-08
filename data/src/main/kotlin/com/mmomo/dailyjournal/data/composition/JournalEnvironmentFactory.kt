package com.mmomo.dailyjournal.data.composition

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mmomo.dailyjournal.data.database.DatabaseEntryRepository
import com.mmomo.dailyjournal.data.database.JournalDatabase
import com.mmomo.dailyjournal.data.prompts.BundledPromptLibrary
import com.mmomo.dailyjournal.data.reminders.AndroidReminderScheduler
import com.mmomo.dailyjournal.data.reminders.ReminderNotifier
import com.mmomo.dailyjournal.data.stores.JournalPreferences
import com.mmomo.dailyjournal.data.stores.NotificationPermissionFlag
import com.mmomo.dailyjournal.data.stores.PreferencesPromptStateStore
import com.mmomo.dailyjournal.data.stores.PreferencesSettingsStore
import com.mmomo.dailyjournal.data.system.ClockEvents
import com.mmomo.dailyjournal.data.system.SystemTimeSource
import com.mmomo.dailyjournal.domain.ports.ReminderScheduler
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.ports.WidgetRefresher
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Composition root: wires the domain ports to the Android implementations (DATA-W1, DATA-W2). */
object JournalEnvironmentFactory {

    /**
     * Production wiring. Call once per process, off the main thread (it opens storage and loads prompts).
     * Throws if the bundled prompt file is invalid (the App shows its startup error).
     */
    fun create(
        context: Context,
        widgets: WidgetRefresher,
        onBackgroundFailure: (Throwable) -> Unit,
        reminderReceiver: Class<out BroadcastReceiver>,
        mainActivity: Class<out Activity>,
    ): JournalEnvironment {
        val app = context.applicationContext
        val database = JournalDatabase.open(app)
        val preferences = JournalPreferences.dataStore(app)
        val time = SystemTimeSource()
        val permission = NotificationPermissionFlag(preferences)
        return wire(
            database = database,
            preferences = preferences,
            library = BundledPromptLibrary(app),
            time = time,
            reminders = AndroidReminderScheduler(app, permission, reminderReceiver),
            widgets = widgets,
            onBackgroundFailure = onBackgroundFailure,
            clockEvents = ClockEvents(app, time),
            notifier = ReminderNotifier(app, mainActivity),
            permission = permission,
            io = Dispatchers.IO,
        )
    }

    /** Same wiring over an in-memory database and a given DataStore, for tests and previews. */
    fun createInMemory(
        context: Context,
        preferences: DataStore<Preferences>,
        time: TimeSource,
        reminders: ReminderScheduler,
        widgets: WidgetRefresher,
        library: BundledPromptLibrary = BundledPromptLibrary(context),
        io: CoroutineDispatcher = Dispatchers.IO,
        onBackgroundFailure: (Throwable) -> Unit = {},
    ): JournalEnvironment = wire(
        database = JournalDatabase.inMemory(context),
        preferences = preferences,
        library = library,
        time = time,
        reminders = reminders,
        widgets = widgets,
        onBackgroundFailure = onBackgroundFailure,
        clockEvents = null,
        notifier = null,
        permission = NotificationPermissionFlag(preferences),
        io = io,
    )

    private fun wire(
        database: JournalDatabase,
        preferences: DataStore<Preferences>,
        library: BundledPromptLibrary,
        time: TimeSource,
        reminders: ReminderScheduler,
        widgets: WidgetRefresher,
        onBackgroundFailure: (Throwable) -> Unit,
        clockEvents: ClockEvents?,
        notifier: ReminderNotifier?,
        permission: NotificationPermissionFlag,
        io: CoroutineDispatcher,
    ): JournalEnvironment {
        val entries = DatabaseEntryRepository(database, io)
        val settings = PreferencesSettingsStore(preferences, io)
        val promptState = PreferencesPromptStateStore(preferences, io)
        val useCases = JournalUseCases(time, entries, library, promptState, settings, reminders, widgets, onBackgroundFailure)
        return JournalEnvironment(
            useCases = useCases,
            clockEvents = clockEvents,
            entryChanges = entries.changes,
            settingsChanges = settings.changes,
            promptStateChanges = promptState.changes,
            reminderNotifier = notifier,
            notificationPermission = permission,
        )
    }
}
