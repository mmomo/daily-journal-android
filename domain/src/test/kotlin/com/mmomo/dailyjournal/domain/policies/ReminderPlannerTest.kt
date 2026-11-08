package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import com.mmomo.dailyjournal.domain.support.newYork
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ReminderPlannerTest : FunSpec({
    val on = ReminderSettings(isEnabled = true, hour = 20, minute = 0)
    val notDone = StreakSummary(5, 5, StreakStatus.Active, isTodayDone = false)

    test("disabled or not allowed schedules nothing (BR-R1)") {
        ReminderPlanner.plan(ReminderSettings(), true, notDone, instant("2026-10-08 09:00"), mexicoCity).shouldBeEmpty()
        ReminderPlanner.plan(on, false, notDone, instant("2026-10-08 09:00"), mexicoCity).shouldBeEmpty()
    }

    test("a 7-day window at 20:00 with the streak text on today's reminder only (BR-R2, BR-R4)") {
        val plan = ReminderPlanner.plan(on, true, notDone, instant("2026-10-08 09:00"), mexicoCity)
        plan.size shouldBe 7
        plan.first().fireAt shouldBe instant("2026-10-08 20:00")
        plan.first().body shouldBe "Your daily prompt is waiting. Keep your 5-day streak going."
        plan.first().identifier shouldBe "reminder-2026-10-08"
        plan.drop(1).all { it.body == ReminderPlanner.BASE_BODY } shouldBe true
        plan.all { it.deepLink == DeepLink.TODAY_EDITOR && it.title == "Time to journal" } shouldBe true
    }

    test("today is skipped when done, tomorrow still scheduled (US-6.2)") {
        val done = notDone.copy(isTodayDone = true)
        val plan = ReminderPlanner.plan(on, true, done, instant("2026-10-08 09:00"), mexicoCity)
        plan.size shouldBe 6
        plan.first().day shouldBe day("2026-10-09")
    }

    test("a time already passed today starts tomorrow; changing the time moves reminders (US-6.1)") {
        ReminderPlanner.plan(on, true, notDone, instant("2026-10-08 20:30"), mexicoCity).first().day shouldBe day("2026-10-09")
        val early = on.copy(hour = 7, minute = 30)
        ReminderPlanner.plan(early, true, notDone, instant("2026-10-08 09:00"), mexicoCity).first().fireAt shouldBe
            instant("2026-10-09 07:30")
    }

    test("a reminder time inside a DST gap fires at the end of the gap (BR-R3)") {
        val gap = on.copy(hour = 2, minute = 30)
        val plan = ReminderPlanner.plan(gap, true, notDone, instant("2026-03-07 12:00", newYork), newYork)
        plan.first { it.day == day("2026-03-08") }.fireAt shouldBe instant("2026-03-08 03:00", newYork)
    }
})
