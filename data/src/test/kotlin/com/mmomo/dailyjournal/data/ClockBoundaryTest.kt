package com.mmomo.dailyjournal.data

import com.mmomo.dailyjournal.data.system.ClockEvents
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import com.mmomo.dailyjournal.domain.support.newYork
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** US-2.2, US-4.3, US-5.3; DATA-C2 (pure computation, no Android needed). */
class ClockBoundaryTest : FunSpec({
    test("before noon the next boundary is 12:00; after noon it's midnight") {
        ClockEvents.nextBoundary(instant("2026-10-08 09:00"), mexicoCity) shouldBe instant("2026-10-08 12:00")
        ClockEvents.nextBoundary(instant("2026-10-08 12:00"), mexicoCity) shouldBe instant("2026-10-09 00:00")
        ClockEvents.nextBoundary(instant("2026-10-08 23:59:59"), mexicoCity) shouldBe instant("2026-10-09 00:00")
        ClockEvents.nextBoundary(instant("2026-10-09 00:00"), mexicoCity) shouldBe instant("2026-10-09 12:00")
    }

    test("DST days keep the right boundaries (New York)") {
        ClockEvents.nextBoundary(instant("2026-11-01 13:00", newYork), newYork) shouldBe instant("2026-11-02 00:00", newYork)
        ClockEvents.nextBoundary(instant("2026-03-08 01:00", newYork), newYork) shouldBe instant("2026-03-08 12:00", newYork)
    }
})
