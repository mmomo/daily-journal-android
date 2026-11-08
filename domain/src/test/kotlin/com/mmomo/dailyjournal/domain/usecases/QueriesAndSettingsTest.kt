package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.TimeZone

class QueriesAndSettingsTest : FunSpec({
    test("past entries are read-only, today's is editable (US-2.4)") {
        val env = TestEnvironment(now = instant("2026-10-08 09:00")).withEntries("2026-10-05", "2026-10-08")
        env.useCases.getEntry.execute(day("2026-10-05"))?.editability shouldBe EntryEditability.Locked
        env.useCases.getEntry.execute(day("2026-10-08"))!!.editability.shouldBeInstanceOf<EntryEditability.Editable>()
        env.useCases.getEntry.execute(day("2026-10-01")).shouldBeNull()
    }

    test("history is newest first and stays on its dates after a time-zone change (US-3.1)") {
        val env = TestEnvironment().withEntries("2026-10-01", "2026-10-08", "2026-10-05")
        env.useCases.listEntries.execute().map { it.day.key } shouldBe listOf("2026-10-08", "2026-10-05", "2026-10-01")
        env.time.timeZone = TimeZone.of("Asia/Tokyo")
        env.useCases.listEntries.execute().map { it.day.key } shouldBe listOf("2026-10-08", "2026-10-05", "2026-10-01")
    }

    test("search: text, prompt, accents, empty and no match (US-3.2)") {
        val env = TestEnvironment()
        env.entries.put(env.entry("2026-10-01", text = "A day at the beach"), env.entry("2026-10-02", text = "Café con leche"))
        env.useCases.searchEntries.execute("beach").map { it.day.key } shouldBe listOf("2026-10-01")
        env.useCases.searchEntries.execute("cafe").map { it.day.key } shouldBe listOf("2026-10-02")
        env.useCases.searchEntries.execute("long enough").size shouldBe 2
        env.useCases.searchEntries.execute("  ").size shouldBe 2
        env.useCases.searchEntries.execute("zebra").shouldBeEmpty()
    }

    test("export and nothing to export (US-3.3)") {
        shouldThrow<JournalError.NothingToExport> { TestEnvironment().useCases.exportJournal.execute(ExportFormat.MARKDOWN) }
        val env = TestEnvironment().withEntries("2026-10-01")
        env.useCases.exportJournal.execute(ExportFormat.MARKDOWN).fileName shouldBe "daily-journal-2026-10-08.md"
    }

    test("categories: save, at least one, widget refreshed (US-1.4, BR-G2)") {
        val env = TestEnvironment()
        env.useCases.updatePromptCategories.execute(setOf(PromptCategory.GRATITUDE))
        env.settings.settings.categories shouldBe setOf(PromptCategory.GRATITUDE)
        env.widgets.reloads shouldBe 1
        shouldThrow<JournalError.CategoriesEmpty> { env.useCases.updatePromptCategories.execute(emptySet()) }
    }

    test("enabling the reminder schedules at 20:00 when allowed (US-6.1)") {
        val env = TestEnvironment()
        env.useCases.configureReminder.execute(ReminderSettings(isEnabled = true))
        env.settings.settings.reminder.isEnabled shouldBe true
        env.reminders.pending.first().fireAt shouldBe instant("2026-10-08 20:00")
    }

    test("denied permission saves the reminder off and reports it (US-6.1, BR-R7)") {
        val env = TestEnvironment()
        env.reminders.authorization = ReminderAuthorization.DENIED
        shouldThrow<JournalError.NotificationsDenied> { env.useCases.configureReminder.execute(ReminderSettings(isEnabled = true)) }
        env.settings.settings.reminder.isEnabled shouldBe false
        env.reminders.pending.shouldBeEmpty()
    }

    test("changing the time and turning off (US-6.1)") {
        val env = TestEnvironment()
        env.useCases.configureReminder.execute(ReminderSettings(isEnabled = true, hour = 7, minute = 30))
        env.reminders.pending.first().fireAt shouldBe instant("2026-10-09 07:30")
        env.useCases.configureReminder.execute(ReminderSettings(isEnabled = false, hour = 7, minute = 30))
        env.reminders.pending.shouldBeEmpty()
    }

    test("saving today skips today's reminder; backfill doesn't silence today (US-6.2)") {
        val env = TestEnvironment(now = instant("2026-10-09 09:00"))
        env.useCases.configureReminder.execute(ReminderSettings(isEnabled = true))
        env.useCases.backfillYesterday.execute("Yesterday.", day("2026-10-08"))
        env.reminders.pending.first().day shouldBe day("2026-10-09")
        env.useCases.saveTodayEntry.execute("Today.", day("2026-10-09"), env.time.now)
        env.reminders.pending.first().day shouldBe day("2026-10-10")
    }

    test("onboarding saves categories and completes; no reminder means no permission check (US-7.1)") {
        val env = TestEnvironment(settings = AppSettings())
        env.useCases.completeOnboarding.execute(setOf(PromptCategory.GOALS), reminder = null)
        env.settings.settings.onboardingCompleted shouldBe true
        env.settings.settings.categories shouldBe setOf(PromptCategory.GOALS)
        env.reminders.pending.shouldBeEmpty()
        shouldThrow<JournalError.CategoriesEmpty> { env.useCases.completeOnboarding.execute(emptySet(), null) }
    }

    test("onboarding completes even when notifications are denied (US-7.1, BR-G4)") {
        val env = TestEnvironment(settings = AppSettings())
        env.reminders.authorization = ReminderAuthorization.DENIED
        shouldThrow<JournalError.NotificationsDenied> {
            env.useCases.completeOnboarding.execute(setOf(PromptCategory.GOALS), ReminderSettings(isEnabled = true))
        }
        env.settings.settings.onboardingCompleted shouldBe true
        env.settings.settings.reminder.isEnabled shouldBe false
    }

    test("get settings returns what was saved (US-7.2)") {
        val env = TestEnvironment()
        env.useCases.updatePromptCategories.execute(setOf(PromptCategory.REFLECTION))
        env.useCases.getSettings.execute().categories shouldBe setOf(PromptCategory.REFLECTION)
        env.time.timeZone shouldBe mexicoCity
    }
})
