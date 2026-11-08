package com.mmomo.dailyjournal.app.property

import androidx.lifecycle.SavedStateHandle
import com.mmomo.dailyjournal.app.support.signal
import com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private sealed interface Op
private data class Type(val text: String) : Op
private data class Pause(val ms: Long) : Op
private data object Done : Op
private data object Back : Op
private data object Midnight : Op

/**
 * PBT-06: random sequences of typing, pauses, Done, Back and one midnight, run against the editor
 * ViewModel and a simple model. After every step the stored entry, the visible text and the lock
 * state must match the model.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorStatefulPropertyTest : FunSpec({
    val ops: Arb<Op> = Arb.choice(
        Arb.element("", "  ", "\n", "a", "hello", "hello world", "Gracias, día").map { Type(it) },
        Arb.int(0..1_500).map { Pause(it.toLong()) },
        Arb.constant(Done),
        Arb.constant(Back),
        Arb.constant(Midnight),
    )

    /** What the editor should be doing. */
    class Model {
        var typed = ""
        var lastSaved = ""
        var stored: String? = null
        var sinceType = 0L
        var autosavePending = false
        var locked = false
        var finishes = 0

        fun save() {
            lastSaved = typed
            stored = typed
            autosavePending = false
        }
    }

    test("the editor matches the model for any sequence of events") {
        checkAll(200, Arb.list(ops, 1..30)) { sequence ->
            runTest {
                Dispatchers.setMain(StandardTestDispatcher(testScheduler))
                try {
                    val env = TestEnvironment(now = instant("2026-10-08 23:00"))
                    val boundaries = signal()
                    val today = day("2026-10-08")
                    val vm = EntryEditorViewModel(env.useCases, env.time, boundaries, today, "", SavedStateHandle())
                    runCurrent()
                    val model = Model()
                    var midnightPassed = false

                    for (op in sequence) {
                        when (op) {
                            is Type -> {
                                vm.onTextChange(op.text)
                                if (!model.locked) {
                                    model.typed = op.text
                                    model.sinceType = 0
                                    model.autosavePending = JournalEntry.isValidText(op.text)
                                }
                            }
                            is Pause -> {
                                advanceTimeBy(op.ms)
                                runCurrent()
                                model.sinceType += op.ms
                                if (model.autosavePending && !model.locked && model.sinceType >= 800) model.save()
                            }
                            Done -> {
                                vm.done { model.finishes++ }
                                runCurrent()
                            }
                            Back -> {
                                vm.leave { }
                                runCurrent()
                            }
                            Midnight -> if (!midnightPassed) {
                                midnightPassed = true
                                env.time.now = instant("2026-10-09 00:00:02")
                                boundaries.emit(Unit)
                                runCurrent()
                            }
                        }
                        // Model transitions for the actions whose effect depends on the state before them.
                        when (op) {
                            Done -> if (!model.locked && JournalEntry.isValidText(model.typed)) model.save()
                            Back -> if (!model.locked && model.typed != model.lastSaved && JournalEntry.isValidText(model.typed)) model.save()
                            Midnight -> if (!model.locked) {
                                if (model.typed != model.lastSaved && JournalEntry.isValidText(model.typed)) model.save()
                                model.locked = true
                                model.autosavePending = false
                            }
                            else -> Unit
                        }

                        env.entries.stored[today]?.text shouldBe model.stored
                        vm.state.value.text shouldBe model.typed
                        vm.state.value.isLocked shouldBe model.locked
                    }
                } finally {
                    advanceUntilIdle()
                    Dispatchers.resetMain()
                }
            }
        }
    }
})
