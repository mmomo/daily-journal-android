package com.mmomo.dailyjournal.domain.property

import com.mmomo.dailyjournal.domain.support.Generators
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.time.StableHash
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/** Property-based tests for time and hashing (business logic model §10.1). Random seed per run; printed on failure. */
class TimePropertyTest : FunSpec({
    test("round-trip: fromKey(day.key) == day") {
        checkAll(Generators.day) { day -> CalendarDay.fromKey(day.key) shouldBe day }
    }

    test("invariant: adding moves dayNumber by exactly n and is reversible") {
        checkAll(Generators.day, Arb.int(-4000..4000)) { day, n ->
            day.adding(n).dayNumber shouldBe day.dayNumber + n
            day.adding(n).adding(-n) shouldBe day
        }
    }

    test("invariant: ordering by dayNumber matches ordering by (year, month, day)") {
        checkAll(Generators.day, Generators.day) { a, b ->
            val byParts = compareValuesBy(a, b, { it.year }, { it.month }, { it.day })
            Integer.signum(a.compareTo(b)) shouldBe Integer.signum(byParts)
        }
    }

    test("round-trip: the start and noon of a day fall on that day in every zone, DST days included") {
        checkAll(Generators.day, Generators.timeZone) { day, zone ->
            CalendarDay.containing(day.startIn(zone), zone) shouldBe day
            CalendarDay.containing(day.noonIn(zone), zone) shouldBe day
        }
    }

    test("invariant: the hash is deterministic") {
        checkAll(Arb.string(0..64)) { text -> StableHash.fnv1a64(text) shouldBe StableHash.fnv1a64(String(text.toCharArray())) }
    }
})
