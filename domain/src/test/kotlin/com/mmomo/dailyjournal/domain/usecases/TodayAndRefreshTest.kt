package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TodayAndRefreshTest : FunSpec({
    test("Today shows a prompt from the library with refreshes and the editor open (US-1.1, US-2.5)") {
        val env = TestEnvironment()
        val state = env.useCases.getTodayState.execute()
        state.day shouldBe day("2026-10-08")
        env.library.prompt(state.prompt.id).shouldNotBeNull()
        state.refreshesRemaining shouldBe 3
        state.entryEditability.shouldBeInstanceOf<EntryEditability.Editable>()
        state.entry.shouldBeNull()
        state.onboardingCompleted shouldBe true
    }

    test("same prompt on relaunch and in the widget; new day, new prompt (US-1.1, US-1.2)") {
        val env = TestEnvironment()
        val first = env.useCases.getTodayState.execute().prompt
        env.time.now = instant("2026-10-08 21:00")
        env.useCases.getTodayState.execute().prompt shouldBe first
        env.time.now = instant("2026-10-09 00:01")
        env.useCases.getTodayState.execute().prompt shouldNotBe first
    }

    test("changing categories keeps today's prompt (US-1.4, BR-P4)") {
        val env = TestEnvironment()
        val first = env.useCases.getTodayState.execute().prompt
        env.useCases.updatePromptCategories.execute(setOf(PromptCategory.GOALS))
        env.useCases.getTodayState.execute().prompt shouldBe first
    }

    test("refresh shows a different prompt, persists, and counts down (US-1.3)") {
        val env = TestEnvironment()
        val first = env.useCases.getTodayState.execute().prompt
        val next = env.useCases.refreshPrompt.execute()
        next shouldNotBe first
        val state = env.useCases.getTodayState.execute()
        state.prompt shouldBe next
        state.refreshesRemaining shouldBe 2
        env.widgets.reloads shouldBe 1
    }

    test("limit of three, then reset at midnight (US-1.3)") {
        val env = TestEnvironment()
        repeat(3) { env.useCases.refreshPrompt.execute() }
        env.useCases.getTodayState.execute().refreshesRemaining shouldBe 0
        shouldThrow<JournalError.NoRefreshesLeft> { env.useCases.refreshPrompt.execute() }
        env.time.now = instant("2026-10-09 00:00")
        env.useCases.getTodayState.execute().refreshesRemaining shouldBe 3
    }

    test("refresh uses the current categories (US-1.4)") {
        val env = TestEnvironment()
        env.useCases.getTodayState.execute()
        env.useCases.updatePromptCategories.execute(setOf(PromptCategory.CREATIVITY))
        env.useCases.refreshPrompt.execute().category shouldBe PromptCategory.CREATIVITY
    }

    test("no refresh after saving; the saved prompt is shown (US-1.3, FR-1.6)") {
        val env = TestEnvironment()
        val prompt = env.useCases.getTodayState.execute().prompt
        env.useCases.saveTodayEntry.execute("Done for today.", day("2026-10-08"), env.time.now)
        shouldThrow<JournalError.EntryLocked> { env.useCases.refreshPrompt.execute() }
        val state = env.useCases.getTodayState.execute()
        state.prompt shouldBe prompt
        state.refreshesRemaining shouldBe 0
    }

    test("moving the clock to another date and back keeps each date's refresh count (US-1.3 misuse, BR-SEC2)") {
        val env = TestEnvironment()
        repeat(3) { env.useCases.refreshPrompt.execute() }
        env.time.now = instant("2026-10-09 09:00")
        env.useCases.refreshPrompt.execute()
        env.time.now = instant("2026-10-08 09:30")
        env.useCases.getTodayState.execute().refreshesRemaining shouldBe 0
    }

    test("backfill offer before 12:00 when yesterday is missing; none after or if it exists (US-2.3)") {
        val env = TestEnvironment(now = instant("2026-10-09 09:00"))
        val offer = env.useCases.getTodayState.execute().backfillOffer.shouldNotBeNull()
        offer.day shouldBe day("2026-10-08")
        offer.until shouldBe instant("2026-10-09 12:00")
        env.time.now = instant("2026-10-09 12:00")
        env.useCases.getTodayState.execute().backfillOffer.shouldBeNull()
        val withYesterday = TestEnvironment(now = instant("2026-10-09 09:00")).withEntries("2026-10-08")
        withYesterday.useCases.getTodayState.execute().backfillOffer.shouldBeNull()
    }

    test("a future-dated entry is shown read-only and today counts as done (BR-E7)") {
        val env = TestEnvironment(now = instant("2026-10-08 21:00")).withEntries("2026-10-09")
        val state = env.useCases.getTodayState.execute()
        state.entry?.day shouldBe day("2026-10-09")
        state.entryEditability shouldBe EntryEditability.Locked
        state.isTodayDone shouldBe true
    }

    test("reports onboarding state for the widget placeholder (BR-G5)") {
        TestEnvironment(settings = AppSettings()).useCases.getTodayState.execute().onboardingCompleted shouldBe false
    }

    test("storage failures surface as StorageFailure without journal text (US-2.1, BR-SEC5)") {
        val env = TestEnvironment()
        env.entries.failing = true
        val error = shouldThrow<JournalError.StorageFailure> { env.useCases.getTodayState.execute() }
        error.reason shouldBe "java.io.IOException"
    }
})
