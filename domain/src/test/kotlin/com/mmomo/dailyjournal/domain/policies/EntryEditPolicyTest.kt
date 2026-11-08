package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.EntryEditability
import com.mmomo.dailyjournal.domain.support.day
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.support.mexicoCity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EntryEditPolicyTest : FunSpec({
    test("today is editable until midnight (US-2.2)") {
        EntryEditPolicy.editability(day("2026-10-08"), hasEntry = true, now = instant("2026-10-08 22:00"), timeZone = mexicoCity) shouldBe
            EntryEditability.Editable(until = instant("2026-10-09 00:00"))
    }

    test("an existing entry locks at midnight, including during the grace window (US-2.2, BR-E5)") {
        EntryEditPolicy.editability(day("2026-10-08"), true, instant("2026-10-09 00:00"), mexicoCity) shouldBe EntryEditability.Locked
        EntryEditPolicy.editability(day("2026-10-08"), true, instant("2026-10-09 09:00"), mexicoCity) shouldBe EntryEditability.Locked
    }

    test("a missing yesterday is backfillable until 12:00 (US-2.3)") {
        EntryEditPolicy.editability(day("2026-10-08"), false, instant("2026-10-09 11:59"), mexicoCity) shouldBe
            EntryEditability.Backfillable(until = instant("2026-10-09 12:00"))
        EntryEditPolicy.editability(day("2026-10-08"), false, instant("2026-10-09 12:00"), mexicoCity) shouldBe EntryEditability.Locked
    }

    test("older and future days are locked (US-2.3, US-2.4, BR-E6)") {
        EntryEditPolicy.editability(day("2026-10-07"), false, instant("2026-10-09 09:00"), mexicoCity) shouldBe EntryEditability.Locked
        EntryEditPolicy.editability(day("2026-10-05"), true, instant("2026-10-08 09:00"), mexicoCity) shouldBe EntryEditability.Locked
        EntryEditPolicy.editability(day("2026-10-10"), true, instant("2026-10-09 09:00"), mexicoCity) shouldBe EntryEditability.Locked
    }

    test("late save grace: edited before midnight and saved within 5 s is allowed (BR-E4, FR-8.1)") {
        val d = day("2026-10-08")
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-08 23:59:59"), instant("2026-10-09 00:00:02"), mexicoCity) shouldBe true
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-08 23:59:59"), instant("2026-10-09 00:00:04"), mexicoCity) shouldBe true
    }

    test("late save grace: too late, or edited after midnight, is rejected (BR-E4)") {
        val d = day("2026-10-08")
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-08 23:59:59"), instant("2026-10-09 00:00:05"), mexicoCity) shouldBe false
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-08 23:59:59"), instant("2026-10-09 00:00:06"), mexicoCity) shouldBe false
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-09 00:00:01"), instant("2026-10-09 00:00:02"), mexicoCity) shouldBe false
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-08 23:00"), instant("2026-10-08 23:30"), mexicoCity) shouldBe false
    }

    test("an edit time in the future counts as now (BR-SEC3)") {
        val d = day("2026-10-08")
        EntryEditPolicy.lateSaveAllowed(d, instant("2026-10-10 10:00"), instant("2026-10-09 00:00:02"), mexicoCity) shouldBe false
    }
})
