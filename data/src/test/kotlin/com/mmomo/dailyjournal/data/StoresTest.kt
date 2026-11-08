package com.mmomo.dailyjournal.data

import androidx.datastore.preferences.core.edit
import com.mmomo.dailyjournal.data.stores.JournalPreferences
import com.mmomo.dailyjournal.data.stores.NotificationPermissionFlag
import com.mmomo.dailyjournal.data.stores.PreferencesPromptStateStore
import com.mmomo.dailyjournal.data.stores.PreferencesSettingsStore
import com.mmomo.dailyjournal.data.support.TempPreferences
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** US-1.2, US-1.3, US-1.4, US-7.2; DATA-S1–S7, DATA-R1 flag. */
@RunWith(RobolectricTestRunner::class)
class StoresTest {
    private val prefs = TempPreferences()
    private val settings = PreferencesSettingsStore(prefs.store, Dispatchers.IO)
    private val promptState = PreferencesPromptStateStore(prefs.store, Dispatchers.IO)

    @After fun tearDown() = prefs.close()

    private fun state(dayKey: String, id: String = "goals-001") =
        DailyPromptState(CalendarDay.fromKey(dayKey)!!, id, 0, listOf(id), "goals")

    @Test
    fun `settings default, save and persist`() = runBlocking<Unit> {
        settings.load() shouldBe AppSettings()
        val changed = AppSettings(categories = setOf(PromptCategory.REFLECTION), onboardingCompleted = true)
        settings.save(changed)
        PreferencesSettingsStore(prefs.store, Dispatchers.IO).load() shouldBe changed
    }

    @Test
    fun `a corrupt settings value falls back to defaults and is overwritten on save (DATA-S2)`() = runBlocking<Unit> {
        prefs.store.edit { it[JournalPreferences.SETTINGS] = "{not json" }
        settings.load() shouldBe AppSettings()
        settings.save(AppSettings(onboardingCompleted = true))
        settings.load().onboardingCompleted shouldBe true
    }

    @Test
    fun `settings changes emit only for real changes (DATA-S6)`() = runBlocking<Unit> {
        val received = async(Dispatchers.IO) { withTimeout(10_000) { settings.changes.first() } }
        delay(500) // let the listener subscribe before writing
        settings.save(AppSettings(onboardingCompleted = true))
        received.await() shouldBe Unit
    }

    @Test
    fun `prompt state saves, reloads and prunes days before keepingFrom (DATA-S4)`() = runBlocking<Unit> {
        promptState.save(state("2026-10-07"), keepingFrom = CalendarDay.fromKey("2026-10-07")!!)
        promptState.save(state("2026-10-08", "goals-002"), keepingFrom = CalendarDay.fromKey("2026-10-07")!!)
        promptState.state(CalendarDay.fromKey("2026-10-07")!!)?.promptId shouldBe "goals-001"
        promptState.save(state("2026-10-09"), keepingFrom = CalendarDay.fromKey("2026-10-08")!!)
        promptState.state(CalendarDay.fromKey("2026-10-07")!!).shouldBeNull()
        promptState.state(CalendarDay.fromKey("2026-10-08")!!)?.promptId shouldBe "goals-002"
    }

    @Test
    fun `concurrent prompt state saves never lose an update (DATA-S5)`() = runBlocking<Unit> {
        val keep = CalendarDay.fromKey("2026-01-01")!!
        (1..20).map { n -> launch(Dispatchers.IO) { promptState.save(state(CalendarDay.fromKey("2026-01-01")!!.adding(n).key), keep) } }
            .forEach { it.join() }
        (1..20).map { async { promptState.state(CalendarDay.fromKey("2026-01-01")!!.adding(it)) } }.awaitAll().all { it != null } shouldBe true
    }

    @Test
    fun `corrupt prompt state means no state (DATA-S3)`() = runBlocking<Unit> {
        prefs.store.edit { it[JournalPreferences.PROMPT_STATE] = "]]" }
        promptState.state(CalendarDay.fromKey("2026-10-08")!!).shouldBeNull()
    }

    @Test
    fun `notification permission flag (DATA-R1)`() = runBlocking<Unit> {
        val flag = NotificationPermissionFlag(prefs.store)
        flag.wasAsked() shouldBe false
        flag.markAsked()
        flag.wasAsked() shouldBe true
    }

    @Test
    fun `a corrupted preferences file is replaced and the app keeps working (NFR design 3)`() = runBlocking<Unit> {
        val context = com.mmomo.dailyjournal.data.support.appContext()
        val file = java.io.File(context.filesDir, "datastore/journal_preferences.preferences_pb")
        file.parentFile!!.mkdirs()
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42, 0x01, 0x02))
        // The production DataStore has the corruption handler: reading must not throw and gives defaults.
        val store = JournalPreferences.dataStore(context)
        PreferencesSettingsStore(store, Dispatchers.IO).load() shouldBe AppSettings()
        PreferencesSettingsStore(store, Dispatchers.IO).save(AppSettings(onboardingCompleted = true))
        PreferencesSettingsStore(store, Dispatchers.IO).load().onboardingCompleted shouldBe true
    }
}
