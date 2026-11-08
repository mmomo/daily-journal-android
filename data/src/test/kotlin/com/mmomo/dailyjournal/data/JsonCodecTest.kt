package com.mmomo.dailyjournal.data

import com.mmomo.dailyjournal.data.stores.PromptStateJson
import com.mmomo.dailyjournal.data.stores.SettingsJson
import com.mmomo.dailyjournal.data.support.IosDataFixtures
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.time.CalendarDay
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** DATA-S1, S2, S3, S7 (org.json needs Robolectric on the JVM). */
@RunWith(RobolectricTestRunner::class)
class JsonCodecTest {
    private val iosSettings = AppSettings(setOf(PromptCategory.GRATITUDE, PromptCategory.GOALS), ReminderSettings(true, 7, 30), true)
    private val day = CalendarDay.fromKey("2026-10-08")!!
    private val iosState = DailyPromptState(day, "reflection-098", 1, listOf("reflection-098", "reflection-125"), "creativity,goals,gratitude,reflection")

    @Test
    fun `settings written by iOS decode to the same values`() {
        SettingsJson.decodeOrNull(IosDataFixtures.SETTINGS_JSON) shouldBe iosSettings
    }

    @Test
    fun `settings encode to the iOS shape`() {
        val json = JSONObject(SettingsJson.encode(iosSettings))
        json.keys().asSequence().toSet() shouldBe setOf("categories", "reminder", "onboardingCompleted")
        json.getJSONArray("categories").let { a -> (0 until a.length()).map { a.getString(it) } } shouldBe listOf("goals", "gratitude")
        json.getJSONObject("reminder").let { listOf(it.getBoolean("isEnabled"), it.getInt("hour"), it.getInt("minute")) } shouldBe listOf(true, 7, 30)
        json.getBoolean("onboardingCompleted") shouldBe true
        SettingsJson.decodeOrNull(json.toString()) shouldBe iosSettings
    }

    @Test
    fun `missing fields take defaults, unknown fields are ignored, empty categories mean all`() {
        SettingsJson.decodeOrNull("{}") shouldBe AppSettings()
        SettingsJson.decodeOrNull("""{"categories":[],"futureField":42,"reminder":{"isEnabled":true}}""") shouldBe
            AppSettings(reminder = ReminderSettings(isEnabled = true))
    }

    @Test
    fun `unreadable settings decode to null (caller uses defaults)`() {
        SettingsJson.decodeOrNull("not json").shouldBeNull()
        SettingsJson.decodeOrNull(null).shouldBeNull()
    }

    @Test
    fun `prompt state written by iOS decodes and encoding round-trips`() {
        PromptStateJson.decodeOrNull(IosDataFixtures.PROMPT_STATE_JSON) shouldBe mapOf(day to iosState)
        PromptStateJson.decodeOrNull(PromptStateJson.encode(mapOf(day to iosState))) shouldBe mapOf(day to iosState)
        JSONObject(PromptStateJson.encode(mapOf(day to iosState))).getJSONObject("2026-10-08").has("promptID") shouldBe true
    }

    @Test
    fun `invalid prompt state items are dropped, unreadable input is null`() {
        PromptStateJson.decodeOrNull("""{"bad-key":{"promptID":"goals-001"},"2026-10-08":{"refreshesUsed":1}}""") shouldBe emptyMap()
        PromptStateJson.decodeOrNull("[1,2,3]").shouldBeNull()
    }
}
