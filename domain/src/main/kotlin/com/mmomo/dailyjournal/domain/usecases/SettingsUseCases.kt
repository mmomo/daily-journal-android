package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.policies.ReminderPlanner
import com.mmomo.dailyjournal.domain.policies.StreakCalculator
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.ports.ReminderScheduler
import com.mmomo.dailyjournal.domain.ports.SettingsStore
import com.mmomo.dailyjournal.domain.ports.TimeSource

class GetSettingsUseCase internal constructor(private val settings: SettingsStore) {
    suspend fun execute(): AppSettings = withStorage { settings.load() }
}

/** US-1.4, BR-G2, BR-G3. Today's already-chosen prompt is unaffected (BR-P4). */
class UpdatePromptCategoriesUseCase internal constructor(
    private val settings: SettingsStore,
    private val afterWrite: AfterWrite,
) {
    suspend fun execute(categories: Set<PromptCategory>) {
        if (categories.isEmpty()) throw JournalError.CategoriesEmpty()
        val current = withStorage { settings.load() }
        withStorage { settings.save(current.copy(categories = categories)) }
        afterWrite.refreshWidget()
    }
}

/** Recomputes the rolling reminder window (use case flow 13, BR-R1–R6). Best effort; never throws. */
class SyncReminderWithTodayUseCase internal constructor(
    private val time: TimeSource,
    private val entries: EntryRepository,
    private val settings: SettingsStore,
    private val scheduler: ReminderScheduler,
    private val afterWrite: AfterWrite,
) {
    suspend fun execute() = afterWrite.bestEffort {
        val reminder = settings.load().reminder
        val isAllowed = reminder.isEnabled && scheduler.authorization() == ReminderAuthorization.ALLOWED
        val streak = if (isAllowed) {
            StreakCalculator.summary(entries.entryDays(), time.now, time.timeZone)
        } else {
            StreakSummary.EMPTY
        }
        scheduler.replacePending(ReminderPlanner.plan(reminder, isAllowed, streak, time.now, time.timeZone))
    }
}

/**
 * Turns the reminder on or off or changes its time (use case flow 12, BR-R7). The App unit shows the
 * permission dialog first on Android 13+; if notifications still aren't allowed, the reminder is saved
 * as off and [JournalError.NotificationsDenied] is thrown.
 */
class ConfigureReminderUseCase internal constructor(
    private val settings: SettingsStore,
    private val scheduler: ReminderScheduler,
    private val syncReminder: SyncReminderWithTodayUseCase,
) {
    suspend fun execute(reminder: ReminderSettings) {
        val current = withStorage { settings.load() }
        if (reminder.isEnabled && withStorage { scheduler.authorization() } != ReminderAuthorization.ALLOWED) {
            withStorage { settings.save(current.copy(reminder = reminder.copy(isEnabled = false))) }
            syncReminder.execute()
            throw JournalError.NotificationsDenied()
        }
        withStorage { settings.save(current.copy(reminder = reminder)) }
        syncReminder.execute()
    }
}

/** Finishes first-launch onboarding (use case flow 14, BR-G4). */
class CompleteOnboardingUseCase internal constructor(
    private val settings: SettingsStore,
    private val configureReminder: ConfigureReminderUseCase,
    private val afterWrite: AfterWrite,
) {
    /** Completes even if notifications are denied; that case is reported by throwing *after* saving. */
    suspend fun execute(categories: Set<PromptCategory>, reminder: ReminderSettings?) {
        if (categories.isEmpty()) throw JournalError.CategoriesEmpty()
        withStorage { settings.save(settings.load().copy(categories = categories)) }
        var denied = false
        if (reminder != null && reminder.isEnabled) {
            try {
                configureReminder.execute(reminder)
            } catch (_: JournalError.NotificationsDenied) {
                denied = true
            }
        }
        withStorage { settings.save(settings.load().copy(onboardingCompleted = true)) }
        afterWrite.refreshWidget()
        if (denied) throw JournalError.NotificationsDenied()
    }
}
