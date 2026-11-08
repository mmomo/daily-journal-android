package com.mmomo.dailyjournal.domain.property

import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.policies.EntryEditPolicy
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.policies.JournalExporter
import com.mmomo.dailyjournal.domain.policies.PromptSelector
import com.mmomo.dailyjournal.domain.policies.ReminderPlanner
import com.mmomo.dailyjournal.domain.policies.StreakCalculator
import com.mmomo.dailyjournal.domain.support.Generators
import com.mmomo.dailyjournal.domain.support.References
import com.mmomo.dailyjournal.domain.support.fullLibrary
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.time.Duration.Companion.seconds

private val selector = PromptSelector(fullLibrary)

class PolicyPropertyTest : FunSpec({
    test("invariant: the daily pick is deterministic and in the selected categories (BR-P1, BR-P2)") {
        checkAll(Generators.day, Generators.categories) { day, categories ->
            val pick = selector.dailyPrompt(day, categories)
            pick shouldBe selector.dailyPrompt(day, categories)
            (pick.category in categories) shouldBe true
        }
    }

    test("invariant: over pool-size consecutive days every prompt appears exactly once (BR-P3)") {
        checkAll(20, Generators.day, Generators.categories) { start, categories ->
            val n = selector.pool(categories).size
            (0 until n).map { selector.dailyPrompt(start.adding(it), categories).id }.toSet().size shouldBe n
        }
    }

    test("invariant: the daily pick avoids recently answered prompts while others remain (BR-P3)") {
        checkAll(Generators.day, Generators.categories, Arb.int(0..30)) { day, categories, count ->
            val answered = selector.pool(categories).shuffled(kotlin.random.Random(day.dayNumber)).take(count).map { it.id }.toSet()
            selector.dailyPrompt(day, categories, answered).id shouldNotContainIn answered
        }
    }

    test("invariant: a refresh never repeats a shown prompt while unshown ones remain (BR-P6)") {
        checkAll(Generators.day, Generators.categories, Arb.int(1..3)) { day, categories, r ->
            val shown = (0 until r).map { selector.pool(categories)[(day.dayNumber + it * 7).mod(selector.pool(categories).size)].id }
            val next = selector.refreshedPrompt(day, categories, r, shown)
            shown shouldNotContain next.id
            next shouldBe selector.refreshedPrompt(day, categories, r, shown)
        }
    }

    test("invariant: only today is editable; only a missing yesterday before noon is backfillable (BR-E5)") {
        checkAll(Generators.timeZone, Arb.int(-5..5), Arb.boolean()) { zone, offset, hasEntry ->
            checkAll(50, Generators.instantOn(zone)) { now ->
                val today = CalendarDay.containing(now, zone)
                val day = today.adding(offset)
                val result = EntryEditPolicy.editability(day, hasEntry, now, zone)
                (result is EntryEditability.Editable) shouldBe (offset == 0)
                (result is EntryEditability.Backfillable) shouldBe (offset == -1 && !hasEntry && now < today.noonIn(zone))
            }
        }
    }

    test("invariant: a late save is never allowed 5 s or more after midnight, or for an edit after midnight (BR-E4)") {
        checkAll(Generators.day, Generators.timeZone, Arb.int(-30..30), Arb.int(-30..30)) { day, zone, editOffset, nowOffset ->
            val midnight = day.adding(1).startIn(zone)
            val edited = midnight + editOffset.seconds
            val now = midnight + nowOffset.seconds
            val allowed = EntryEditPolicy.lateSaveAllowed(day, edited, now, zone)
            if (nowOffset >= 5 || nowOffset < 0 || minOf(editOffset, nowOffset) >= 0) allowed.shouldBeFalse()
        }
    }

    test("invariants and oracle: streak bounds, best = longest run, adding a day never lowers either (BR-S)") {
        checkAll(Generators.day, Generators.timeZone) { today, zone ->
            checkAll(30, Generators.entryDays(today), Arb.int(0..86_399)) { days, second ->
                val now = today.startIn(zone) + second.seconds
                val local = CalendarDay.containing(now, zone)
                val s = StreakCalculator.summary(days, now, zone)
                s.current shouldBeGreaterThanOrEqual 0
                s.current shouldBeLessThanOrEqual days.size + 1
                s.best shouldBeGreaterThanOrEqual s.current
                val counted = days.filter { it <= local }.map { it.dayNumber }.toMutableSet()
                if (days.any { it > local }) counted += local.dayNumber
                s.best shouldBe maxOf(References.longestRun(counted), s.current)
                val more = StreakCalculator.summary(days + local, now, zone)
                more.best shouldBeGreaterThanOrEqual s.best
                if (s.status != StreakStatus.None || s.current > 0) more.current shouldBeGreaterThanOrEqual minOf(s.current, 1)
            }
        }
    }

    test("invariant: text is valid exactly when it has a non-whitespace character and at most 20,000 code points (BR-E2)") {
        checkAll(Generators.text) { text ->
            val codePoints = text.codePointCount(0, text.length)
            JournalEntry.isWithinLimit(text) shouldBe (codePoints <= JournalEntry.MAX_LENGTH)
            JournalEntry.isValidText(text) shouldBe text.any { !it.isWhitespace() && it != ' ' && it != ' ' && it != ' ' }
        }
    }

    test("oracle and invariants: search matches the reference; case and accents don't matter (BR-Q)") {
        checkAll(Generators.validText) { text ->
            val entry = JournalEntry("id", CalendarDay(2026, 10, 8), text, "goals-001", "What would make tomorrow a good day?",
                instant("2026-10-08 21:00"), instant("2026-10-08 21:00"))
            checkAll(20, Generators.queryFor(text)) { query ->
                EntrySearchMatcher.matches(entry, query) shouldBe References.matches(entry, query)
                EntrySearchMatcher.matches(entry, query.uppercase()) shouldBe EntrySearchMatcher.matches(entry, query.lowercase())
            }
            EntrySearchMatcher.matches(entry, "") shouldBe true
        }
    }

    test("invariant: export contains every entry once, in date order, text unchanged (BR-X)") {
        val entriesArb = Arb.set(Arb.int(0..2000), 0..40).let { offsets ->
            Arb.bind(offsets, Arb.list(Generators.validText, 40..40)) { days, texts ->
                days.mapIndexed { i, offset ->
                    val d = CalendarDay(2024, 1, 1).adding(offset)
                    JournalEntry("id$i", d, texts[i], "goals-001", "Prompt", instant("${d.key} 21:00"), instant("${d.key} 21:00"))
                }
            }
        }
        checkAll(entriesArb, Arb.element(ExportFormat.entries)) { entries, format ->
            val contents = JournalExporter.export(entries.shuffled(), format, CalendarDay(2033, 1, 1)).contents
            val positions = entries.sortedBy { it.day }.map { contents.indexOf(if (format == ExportFormat.MARKDOWN) "## ${it.day.key}" else "${it.day.key}\nPrompt:") }
            positions.all { it >= 0 } shouldBe true
            positions shouldBe positions.sorted()
            entries.all { contents.contains(it.text) } shouldBe true
        }
    }

    test("invariants and idempotence: reminder plan (BR-R)") {
        checkAll(Generators.timeZone, Arb.int(0..23), Arb.int(0..59), Arb.boolean()) { zone, hour, minute, done ->
            checkAll(20, Generators.instantOn(zone)) { now ->
                val settings = ReminderSettings(isEnabled = true, hour = hour, minute = minute)
                val streak = StreakSummary(2, 2, StreakStatus.Active, isTodayDone = done)
                val plan = ReminderPlanner.plan(settings, true, streak, now, zone)
                plan.size shouldBeLessThanOrEqual 7
                plan.map { it.day }.toSet().size shouldBe plan.size
                plan.all { it.fireAt > now } shouldBe true
                if (done) plan.none { it.day == CalendarDay.containing(now, zone) } shouldBe true
                ReminderPlanner.plan(settings, true, streak, now, zone) shouldBe plan
            }
        }
    }

    test("round-trip and invariant: deep links (NFR-6.3)") {
        checkAll(Arb.element(DeepLink.entries)) { link -> DeepLink.parse(link.toUri()) shouldBe link }
        checkAll(Arb.string(0..40)) { text ->
            val parsed = DeepLink.parse(text)
            if (parsed != null) DeepLink.parse(parsed.toUri()) shouldBe parsed
        }
    }
})

private infix fun String.shouldNotContainIn(set: Set<String>) = (this in set).shouldBeFalse()
