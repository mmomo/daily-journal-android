package com.mmomo.dailyjournal.app

import com.mmomo.dailyjournal.app.support.signal
import com.mmomo.dailyjournal.app.support.vmTest
import com.mmomo.dailyjournal.app.ui.backfill.BackfillViewModel
import com.mmomo.dailyjournal.app.ui.entry.EntryDetailViewModel
import com.mmomo.dailyjournal.app.ui.history.HistoryViewModel
import com.mmomo.dailyjournal.app.ui.today.TodayViewModel
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest : FunSpec({
    test("loads today, refresh uses one of the refreshes, a signal reloads") {
        vmTest {
            val env = TestEnvironment()
            val signals = signal()
            val vm = TodayViewModel(env.useCases, signals)
            advanceUntilIdle()
            val first = vm.state.value.today!!
            first.day shouldBe day("2026-10-08")
            vm.refreshPrompt()
            advanceUntilIdle()
            vm.state.value.today!!.refreshesRemaining shouldBe first.refreshesRemaining - 1
            env.entries.put(env.entry("2026-10-08"))
            signals.emit(Unit)
            advanceUntilIdle()
            vm.state.value.today!!.isTodayDone shouldBe true
        }
    }

    test("after a save the editor stays until Done; focus requests only while today isn't done") {
        vmTest {
            val env = TestEnvironment()
            val vm = TodayViewModel(env.useCases, signal())
            advanceUntilIdle()
            vm.requestFocus()
            vm.state.value.focusRequests shouldBe 1
            env.entries.put(env.entry("2026-10-08"))
            vm.onEntrySaved(day("2026-10-08"))
            advanceUntilIdle()
            vm.state.value.editorDay shouldBe day("2026-10-08")
            vm.finishEditing()
            advanceUntilIdle()
            vm.state.value.editorDay shouldBe null
            vm.requestFocus()
            vm.state.value.focusRequests shouldBe 1
        }
    }

    test("the backfill sheet opens only while yesterday is offered") {
        vmTest {
            val morning = TestEnvironment(now = instant("2026-10-08 09:00"))
            val vm = TodayViewModel(morning.useCases, signal())
            advanceUntilIdle()
            vm.openBackfill()
            vm.state.value.showBackfill shouldBe true
            val afternoon = TestEnvironment(now = instant("2026-10-08 13:00"))
            val late = TodayViewModel(afternoon.useCases, signal())
            advanceUntilIdle()
            late.openBackfill()
            late.state.value.showBackfill shouldBe false
        }
    }
})

@OptIn(ExperimentalCoroutinesApi::class)
class BackfillViewModelTest : FunSpec({
    suspend fun offerFor(env: TestEnvironment) = env.useCases.getTodayState.execute().backfillOffer!!

    test("Save writes yesterday's entry and closes") {
        vmTest {
            val env = TestEnvironment()
            val vm = BackfillViewModel(env.useCases, offerFor(env))
            vm.onTextChange("Yesterday was good")
            vm.save()
            runCurrent()
            vm.state.value.closed shouldBe true
            env.entries.stored[day("2026-10-07")]!!.text shouldBe "Yesterday was good"
        }
    }

    test("Back with text asks first; Keep writing stays, Discard closes; Back without text closes") {
        vmTest {
            val env = TestEnvironment()
            val vm = BackfillViewModel(env.useCases, offerFor(env))
            vm.onTextChange("Half a thought")
            vm.onBack()
            vm.state.value.confirmDiscard shouldBe true
            vm.keepWriting()
            vm.state.value.closed shouldBe false
            vm.onBack()
            vm.discard()
            vm.state.value.closed shouldBe true
            env.entries.stored.isEmpty() shouldBe true
            val empty = BackfillViewModel(env.useCases, offerFor(env))
            empty.onBack()
            empty.state.value.closed shouldBe true
        }
    }

    test("saving after 12:00 reports the closed window") {
        vmTest {
            val env = TestEnvironment()
            val vm = BackfillViewModel(env.useCases, offerFor(env))
            vm.onTextChange("Too late")
            env.time.now = instant("2026-10-08 12:00")
            vm.save()
            runCurrent()
            vm.state.value.windowClosed shouldBe true
            vm.state.value.canSave shouldBe false
            vm.state.value.message shouldBe "Yesterday can no longer be filled in."
        }
    }
})

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest : FunSpec({
    fun env() = TestEnvironment().apply {
        entries.put(entry("2026-10-01", text = "Walked by the sea"), entry("2026-10-02", text = "Coffee with Ana"))
    }

    test("search waits 250 ms after typing, then filters") {
        vmTest {
            val vm = HistoryViewModel(env().useCases, signal())
            advanceUntilIdle()
            vm.state.value.entries shouldHaveSize 2
            vm.onQueryChange("sea")
            advanceTimeBy(100)
            runCurrent()
            vm.state.value.entries shouldHaveSize 2
            advanceTimeBy(200)
            runCurrent()
            vm.state.value.entries.single().day shouldBe day("2026-10-01")
            vm.onQueryChange("zebra")
            advanceUntilIdle()
            vm.state.value.noMatches shouldBe true
        }
    }

    test("Back clears the search first, then leaves") {
        vmTest {
            val vm = HistoryViewModel(env().useCases, signal())
            vm.onQueryChange("sea")
            vm.onBack() shouldBe true
            vm.state.value.query shouldBe ""
            vm.onBack() shouldBe false
            advanceUntilIdle()
        }
    }

    test("an empty journal shows the empty state") {
        vmTest {
            val vm = HistoryViewModel(TestEnvironment().useCases, signal())
            advanceUntilIdle()
            vm.state.value.isEmptyJournal shouldBe true
        }
    }
})

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDetailViewModelTest : FunSpec({
    test("today's entry is editable, a past one is read-only, a bad route key shows not found") {
        vmTest {
            val env = TestEnvironment().apply { entries.put(entry("2026-10-08"), entry("2026-10-01")) }
            val today = EntryDetailViewModel(env.useCases, "2026-10-08", signal())
            val past = EntryDetailViewModel(env.useCases, "2026-10-01", signal())
            val bad = EntryDetailViewModel(env.useCases, "../etc", signal())
            advanceUntilIdle()
            (today.state.value as EntryDetailViewModel.UiState.Loaded).detail.editability.isEditable shouldBe true
            (past.state.value as EntryDetailViewModel.UiState.Loaded).detail.editability.isEditable shouldBe false
            bad.state.value.shouldBeInstanceOf<EntryDetailViewModel.UiState.Missing>()
        }
    }
})
