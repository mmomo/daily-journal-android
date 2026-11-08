package com.mmomo.dailyjournal.domain.usecases

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SaveAndBackfillTest : FunSpec({
    test("save stores today's text with today's prompt, refreshes the widget and reminders (US-2.1)") {
        val env = TestEnvironment(now = instant("2026-10-08 21:00"))
        val prompt = env.useCases.getTodayState.execute().prompt
        val saved = env.useCases.saveTodayEntry.execute("Grateful for a quiet morning.", day("2026-10-08"), env.time.now)
        saved.day shouldBe day("2026-10-08")
        saved.promptId shouldBe prompt.id
        env.entries.stored[day("2026-10-08")] shouldBe saved
        env.widgets.reloads shouldBe 1
        env.reminders.replaceCalls shouldBe 1
        env.useCases.getTodayState.execute().isTodayDone shouldBe true
    }

    test("empty text and text over 20,000 characters are rejected; nothing is saved (US-2.1, BR-E2)") {
        val env = TestEnvironment()
        shouldThrow<JournalError.EmptyText> { env.useCases.saveTodayEntry.execute(" \n ", day("2026-10-08"), env.time.now) }
        shouldThrow<JournalError.TextTooLong> {
            env.useCases.saveTodayEntry.execute("a".repeat(JournalEntry.MAX_LENGTH + 1), day("2026-10-08"), env.time.now)
        }
        env.entries.saves shouldBe 0
    }

    test("one entry per day: a second save updates the same entry (US-2.1, US-2.2)") {
        val env = TestEnvironment(now = instant("2026-10-08 08:00"))
        val first = env.useCases.saveTodayEntry.execute("Morning.", day("2026-10-08"), env.time.now)
        env.time.now = instant("2026-10-08 22:00")
        val second = env.useCases.saveTodayEntry.execute("Morning. And evening.", day("2026-10-08"), env.time.now)
        second.id shouldBe first.id
        second.text shouldBe "Morning. And evening."
        second.createdAt shouldBe first.createdAt
        second.updatedAt shouldBe instant("2026-10-08 22:00")
        env.entries.stored.values shouldHaveSize 1
    }

    test("the prompt is frozen with the entry even after a refresh-locked day (US-2.1)") {
        val env = TestEnvironment()
        env.useCases.refreshPrompt.execute()
        val shown = env.useCases.getTodayState.execute().prompt
        env.useCases.saveTodayEntry.execute("Answer.", day("2026-10-08"), env.time.now).promptId shouldBe shown.id
    }

    test("typing across midnight: an edit just before midnight saved 2 s after is kept (US-2.2, BR-E4)") {
        val env = TestEnvironment(now = instant("2026-10-08 23:59:30"))
        env.useCases.saveTodayEntry.execute("Started.", day("2026-10-08"), env.time.now)
        env.time.now = instant("2026-10-09 00:00:02")
        val late = env.useCases.saveTodayEntry.execute("Started. Last words.", day("2026-10-08"), instant("2026-10-08 23:59:59"))
        late.text shouldBe "Started. Last words."
        late.day shouldBe day("2026-10-08")
        late.updatedAt shouldBe instant("2026-10-09 00:00:02")
    }

    test("typing across midnight: too late, or edited after midnight, is rejected (US-2.2, BR-E4)") {
        val env = TestEnvironment(now = instant("2026-10-09 00:00:06"))
        shouldThrow<JournalError.EntryLocked> {
            env.useCases.saveTodayEntry.execute("Late.", day("2026-10-08"), instant("2026-10-08 23:59:59"))
        }
        env.time.now = instant("2026-10-09 00:00:02")
        shouldThrow<JournalError.EntryLocked> {
            env.useCases.saveTodayEntry.execute("Late.", day("2026-10-08"), instant("2026-10-09 00:00:01"))
        }
        env.entries.saves shouldBe 0
    }

    test("no new entry for today while a future-dated entry exists (BR-E7)") {
        val env = TestEnvironment(now = instant("2026-10-08 21:00")).withEntries("2026-10-09")
        shouldThrow<JournalError.EntryLocked> { env.useCases.saveTodayEntry.execute("Hi.", day("2026-10-08"), env.time.now) }
    }

    test("a failed save keeps nothing and reports StorageFailure (US-2.1 save fails, NFR-6.8)") {
        val env = TestEnvironment()
        env.entries.failing = true
        shouldThrow<JournalError.StorageFailure> { env.useCases.saveTodayEntry.execute("Text.", day("2026-10-08"), env.time.now) }
        env.entries.stored.isEmpty() shouldBe true
    }

    test("a widget or reminder failure doesn't fail a successful save (DOM-REL-5)") {
        val env = TestEnvironment()
        env.widgets.failing = true
        env.reminders.failing = true
        env.useCases.saveTodayEntry.execute("Still saved.", day("2026-10-08"), env.time.now)
        env.entries.stored[day("2026-10-08")]?.text shouldBe "Still saved."
        env.backgroundFailures.size shouldBe 2
    }

    test("concurrent saves run one at a time and the last arrival wins (NFR design §3)") {
        val env = TestEnvironment()
        coroutineScope {
            (1..20).map { n -> async { env.useCases.saveTodayEntry.execute("Version $n", day("2026-10-08"), env.time.now) } }.awaitAll()
        }
        env.entries.stored.values shouldHaveSize 1
        env.entries.saves shouldBe 20
    }

    test("backfill saves yesterday with yesterday's prompt before noon (US-2.3)") {
        val env = TestEnvironment(now = instant("2026-10-09 09:00"))
        val offer = env.useCases.getTodayState.execute().backfillOffer!!
        env.time.now = instant("2026-10-09 11:30")
        val saved = env.useCases.backfillYesterday.execute("Late entry.", day("2026-10-08"))
        saved.day shouldBe day("2026-10-08")
        saved.promptId shouldBe offer.prompt.id
    }

    test("backfill: window closed, yesterday exists, or not yesterday (US-2.3)") {
        val closed = TestEnvironment(now = instant("2026-10-09 12:00"))
        shouldThrow<JournalError.BackfillNotAvailable> { closed.useCases.backfillYesterday.execute("x", day("2026-10-08")) }
        val exists = TestEnvironment(now = instant("2026-10-09 09:00")).withEntries("2026-10-08")
        shouldThrow<JournalError.BackfillNotAvailable> { exists.useCases.backfillYesterday.execute("x", day("2026-10-08")) }
        val older = TestEnvironment(now = instant("2026-10-09 09:00"))
        shouldThrow<JournalError.BackfillNotAvailable> { older.useCases.backfillYesterday.execute("x", day("2026-10-07")) }
    }

    test("backfill restores an at-risk streak (US-4.2)") {
        val env = TestEnvironment(now = instant("2026-10-09 09:00")).withEntries("2026-10-05", "2026-10-06", "2026-10-07")
        env.useCases.getStreak.execute().current shouldBe 3
        env.time.now = instant("2026-10-09 11:00")
        env.useCases.backfillYesterday.execute("Made it.", day("2026-10-08"))
        env.useCases.getStreak.execute().current shouldBe 4
    }
})
