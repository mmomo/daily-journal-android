package com.mmomo.dailyjournal.domain.property

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.support.Generators
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import com.mmomo.dailyjournal.domain.support.day
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

/** Stateful properties (PBT-06): random command sequences against the use cases and a simple model. */
class StatefulPropertyTest : FunSpec({
    test("refresh sequences: at most 3 succeed, shown prompts never repeat (model: a counter)") {
        checkAll(100, Arb.list(Arb.element("refresh", "look"), 0..12)) { commands ->
            val env = TestEnvironment()
            var refreshes = 0
            val seen = mutableListOf(env.useCases.getTodayState.execute().prompt.id)
            for (command in commands) {
                when (command) {
                    "refresh" -> try {
                        val next = env.useCases.refreshPrompt.execute()
                        refreshes++
                        (next.id in seen) shouldBe false
                        seen += next.id
                    } catch (_: JournalError.NoRefreshesLeft) {
                        refreshes shouldBe 3
                    }
                    else -> {
                        val state = env.useCases.getTodayState.execute()
                        state.prompt.id shouldBe seen.last()
                        state.refreshesRemaining shouldBe 3 - refreshes
                    }
                }
                refreshes shouldBeLessThanOrEqual 3
            }
        }
    }

    test("save sequences: exactly one entry holding the last valid text (model: an optional string)") {
        checkAll(100, Arb.list(Generators.text, 0..10)) { texts ->
            val env = TestEnvironment()
            var model: String? = null
            for (text in texts) {
                try {
                    env.useCases.saveTodayEntry.execute(text, day("2026-10-08"), env.time.now)
                    model = text
                } catch (_: JournalError.EmptyText) {
                    JournalEntry.isValidText(text) shouldBe false
                } catch (_: JournalError.TextTooLong) {
                    JournalEntry.isWithinLimit(text) shouldBe false
                }
                env.entries.stored.values shouldHaveSize (if (model == null) 0 else 1)
                env.entries.stored[day("2026-10-08")]?.text shouldBe model
            }
        }
    }
})
