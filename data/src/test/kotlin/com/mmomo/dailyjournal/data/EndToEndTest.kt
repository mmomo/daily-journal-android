package com.mmomo.dailyjournal.data

import com.mmomo.dailyjournal.data.composition.JournalEnvironmentFactory
import com.mmomo.dailyjournal.data.support.TempPreferences
import com.mmomo.dailyjournal.data.support.appContext
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.support.FakeReminderScheduler
import com.mmomo.dailyjournal.domain.support.FakeWidgetRefresher
import com.mmomo.dailyjournal.domain.support.FixedTimeSource
import com.mmomo.dailyjournal.domain.support.IosParityFixtures
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The domain use cases on real Room and DataStore storage with the real prompt file (unit exit criteria). */
@RunWith(RobolectricTestRunner::class)
class EndToEndTest {
    private val prefs = TempPreferences()
    private val time = FixedTimeSource(instant("2026-10-08 09:00"))
    private val widgets = FakeWidgetRefresher()
    private val reminders = FakeReminderScheduler()
    private val env = JournalEnvironmentFactory.createInMemory(appContext(), prefs.store, time, reminders, widgets, io = Dispatchers.IO)
    private val useCases = env.useCases

    @After fun tearDown() = prefs.close()

    @Test
    fun `daily flow - onboarding, prompt like iOS, refresh, save, streak, search, export`() = runBlocking<Unit> {
        useCases.completeOnboarding.execute(PromptCategory.entries.toSet(), reminder = null)
        val today = useCases.getTodayState.execute()
        today.prompt.id shouldBe IosParityFixtures.DAILY_ALL_2026_10_08
        useCases.refreshPrompt.execute().id shouldBe IosParityFixtures.refreshesAll20261008.first()

        useCases.saveTodayEntry.execute("A quiet morning at the café.", day("2026-10-08"), time.now)
        useCases.getTodayState.execute().isTodayDone shouldBe true
        useCases.getStreak.execute().current shouldBe 1
        useCases.searchEntries.execute("cafe").map { it.day } shouldBe listOf(day("2026-10-08"))
        widgets.reloads shouldBe 3 // onboarding, refresh, save

        time.now = instant("2026-10-09 09:00")
        useCases.getTodayState.execute().streak.current shouldBe 1
        useCases.saveTodayEntry.execute("Second day.", day("2026-10-09"), time.now)
        useCases.getStreak.execute().current shouldBe 2
        useCases.exportJournal.execute(com.mmomo.dailyjournal.domain.entities.ExportFormat.MARKDOWN).contents.contains("A quiet morning at the café.") shouldBe true
    }

    @Test
    fun `settings and reminders persist through storage`() = runBlocking<Unit> {
        useCases.configureReminder.execute(ReminderSettings(isEnabled = true, hour = 7, minute = 30))
        useCases.getSettings.execute().reminder shouldBe ReminderSettings(true, 7, 30)
        reminders.pending.first().fireAt shouldBe instant("2026-10-09 07:30")
        useCases.updatePromptCategories.execute(setOf(PromptCategory.GOALS))
        useCases.getSettings.execute() shouldBe AppSettings(setOf(PromptCategory.GOALS), ReminderSettings(true, 7, 30), false)
    }

    @Test
    fun `backfill before noon restores an at-risk streak on real storage (US-4_2)`() = runBlocking<Unit> {
        time.now = instant("2026-10-05 21:00")
        useCases.saveTodayEntry.execute("One", day("2026-10-05"), time.now)
        time.now = instant("2026-10-06 21:00")
        useCases.saveTodayEntry.execute("Two", day("2026-10-06"), time.now)
        time.now = instant("2026-10-08 09:00")
        useCases.getStreak.execute().current shouldBe 2
        useCases.backfillYesterday.execute("Late", day("2026-10-07"))
        useCases.getStreak.execute().current shouldBe 3
    }
}
