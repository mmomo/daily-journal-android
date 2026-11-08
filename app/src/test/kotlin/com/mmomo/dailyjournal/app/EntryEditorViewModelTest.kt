package com.mmomo.dailyjournal.app

import androidx.lifecycle.SavedStateHandle
import com.mmomo.dailyjournal.app.support.signal
import com.mmomo.dailyjournal.app.support.vmTest
import com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel
import com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel.Status
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.ports.EntryRepository
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.yield
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorViewModelTest : FunSpec({
    val today = day("2026-10-08")

    fun editor(env: TestEnvironment, boundaries: MutableSharedFlow<Unit> = signal(), saved: SavedStateHandle = SavedStateHandle(), text: String = "") =
        EntryEditorViewModel(env.useCases, env.time, boundaries, today, text, saved)

    test("autosave runs 0.8 s after the last change, not before") {
        vmTest {
            val env = TestEnvironment()
            val vm = editor(env)
            vm.onTextChange("First")
            advanceTimeBy(500)
            vm.onTextChange("First thoughts")
            advanceTimeBy(700)
            runCurrent()
            env.entries.stored[today].shouldBeNull()
            advanceTimeBy(101)
            runCurrent()
            env.entries.stored[today]!!.text shouldBe "First thoughts"
            vm.state.value.status shouldBe Status.SAVED
        }
    }

    test("autosave completes when storage really suspends (regression: the autosave job cancelled itself)") {
        vmTest {
            val env = TestEnvironment()
            val suspending = object : EntryRepository by env.entries {
                override suspend fun save(entry: JournalEntry) {
                    yield()
                    env.entries.save(entry)
                }
            }
            val useCases = JournalUseCases(env.time, suspending, env.library, env.promptState, env.settings, env.reminders, env.widgets) {}
            val vm = EntryEditorViewModel(useCases, env.time, signal(), today, "", SavedStateHandle())
            vm.onTextChange("Typed and paused")
            advanceUntilIdle()
            env.entries.stored[today]!!.text shouldBe "Typed and paused"
            vm.state.value.status shouldBe Status.SAVED
        }
    }

    test("Done saves immediately and finishes; with blank text it does nothing") {
        vmTest {
            val env = TestEnvironment()
            val vm = editor(env)
            var finished = 0
            vm.onTextChange("   ")
            vm.done { finished++ }
            runCurrent()
            finished shouldBe 0
            vm.state.value.canFinish shouldBe false
            vm.onTextChange("Done now")
            vm.done { finished++ }
            runCurrent()
            finished shouldBe 1
            env.entries.stored[today]!!.text shouldBe "Done now"
        }
    }

    test("Back saves unsaved text before leaving") {
        vmTest {
            val env = TestEnvironment()
            val vm = editor(env)
            var left = false
            vm.onTextChange("Leaving soon")
            vm.leave { left = true }
            runCurrent()
            left shouldBe true
            env.entries.stored[today]!!.text shouldBe "Leaving soon"
        }
    }

    test("a failed save keeps the text, shows an error, and Back stays") {
        vmTest {
            val env = TestEnvironment()
            env.entries.failing = true
            val vm = editor(env)
            var left = false
            vm.onTextChange("Precious words")
            vm.leave { left = true }
            runCurrent()
            left shouldBe false
            vm.state.value.text shouldBe "Precious words"
            vm.state.value.status shouldBe Status.ERROR
            vm.state.value.message shouldBe "Couldn't save. Try again."
        }
    }

    test("text over 20,000 characters is rejected and the text is unchanged") {
        vmTest {
            val env = TestEnvironment()
            val vm = editor(env)
            vm.onTextChange("short") shouldBe true
            vm.onTextChange("x".repeat(JournalEntry.MAX_LENGTH + 1)) shouldBe false
            vm.state.value.text shouldBe "short"
            vm.state.value.message shouldBe EntryEditorViewModel.MESSAGE_LIMIT
            vm.onTextChange("x".repeat(JournalEntry.MAX_LENGTH)) shouldBe true
        }
    }

    test("at midnight pending text is saved to its own day, then the editor locks") {
        vmTest {
            val env = TestEnvironment(now = instant("2026-10-08 23:59:58"))
            val boundaries = signal()
            val vm = editor(env, boundaries)
            runCurrent()
            vm.onTextChange("Just before midnight")
            env.time.now = instant("2026-10-09 00:00:02")
            boundaries.emit(Unit)
            runCurrent()
            env.entries.stored[today]!!.text shouldBe "Just before midnight"
            vm.state.value.isLocked shouldBe true
            vm.state.value.message shouldBe EntryEditorViewModel.MESSAGE_LOCKED
            vm.onTextChange("more") shouldBe false
        }
    }

    test("unsaved text survives process death through the saved state") {
        vmTest {
            val env = TestEnvironment()
            val saved = SavedStateHandle()
            editor(env, saved = saved).onTextChange("Not saved yet")
            val restored = editor(env, saved = saved, text = "")
            restored.state.value.text shouldBe "Not saved yet"
            restored.isDirty shouldBe true
            advanceUntilIdle()
        }
    }
})
