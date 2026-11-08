package com.mmomo.dailyjournal.data.property

import com.mmomo.dailyjournal.data.system.ClockEvents
import com.mmomo.dailyjournal.domain.support.Generators
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import kotlin.time.Duration.Companion.hours

/** Invariant: the next boundary is after now, is the day's 12:00 or the next midnight, and within 25 hours. */
class ClockPropertyTest : FunSpec({
    test("invariant - next clock boundary") {
        checkAll(Generators.timeZone) { zone ->
            checkAll(100, Generators.instantOn(zone)) { now ->
                val next = ClockEvents.nextBoundary(now, zone)
                val today = CalendarDay.containing(now, zone)
                (next > now) shouldBe true
                (next == today.noonIn(zone) || next == today.adding(1).startIn(zone)) shouldBe true
                (next - now <= 25.hours) shouldBe true
            }
        }
    }
})
