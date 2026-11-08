package com.mmomo.dailyjournal.domain.performance

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.policies.PromptSelector
import com.mmomo.dailyjournal.domain.policies.StreakCalculator
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.fullLibrary
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.medianTime
import com.mmomo.dailyjournal.domain.support.mexicoCity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import kotlin.time.Duration.Companion.milliseconds

/** Regression smoke tests for DOM-PERF-1 to 3 (median after warm-up; targets are generous on purpose). */
class PerformanceSmokeTest : FunSpec({
    val start = day("2016-10-10")
    // Ten years of entries with a gap every 50 days, like a real journal.
    val entryDays = (0 until 3_650).filter { it % 50 != 49 }.map { start.adding(it) }.toSet()
    val entries = entryDays.mapIndexed { i, d ->
        JournalEntry("id$i", d, "Day $i: a quiet morning with coffee, a walk, and a café visit.", "gratitude-001",
            "What small thing made today better?", instant("${d.key} 21:00"), instant("${d.key} 21:00"))
    }

    test("DOM-PERF-1: streak with 3,650 entry days under 50 ms") {
        val now = instant("2026-10-08 21:00")
        medianTime { StreakCalculator.summary(entryDays, now, mexicoCity) } shouldBeLessThan 50.milliseconds
    }

    test("DOM-PERF-2: daily pick from 600 prompts with 30 answered under 10 ms") {
        val selector = PromptSelector(fullLibrary)
        val answered = fullLibrary.take(30).map { it.id }.toSet()
        val all = PromptCategory.entries.toSet()
        medianTime { selector.dailyPrompt(day("2026-10-08"), all, answered) } shouldBeLessThan 10.milliseconds
    }

    test("DOM-PERF-3: search over 3,650 entries under 100 ms") {
        medianTime { entries.count { EntrySearchMatcher.matches(it, "cafe") } } shouldBeLessThan 100.milliseconds
    }
})
