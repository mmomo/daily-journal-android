package com.mmomo.dailyjournal.data

import android.util.Log
import com.mmomo.dailyjournal.data.database.EntryEntity
import com.mmomo.dailyjournal.data.support.TempDatabase
import com.mmomo.dailyjournal.data.support.entry
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/** US-2.1, US-3.1, US-3.2, US-4.1; DATA-E1–E8. */
@RunWith(RobolectricTestRunner::class)
class RepositoryTest {
    private val db = TempDatabase()
    private val repo = db.repository

    @After fun tearDown() = db.close()

    private fun day(key: String) = CalendarDay.fromKey(key)!!

    @Test
    fun `insert then update by id keeps one entry per day (DATA-E1)`() = runBlocking<Unit> {
        repo.save(entry("2026-10-08", "Morning."))
        repo.save(entry("2026-10-08", "Morning. And evening."))
        repo.allEntries().size shouldBe 1
        repo.entry(day("2026-10-08"))?.text shouldBe "Morning. And evening."
    }

    @Test
    fun `a different entry on the same day fails and never overwrites (DATA-E1)`() = runBlocking<Unit> {
        repo.save(entry("2026-10-08", "First."))
        shouldThrowAny { repo.save(entry("2026-10-08", "Intruder.", id = "other-id")) }
        repo.entry(day("2026-10-08"))?.text shouldBe "First."
    }

    @Test
    fun `ranges, after, all and entry days (DATA-E3)`() = runBlocking<Unit> {
        listOf("2026-10-01", "2026-10-05", "2026-10-08", "2026-10-09").forEach { repo.save(entry(it)) }
        repo.entries(day("2026-10-02"), day("2026-10-08")).map { it.day.key }.sorted() shouldBe listOf("2026-10-05", "2026-10-08")
        repo.entriesAfter(day("2026-10-08")).map { it.day.key } shouldBe listOf("2026-10-09")
        repo.entryDays() shouldBe setOf(day("2026-10-01"), day("2026-10-05"), day("2026-10-08"), day("2026-10-09"))
        repo.entry(day("2026-10-02")).shouldBeNull()
    }

    @Test
    fun `search follows the domain matcher (DATA-E4)`() = runBlocking<Unit> {
        repo.save(entry("2026-10-01", "A day at the Café"))
        repo.save(entry("2026-10-02", "Rain"))
        repo.search("cafe").map { it.day.key } shouldBe listOf("2026-10-01")
        repo.search("zebra").shouldBeEmpty()
    }

    @Test
    fun `stored values round-trip at millisecond precision (DATA model 1)`() = runBlocking<Unit> {
        val original = entry("2026-10-08", "Emoji 🌅, accents é, 日記")
        repo.save(original)
        repo.entry(day("2026-10-08")) shouldBe original
    }

    @Test
    fun `an unreadable row is skipped, logged without text, and kept (DATA-E5)`() = runBlocking<Unit> {
        ShadowLog.clear()
        db.database.entries().insert(EntryEntity("bad-row", "2026-13-45", 0, "SECRET JOURNAL TEXT", "goals-001", "Prompt", 0, 0))
        repo.save(entry("2026-10-08"))
        repo.allEntries().map { it.id } shouldBe listOf("id-2026-10-08")
        db.database.entries().byId("bad-row")?.text shouldBe "SECRET JOURNAL TEXT"
        val logs = ShadowLog.getLogs().filter { it.type >= Log.WARN }
        logs.any { it.msg.contains("bad-row") } shouldBe true
        logs.none { it.msg.contains("SECRET") } shouldBe true
    }

    @Test
    fun `a write emits a change signal (DATA-E6)`() = runBlocking<Unit> {
        val changed = async(Dispatchers.IO) { withTimeout(10_000) { repo.changes.first() } }
        delay(500) // let the listener subscribe before writing
        repo.save(entry("2026-10-08"))
        changed.await() shouldBe Unit
    }
}
