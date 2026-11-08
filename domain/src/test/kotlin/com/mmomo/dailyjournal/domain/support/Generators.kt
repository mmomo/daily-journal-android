package com.mmomo.dailyjournal.domain.support

import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.withEdgecases
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Reusable generators (business logic model §10.2, PBT-07; clarification C2:A). */
object Generators {
    val first: CalendarDay = day("2024-01-01")
    val last: CalendarDay = day("2032-12-31")

    /** Days from 2024 to 2032 (leap years 2024 and 2028), with the range edges. */
    val day: Arb<CalendarDay> = Arb.int(first.dayNumber..last.dayNumber)
        .withEdgecases(first.dayNumber, last.dayNumber, day("2024-02-29").dayNumber, day("2028-02-29").dayNumber)
        .let { ints -> arbitrary { CalendarDay.fromDayNumber(ints.bind()) } }

    val zoneIds = listOf(
        "America/Mexico_City", "America/New_York", "Europe/London", "Asia/Kolkata",
        "Asia/Kathmandu", "Australia/Lord_Howe", "Pacific/Kiritimati", "Etc/GMT+12",
    )
    val timeZone: Arb<TimeZone> = Arb.element(zoneIds).let { ids -> arbitrary { TimeZone.of(ids.bind()) } }

    /** Seconds of the day, biased toward 00:00, 11:59:59, 12:00 and 23:59:59. */
    private val secondOfDay: Arb<Int> = Arb.choice(
        Arb.int(0 until 86_400),
        Arb.element(0, 1, 43_199, 43_200, 43_201, 86_399),
    )

    /** An instant on a generated day in [zone] (the start of the day plus a biased offset). */
    fun instantOn(zone: TimeZone): Arb<Instant> = arbitrary {
        day.bind().startIn(zone) + secondOfDay.bind().seconds
    }

    val categories: Arb<Set<PromptCategory>> = Arb.set(Arb.element(PromptCategory.entries), 1..4)

    private val whitespace = listOf(" ", "\t", "\n", "\r\n", " ", " ")
    private val words = listOf("quiet", "morning", "Café", "naïve", "über", "señal", "日記", "☕", "🌅", "grateful", "beach")

    /** Entry texts including empty, whitespace-only, accented, emoji, long and just-over-limit strings. */
    val text: Arb<String> = Arb.choice(
        Arb.element("", " ", "\n\t ", "  "),
        Arb.list(Arb.element(whitespace), 1..8).let { parts -> arbitrary { parts.bind().joinToString("") } },
        Arb.list(Arb.element(words), 1..30).let { parts -> arbitrary { parts.bind().joinToString(" ") } },
        Arb.string(1..200),
        arbitrary { "a".repeat(JournalEntry.MAX_LENGTH) },
        arbitrary { "a".repeat(JournalEntry.MAX_LENGTH + 1) },
        arbitrary { "🌅".repeat(JournalEntry.MAX_LENGTH) },
    )

    /** Valid entry texts (what a user could save). */
    val validText: Arb<String> = Arb.list(Arb.element(words), 1..20).let { parts -> arbitrary { parts.bind().joinToString(" ") } }

    /** Sets of entry days: empty, runs and gaps, within a window around [around]. */
    fun entryDays(around: CalendarDay): Arb<Set<CalendarDay>> = Arb.set(Arb.int(-40..3), 0..30)
        .let { offsets -> arbitrary { offsets.bind().mapTo(HashSet()) { around.adding(it) } } }

    /** Queries: fragments of [text] with case or accents changed, or unrelated words. */
    fun queryFor(text: String): Arb<String> = arbitrary {
        if (text.length < 2 || Arb.int(0..3).bind() == 0) {
            Arb.element(words).bind()
        } else {
            val start = Arb.int(0 until text.length - 1).bind()
            val end = Arb.int(start + 1..minOf(text.length, start + 8)).bind()
            val piece = text.substring(start, end)
            if (Arb.int(0..1).bind() == 0) piece.uppercase() else piece
        }
    }
}
