package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import com.mmomo.dailyjournal.domain.support.newYork
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

private fun streak(days: List<String>, now: String, zone: TimeZone = mexicoCity): StreakSummary =
    StreakCalculator.summary(days.map { day(it) }.toSet(), instant(now, zone), zone)

class StreakCalculatorTest : FunSpec({
    test("streak including today (US-4.1)") {
        streak(listOf("2026-10-06", "2026-10-07", "2026-10-08"), "2026-10-08 21:00") shouldBe
            StreakSummary(3, 3, StreakStatus.Active, isTodayDone = true)
    }

    test("today not written yet ends at yesterday, not at risk (US-4.1)") {
        streak(listOf("2026-10-06", "2026-10-07"), "2026-10-08 09:00") shouldBe
            StreakSummary(2, 2, StreakStatus.Active, isTodayDone = false)
    }

    test("no entries gives 0 (US-4.1)") {
        streak(emptyList(), "2026-10-08 09:00") shouldBe StreakSummary.EMPTY
    }

    test("daylight-saving fall-back day still counts as one day (US-4.1, New York)") {
        streak(listOf("2026-10-31", "2026-11-01", "2026-11-02"), "2026-11-02 20:00", newYork).current shouldBe 3
    }

    test("at risk before noon when yesterday is missing (US-4.2, BR-S4)") {
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07"), "2026-10-09 09:00") shouldBe
            StreakSummary(3, 3, StreakStatus.AtRisk(3, instant("2026-10-09 12:00")), isTodayDone = false)
    }

    test("a backfill restores, and writing today extends (US-4.2)") {
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"), "2026-10-09 11:00").current shouldBe 4
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09"), "2026-10-09 11:30").current shouldBe 5
    }

    test("today written, yesterday missing, before noon shows 1 at risk of 3 (US-4.2, BR-S2)") {
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-09"), "2026-10-09 09:00") shouldBe
            StreakSummary(1, 3, StreakStatus.AtRisk(3, instant("2026-10-09 12:00")), isTodayDone = true)
    }

    test("reset at the 12:00 cutoff, restart, two missed days (US-4.3)") {
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07"), "2026-10-09 12:00") shouldBe
            StreakSummary(0, 3, StreakStatus.None, isTodayDone = false)
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-09"), "2026-10-09 20:00").current shouldBe 1
        streak(listOf("2026-10-06"), "2026-10-09 09:00").current shouldBe 0
    }

    test("moving the clock back to 11:00 brings the at-risk state back (US-4.3 misuse, BR-SEC1)") {
        streak(listOf("2026-10-05", "2026-10-06", "2026-10-07"), "2026-10-09 11:00").status shouldBe
            StreakStatus.AtRisk(3, instant("2026-10-09 12:00"))
    }

    test("best streak and new record (US-4.4)") {
        val twelve = (0 until 12).map { day("2026-09-01").adding(it).key }
        val four = (0 until 4).map { day("2026-10-05").adding(it).key }
        streak(twelve + four, "2026-10-08 21:00") shouldBe StreakSummary(4, 12, StreakStatus.Active, true)
        val thirteen = (0 until 13).map { day("2026-09-26").adding(it).key }
        streak(thirteen, "2026-10-08 21:00").best shouldBe 13
    }

    test("a future-dated entry counts as today (BR-E7)") {
        streak(listOf("2026-10-07", "2026-10-09"), "2026-10-08 21:00") shouldBe
            StreakSummary(2, 2, StreakStatus.Active, isTodayDone = true)
    }

    test("changing time zone recomputes for the new local date without losing entries (US-4.1 misuse)") {
        val days = listOf("2026-10-06", "2026-10-07", "2026-10-08")
        val moment = instant("2026-10-08 21:00", mexicoCity) // 2026-10-09 17:00 in Kiritimati
        StreakCalculator.summary(days.map { day(it) }.toSet(), moment, TimeZone.of("Pacific/Kiritimati")) shouldBe
            StreakSummary(3, 3, StreakStatus.Active, isTodayDone = false)
    }
})
