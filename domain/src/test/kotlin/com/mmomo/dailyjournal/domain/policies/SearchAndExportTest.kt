package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun entry(dayKey: String, text: String, prompt: String = "What small thing made today better?") =
    JournalEntry("id-$dayKey", day(dayKey), text, "gratitude-001", prompt, instant("$dayKey 21:00"), instant("$dayKey 21:00"))

class SearchAndExportTest : FunSpec({
    test("search matches entry text and prompt text (US-3.2)") {
        EntrySearchMatcher.matches(entry("2026-10-01", "A walk on the beach."), "beach") shouldBe true
        EntrySearchMatcher.matches(entry("2026-10-01", "Nothing here", prompt = "What are you grateful for?"), "grateful") shouldBe true
    }

    test("search ignores case and accents (US-3.2)") {
        EntrySearchMatcher.matches(entry("2026-10-01", "Coffee at the Café"), "cafe") shouldBe true
        EntrySearchMatcher.matches(entry("2026-10-01", "Coffee at the cafe"), "CAFÉ") shouldBe true
        EntrySearchMatcher.matches(entry("2026-10-01", "Señal débil"), "senal DEBIL") shouldBe true
    }

    test("no match and empty query (US-3.2, BR-Q1)") {
        EntrySearchMatcher.matches(entry("2026-10-01", "A walk on the beach."), "zebra") shouldBe false
        EntrySearchMatcher.matches(entry("2026-10-01", "Anything"), "   ") shouldBe true
    }

    test("Markdown export has the same layout as iOS, oldest first (US-3.3, BR-X)") {
        val doc = JournalExporter.export(
            listOf(entry("2026-10-05", "Second."), entry("2026-10-01", "Grateful for a quiet morning.")),
            ExportFormat.MARKDOWN, day("2026-10-08"),
        )
        doc.fileName shouldBe "daily-journal-2026-10-08.md"
        doc.mimeType shouldBe "text/markdown"
        doc.contents shouldBe """
            # Daily Journal

            Exported 2026-10-08 · 2 entries

            ## 2026-10-01

            > What small thing made today better?

            Grateful for a quiet morning.

            ## 2026-10-05

            > What small thing made today better?

            Second.

        """.trimIndent()
    }

    test("plain-text export has the same layout as iOS (US-3.3)") {
        val doc = JournalExporter.export(
            listOf(entry("2026-10-01", "Grateful for a quiet morning."), entry("2026-10-05", "Second.")),
            ExportFormat.PLAIN_TEXT, day("2026-10-08"),
        )
        doc.fileName shouldBe "daily-journal-2026-10-08.txt"
        doc.contents shouldBe "DAILY JOURNAL\nExported 2026-10-08 - 2 entries\n\n" +
            "2026-10-01\nPrompt: What small thing made today better?\n\nGrateful for a quiet morning.\n\n" +
            "----------------------------------------\n\n" +
            "2026-10-05\nPrompt: What small thing made today better?\n\nSecond.\n"
    }

    test("one entry is singular") {
        JournalExporter.export(listOf(entry("2026-10-01", "Only.")), ExportFormat.PLAIN_TEXT, day("2026-10-08"))
            .contents.lines()[1] shouldBe "Exported 2026-10-08 - 1 entry"
    }
})
