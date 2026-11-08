package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.time.StableHash

/**
 * Deterministic prompt selection, ported from the iOS code bit for bit (business-logic-model §1).
 *
 * The daily pick walks the category pool with a full-period stride, so no prompt is picked twice
 * within `pool.size` (≥ 90) consecutive days for a category selection, without stored history.
 */
class PromptSelector(library: List<Prompt>) {
    private val library: List<Prompt>

    init {
        require(library.isNotEmpty()) { "The prompt library must not be empty" }
        this.library = library.sortedBy { it.id }
    }

    /** BR-P1: prompts in the selected categories, sorted by id; the whole library if none match. */
    fun pool(categories: Set<PromptCategory>): List<Prompt> {
        val filtered = library.filter { it.category in categories }
        return filtered.ifEmpty { library }
    }

    /** BR-P2, BR-P3: the day's prompt, skipping prompts answered in the previous 30 days. */
    fun dailyPrompt(
        day: CalendarDay,
        categories: Set<PromptCategory>,
        answeredRecently: Set<String> = emptySet(),
    ): Prompt {
        val pool = pool(categories)
        val n = pool.size
        if (n <= 1) return pool[0]
        val stride = stride(categories, n)
        val base = mod(stride.offset + mod(day.dayNumber.toLong(), n) * stride.step, n)
        for (k in 0 until n) {
            val candidate = pool[((base + k.toLong() * stride.step) % n).toInt()]
            if (candidate.id !in answeredRecently) return candidate
        }
        return pool[base.toInt()] // BR-P9: everything excluded
    }

    /**
     * BR-P6: the prompt for refresh number [refreshNumber] (1-based) on [day]. Never returns a prompt
     * already shown today while others remain; avoids prompts answered in the previous 30 days.
     */
    fun refreshedPrompt(
        day: CalendarDay,
        categories: Set<PromptCategory>,
        refreshNumber: Int,
        shownToday: List<String>,
        answeredRecently: Set<String> = emptySet(),
    ): Prompt {
        val pool = pool(categories)
        val n = pool.size
        if (n <= 1) throw JournalError.NoRefreshesLeft()
        val seed = "refresh|${day.key}|${PromptCategory.key(categories)}|$refreshNumber"
        val start = (StableHash.fnv1a64(seed) % n.toULong()).toInt()
        val shown = shownToday.toSet()

        fun scan(excluded: Set<String>): Prompt? {
            for (k in 0 until n) {
                val candidate = pool[(start + k) % n]
                if (candidate.id !in excluded) return candidate
            }
            return null
        }

        scan(shown + answeredRecently)?.let { return it }
        // BR-P9: relax the answered-recently exclusion, then keep only "not the current prompt".
        scan(shown)?.let { return it }
        shownToday.lastOrNull()?.let { current -> scan(setOf(current))?.let { return it } }
        throw JournalError.NoRefreshesLeft()
    }

    internal data class Stride(val offset: Long, val step: Long)

    companion object {
        const val MAX_REFRESHES_PER_DAY = 3
        const val NO_REPEAT_WINDOW_DAYS = 30

        /** Offset and a step coprime with [n], derived from the category selection (identical to iOS). */
        internal fun stride(categories: Set<PromptCategory>, n: Int): Stride {
            val key = PromptCategory.key(categories)
            val offset = (StableHash.fnv1a64("offset|$key") % n.toULong()).toLong()
            var step = (StableHash.fnv1a64("step|$key") % n.toULong()).toInt()
            if (step == 0) step = 1
            while (StableHash.gcd(step, n) != 1) {
                step = step % (n - 1) + 1
            }
            return Stride(offset, step.toLong())
        }

        internal fun mod(value: Long, n: Int): Long = ((value % n) + n) % n
    }
}
