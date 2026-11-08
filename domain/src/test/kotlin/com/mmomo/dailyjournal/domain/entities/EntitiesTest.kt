package com.mmomo.dailyjournal.domain.entities

import com.mmomo.dailyjournal.domain.policies.PromptLibraryException
import com.mmomo.dailyjournal.domain.policies.PromptLibraryValidator
import com.mmomo.dailyjournal.domain.policies.TextValidation
import com.mmomo.dailyjournal.domain.support.fullLibrary
import com.mmomo.dailyjournal.domain.support.libraryOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EntitiesTest : FunSpec({
    test("known deep links round-trip; query and fragment are ignored (US-5.2, US-6.3)") {
        DeepLink.entries.forEach { DeepLink.parse(it.toUri()) shouldBe it }
        DeepLink.parse("dailyjournal://today?source=widget#x") shouldBe DeepLink.TODAY_EDITOR
        DeepLink.parse("DailyJournal://BACKFILL") shouldBe DeepLink.BACKFILL_EDITOR
    }

    test("unknown or malformed links are rejected (NFR-6.3, BR-SEC4)") {
        listOf(null, "", "https://today", "dailyjournal://settings", "dailyjournal://today/extra", "dailyjournal:today",
            "dailyjournal://", "javascript:alert(1)", "dailyjournal://today\u0000", "%%%")
            .forEach { DeepLink.parse(it).shouldBeNull() }
    }

    test("text validation: blank and over-limit are rejected (US-2.1, BR-E2)") {
        shouldThrow<JournalError.EmptyText> { TextValidation.validate("  \n\t ") }
        shouldThrow<JournalError.TextTooLong> { TextValidation.validate("a".repeat(JournalEntry.MAX_LENGTH + 1)) }
        TextValidation.validate("a".repeat(JournalEntry.MAX_LENGTH))
        // Emoji count as one character each (code points), so 20,000 emoji are allowed.
        TextValidation.validate("🌅".repeat(JournalEntry.MAX_LENGTH))
        shouldThrow<JournalError.TextTooLong> { TextValidation.validate("🌅".repeat(JournalEntry.MAX_LENGTH + 1)) }
    }

    test("settings defaults, clamping and empty categories, like iOS (BR-G1)") {
        val defaults = AppSettings()
        defaults.categories shouldBe PromptCategory.entries.toSet()
        defaults.reminder shouldBe ReminderSettings(isEnabled = false, hour = 20, minute = 0)
        defaults.onboardingCompleted shouldBe false
        ReminderSettings(true, hour = 30, minute = -5) shouldBe ReminderSettings(true, 23, 0)
        AppSettings(categories = emptySet()).categories shouldBe PromptCategory.entries.toSet()
    }

    test("category key is sorted and comma-joined like iOS") {
        PromptCategory.key(setOf(PromptCategory.REFLECTION, PromptCategory.CREATIVITY)) shouldBe "creativity,reflection"
    }

    test("prompt library validation (NFR-6.7)") {
        PromptLibraryValidator.validate(fullLibrary)
        shouldThrow<PromptLibraryException.TooFewPrompts> { PromptLibraryValidator.validate(libraryOf(89)) }
        shouldThrow<PromptLibraryException.DuplicateId> { PromptLibraryValidator.validate(fullLibrary + fullLibrary.first()) }
        shouldThrow<PromptLibraryException.MalformedId> {
            PromptLibraryValidator.validate(fullLibrary + Prompt("goals-1", PromptCategory.GOALS, "A malformed id prompt"))
        }
        shouldThrow<PromptLibraryException.CategoryMismatch> {
            PromptLibraryValidator.validate(fullLibrary + Prompt("goals-999", PromptCategory.GRATITUDE, "Mismatched category"))
        }
        shouldThrow<PromptLibraryException.TextLength> {
            PromptLibraryValidator.validate(fullLibrary + Prompt("goals-999", PromptCategory.GOALS, "Too short"))
        }
    }
})
