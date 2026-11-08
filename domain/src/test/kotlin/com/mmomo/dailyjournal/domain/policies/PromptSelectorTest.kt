package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.support.IosParityFixtures
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.fullLibrary
import com.mmomo.dailyjournal.domain.time.StableHash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class PromptSelectorTest : FunSpec({
    val selector = PromptSelector(fullLibrary)
    val all = PromptCategory.entries.toSet()
    val allSets = (1 until 16).map { mask -> PromptCategory.entries.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet() }

    test("FNV-1a hash matches iOS for plain, accented, emoji and Japanese text (US-1.2)") {
        IosParityFixtures.hashes.forEach { (text, expected) -> withClue(text) { StableHash.fnv1a64(text) shouldBe expected } }
    }

    test("daily picks match iOS for every category combination and date (US-1.2, FR-1.4)") {
        IosParityFixtures.dailyPicks.forEach { (key, dayKey, expected) ->
            val categories = key.split(",").map { PromptCategory.fromRawValue(it)!! }.toSet()
            withClue("$key $dayKey") { selector.dailyPrompt(day(dayKey), categories).id shouldBe expected }
        }
    }

    test("a prompt answered recently is skipped, matching iOS (BR-P3)") {
        val first = selector.dailyPrompt(day("2026-10-08"), all)
        first.id shouldBe IosParityFixtures.DAILY_ALL_2026_10_08
        selector.dailyPrompt(day("2026-10-08"), all, answeredRecently = setOf(first.id)).id shouldBe
            IosParityFixtures.DAILY_ALL_2026_10_08_SKIPPING_ANSWERED
    }

    test("three refreshes match iOS and never repeat a shown prompt (US-1.3, BR-P6)") {
        val shown = mutableListOf(selector.dailyPrompt(day("2026-10-08"), all).id)
        IosParityFixtures.refreshesAll20261008.forEachIndexed { index, expected ->
            val next = selector.refreshedPrompt(day("2026-10-08"), all, index + 1, shown)
            next.id shouldBe expected
            shown shouldNotContain next.id
            shown += next.id
        }
        val gratitude = setOf(PromptCategory.GRATITUDE)
        val daily = selector.dailyPrompt(day("2026-10-08"), gratitude)
        selector.refreshedPrompt(day("2026-10-08"), gratitude, 1, listOf(daily.id)).id shouldBe
            IosParityFixtures.REFRESH_GRATITUDE_2026_10_08
    }

    test("no prompt repeats within 30 days and picks stay in the selected categories (US-1.2, US-1.4)") {
        allSets.forEach { categories ->
            val ids = (0 until 30).map { selector.dailyPrompt(day("2026-10-08").adding(it), categories) }
            withClue(categories) {
                ids.map { it.id }.toSet().size shouldBe 30
                ids.all { it.category in categories } shouldBe true
            }
        }
    }

    test("one category alone goes 150 days without a repeat (US-1.2 with 600 prompts)") {
        PromptCategory.entries.forEach { category ->
            (0 until 150).map { selector.dailyPrompt(day("2026-01-01").adding(it), setOf(category)).id }
                .toSet().size shouldBe 150
        }
    }

    test("an empty category set falls back to the whole library, like iOS") {
        selector.pool(emptySet()).size shouldBe 600
    }

    test("a single-prompt pool can't be refreshed (BR-P9)") {
        val tiny = PromptSelector(listOf(Prompt("goals-001", PromptCategory.GOALS, "The only prompt here")))
        tiny.dailyPrompt(day("2026-10-08"), setOf(PromptCategory.GOALS)).id shouldBe "goals-001"
        shouldThrow<JournalError.NoRefreshesLeft> {
            tiny.refreshedPrompt(day("2026-10-08"), setOf(PromptCategory.GOALS), 1, listOf("goals-001"))
        }
    }

    test("refresh relaxes exclusions in the iOS order when everything is excluded (BR-P9)") {
        val two = PromptSelector(listOf(
            Prompt("goals-001", PromptCategory.GOALS, "First prompt text"),
            Prompt("goals-002", PromptCategory.GOALS, "Second prompt text"),
        ))
        val goals = setOf(PromptCategory.GOALS)
        // Both shown; only the current one (the last shown) is excluded in the final step.
        two.refreshedPrompt(day("2026-10-08"), goals, 2, listOf("goals-001", "goals-002")).id shouldBe "goals-001"
        // Answered-recently is relaxed before shown-today.
        two.refreshedPrompt(day("2026-10-08"), goals, 1, listOf("goals-001"), answeredRecently = setOf("goals-002")).id shouldBe "goals-002"
    }
})
