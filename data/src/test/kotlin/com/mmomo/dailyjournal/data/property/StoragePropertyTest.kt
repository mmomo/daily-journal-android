package com.mmomo.dailyjournal.data.property

import com.mmomo.dailyjournal.data.stores.JournalPreferences
import com.mmomo.dailyjournal.data.stores.PreferencesPromptStateStore
import com.mmomo.dailyjournal.data.stores.PromptStateJson
import com.mmomo.dailyjournal.data.stores.SettingsJson
import com.mmomo.dailyjournal.data.support.TempDatabase
import com.mmomo.dailyjournal.data.support.TempPreferences
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.policies.EntrySearchMatcher
import com.mmomo.dailyjournal.domain.support.Generators
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

/**
 * Property-based tests for the data unit (business logic model §8), using the domain's shared
 * generators. Random seed per run; Kotest prints the seed and the shrunk input on failure.
 */
@RunWith(RobolectricTestRunner::class)
class StoragePropertyTest {
    private val db = TempDatabase()
    private val prefs = TempPreferences()

    @After fun tearDown() {
        db.close()
        prefs.close()
    }

    private val millis = Arb.long(1_700_000_000_000L..2_000_000_000_000L)
    private val entryArb: Arb<JournalEntry> = arbitrary {
        val created = Instant.fromEpochMilliseconds(millis.bind())
        JournalEntry(
            id = "id-" + Arb.int(0..1_000_000).bind(),
            day = Generators.day.bind(),
            text = Generators.text.bind(),
            promptId = "goals-" + Arb.int(1..150).bind().toString().padStart(3, '0'),
            promptText = Generators.validText.bind(),
            createdAt = created,
            updatedAt = created,
        )
    }
    private val settingsArb: Arb<AppSettings> = arbitrary {
        AppSettings(
            categories = Generators.categories.bind(),
            reminder = ReminderSettings(Arb.boolean().bind(), Arb.int(0..23).bind(), Arb.int(0..59).bind()),
            onboardingCompleted = Arb.boolean().bind(),
        )
    }
    private val stateArb: Arb<DailyPromptState> = arbitrary {
        val shown = Arb.list(Arb.element((1..150).map { "reflection-" + it.toString().padStart(3, '0') }), 1..4).bind()
        DailyPromptState(Generators.day.bind(), shown.last(), Arb.int(0..3).bind(), shown, "reflection")
    }

    private suspend fun clearEntries() = withContext(Dispatchers.IO) { db.database.clearAllTables() }

    @Test
    fun `round-trip - an entry reads back unchanged (millisecond precision)`() = runBlocking<Unit> {
        checkAll(100, entryArb) { entry ->
            clearEntries()
            db.repository.save(entry)
            db.repository.entry(entry.day) shouldBe entry
        }
    }

    @Test
    fun `round-trip - settings JSON`() = runBlocking<Unit> {
        checkAll(500, settingsArb) { settings -> SettingsJson.decodeOrNull(SettingsJson.encode(settings)) shouldBe settings }
    }

    @Test
    fun `round-trip - prompt state JSON`() = runBlocking<Unit> {
        checkAll(300, Arb.list(stateArb, 0..5)) { states ->
            val map = states.associateBy { it.day }
            PromptStateJson.decodeOrNull(PromptStateJson.encode(map)) shouldBe map
        }
    }

    @Test
    fun `invariant - decoding arbitrary text never throws`() = runBlocking<Unit> {
        val junk = Arb.choice(Arb.string(0..200), Arb.element("{}", "[]", "null", "{\"categories\":5}", "{\"2026-10-08\":[]}", "{\"reminder\":{\"hour\":\"x\"}}"))
        checkAll(1000, junk) { text ->
            SettingsJson.decodeOrNull(text)
            PromptStateJson.decodeOrNull(text)
        }
    }

    @Test
    fun `oracle - ranges, after and search equal filtering all entries`() = runBlocking<Unit> {
        checkAll(60, Arb.set(Arb.int(0..120), 0..25), Arb.int(0..120), Arb.int(0..120), Arb.element("cafe", "MORNING", "日記", "zz", "a")) { offsets, a, b, query ->
            clearEntries()
            val start = CalendarDay.fromKey("2026-01-01")!!
            offsets.forEach { db.repository.save(com.mmomo.dailyjournal.data.support.entry(start.adding(it).key, text = "Day $it at the Café, morning 日記")) }
            val all = db.repository.allEntries()
            val from = start.adding(minOf(a, b))
            val through = start.adding(maxOf(a, b))
            db.repository.entries(from, through).map { it.id }.toSet() shouldBe all.filter { it.day in from..through }.map { it.id }.toSet()
            db.repository.entriesAfter(from).map { it.id }.toSet() shouldBe all.filter { it.day > from }.map { it.id }.toSet()
            db.repository.search(query).map { it.id }.toSet() shouldBe all.filter { EntrySearchMatcher.matches(it, query) }.map { it.id }.toSet()
        }
    }

    @Test
    fun `stateful - random saves behave like a day-to-entry map and another id on a used day fails`() = runBlocking<Unit> {
        val command = arbitrary { Triple(Arb.int(0..4).bind(), Arb.element("a", "b").bind(), Generators.validText.bind()) }
        checkAll(60, Arb.list(command, 0..15)) { commands ->
            clearEntries()
            val model = HashMap<CalendarDay, JournalEntry>()
            for ((offset, idSuffix, text) in commands) {
                val e = com.mmomo.dailyjournal.data.support.entry(CalendarDay.fromKey("2026-10-01")!!.adding(offset).key, text, id = "$offset-$idSuffix")
                val existing = model[e.day]
                val result = runCatching { db.repository.save(e) }
                if (existing == null || existing.id == e.id) {
                    result.isSuccess shouldBe true
                    model[e.day] = e
                } else {
                    result.isFailure shouldBe true
                }
                db.repository.allEntries().associateBy { it.day } shouldBe model
            }
        }
    }

    @Test
    fun `invariant - pruning keeps nothing older than keepingFrom and keeps the saved state`() = runBlocking<Unit> {
        val store = PreferencesPromptStateStore(prefs.store, Dispatchers.IO)
        checkAll(60, Arb.list(stateArb, 1..6), Generators.day) { states, keepingFrom ->
            states.forEach { store.save(it, keepingFrom) }
            val stored = PromptStateJson.decodeOrNull(
                prefs.store.data.first()[JournalPreferences.PROMPT_STATE],
            ) ?: emptyMap()
            stored.keys.all { it >= keepingFrom } shouldBe true
            val last = states.last()
            if (last.day >= keepingFrom) stored[last.day] shouldBe last
        }
    }
}
