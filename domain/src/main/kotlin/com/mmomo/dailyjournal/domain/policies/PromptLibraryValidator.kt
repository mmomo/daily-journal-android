package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import java.util.Locale

/** Why a prompt library is invalid. Messages contain ids only, never prompt text. */
sealed class PromptLibraryException(message: String) : Exception(message) {
    class DuplicateId(val id: String) : PromptLibraryException("Duplicate id $id")
    class MalformedId(val id: String) : PromptLibraryException("Malformed id $id")
    class CategoryMismatch(val id: String) : PromptLibraryException("Category does not match id $id")
    class TextLength(val id: String) : PromptLibraryException("Text length out of range for $id")
    class DuplicateText(val id: String) : PromptLibraryException("Duplicate text for $id")
    class TooFewPrompts(val category: String, val count: Int) :
        PromptLibraryException("Too few prompts in $category: $count")
    class TooFewTotal(val count: Int) : PromptLibraryException("Too few prompts in total: $count")
}

/** Validates the bundled prompt library (business-logic-model §9.1, NFR-6.7). Same rules as iOS. */
object PromptLibraryValidator {
    const val MINIMUM_PER_CATEGORY = 90
    const val MINIMUM_TOTAL = 365
    private val idPattern = Regex("^(gratitude|reflection|goals|creativity)-\\d{3}$")

    fun validate(
        prompts: List<Prompt>,
        minimumPerCategory: Int = MINIMUM_PER_CATEGORY,
        minimumTotal: Int = MINIMUM_TOTAL,
    ) {
        val ids = HashSet<String>()
        val texts = HashSet<String>()
        for (prompt in prompts) {
            if (!ids.add(prompt.id)) throw PromptLibraryException.DuplicateId(prompt.id)
            if (!idPattern.matches(prompt.id)) throw PromptLibraryException.MalformedId(prompt.id)
            val prefix = prompt.id.substringBefore('-')
            if (PromptCategory.fromRawValue(prefix) != prompt.category) {
                throw PromptLibraryException.CategoryMismatch(prompt.id)
            }
            val trimmed = prompt.text.trim()
            if (trimmed.length !in 10..200) throw PromptLibraryException.TextLength(prompt.id)
            if (!texts.add(trimmed.lowercase(Locale.ROOT))) throw PromptLibraryException.DuplicateText(prompt.id)
        }
        for (category in PromptCategory.entries) {
            val count = prompts.count { it.category == category }
            if (count < minimumPerCategory) throw PromptLibraryException.TooFewPrompts(category.rawValue, count)
        }
        if (prompts.size < minimumTotal) throw PromptLibraryException.TooFewTotal(prompts.size)
    }
}
