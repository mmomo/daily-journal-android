package com.mmomo.dailyjournal.domain.time

import com.mmomo.dailyjournal.domain.support.IosParityFixtures
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import com.mmomo.dailyjournal.domain.support.newYork
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class CalendarDayTest : FunSpec({
    test("dayNumber counts from 2001-01-01 like iOS (BR-T5)") {
        day("2001-01-01").dayNumber shouldBe 0
        day("2001-01-02").dayNumber shouldBe 1
        day("2000-12-31").dayNumber shouldBe -1
        day("2026-10-08").dayNumber shouldBe IosParityFixtures.DAY_NUMBER_2026_10_08
    }

    test("adding crosses leap days and years") {
        day("2024-02-28").adding(1) shouldBe day("2024-02-29")
        day("2024-02-28").adding(2) shouldBe day("2024-03-01")
        day("2026-12-31").adding(1) shouldBe day("2027-01-01")
        day("2026-03-01").adding(-1) shouldBe day("2026-02-28")
    }

    test("out-of-range parts are normalized like iOS") {
        CalendarDay(2026, 2, 30) shouldBe day("2026-03-02")
        CalendarDay(2026, 13, 1) shouldBe day("2027-01-01")
        CalendarDay(2026, 0, 1) shouldBe day("2025-12-01")
    }

    test("key round-trips and fromKey rejects malformed or impossible dates (BR-SEC4)") {
        CalendarDay.fromKey("2026-10-08")?.key shouldBe "2026-10-08"
        listOf(null, "", "2026-1-08", "2026-02-30", "2026-13-01", "26-10-08", "2026-10-08x", "+026-10-08", "２０２６-10-08")
            .forEach { CalendarDay.fromKey(it).shouldBeNull() }
    }

    test("the same instant falls on different days in different zones") {
        val moment = instant("2026-10-08 22:00", mexicoCity)
        CalendarDay.containing(moment, mexicoCity) shouldBe day("2026-10-08")
        CalendarDay.containing(moment, TimeZone.of("Asia/Tokyo")) shouldBe day("2026-10-09")
    }

    test("DST fall-back day is 25 hours and noon is 12:00 (New York 2026-11-01)") {
        val d = day("2026-11-01")
        (d.adding(1).startIn(newYork) - d.startIn(newYork)) shouldBe 25.hours
        d.noonIn(newYork) shouldBe instant("2026-11-01 12:00", newYork)
        d.startIn(newYork) shouldBe Instant.fromEpochSeconds(IosParityFixtures.NEW_YORK_START_2026_11_01)
    }

    test("DST spring-forward day is 23 hours (New York 2026-03-08)") {
        val d = day("2026-03-08")
        (d.adding(1).startIn(newYork) - d.startIn(newYork)) shouldBe 23.hours
        d.noonIn(newYork) shouldBe Instant.fromEpochSeconds(IosParityFixtures.NEW_YORK_NOON_2026_03_08)
    }

    test("a wall-clock time in a DST gap moves to the end of the gap, like iOS") {
        val d = day("2026-03-08")
        val resolved = DayMath.instant(d, hour = 2, minute = 30, timeZone = newYork)
        resolved shouldBe Instant.fromEpochSeconds(IosParityFixtures.NEW_YORK_0230_IN_GAP_2026_03_08)
        resolved shouldBeGreaterThanOrEqualTo instant("2026-03-08 03:00", newYork)
        resolved shouldBeLessThan instant("2026-03-08 04:00", newYork)
        CalendarDay.containing(resolved, newYork) shouldBe d
    }

    test("ordering follows the calendar") {
        listOf(day("2026-10-09"), day("2025-12-31"), day("2026-10-08")).sorted() shouldBe
            listOf(day("2025-12-31"), day("2026-10-08"), day("2026-10-09"))
    }
})
