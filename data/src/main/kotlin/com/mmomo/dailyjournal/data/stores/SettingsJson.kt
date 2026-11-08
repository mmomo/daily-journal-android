package com.mmomo.dailyjournal.data.stores

import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import org.json.JSONArray
import org.json.JSONObject

/**
 * `settings.v1` JSON in the iOS shape (data model §2). Missing fields take defaults, unknown fields
 * are ignored, and unreadable input never throws: it decodes to the defaults (DATA-S2, DATA-S7).
 */
object SettingsJson {
    fun encode(settings: AppSettings): String = JSONObject()
        .put("categories", JSONArray(settings.categories.map { it.rawValue }.sorted()))
        .put(
            "reminder",
            JSONObject()
                .put("isEnabled", settings.reminder.isEnabled)
                .put("hour", settings.reminder.hour)
                .put("minute", settings.reminder.minute),
        )
        .put("onboardingCompleted", settings.onboardingCompleted)
        .toString()

    /** Decodes [json]; `null` if it isn't a JSON object at all (the caller then logs and uses defaults). */
    fun decodeOrNull(json: String?): AppSettings? {
        if (json == null) return null
        val root = try {
            JSONObject(json)
        } catch (_: Exception) {
            return null
        }
        val defaults = AppSettings()
        val categories = root.optJSONArray("categories")?.let { array ->
            (0 until array.length()).mapNotNull { PromptCategory.fromRawValue(array.optString(it)) }.toSet()
        } ?: defaults.categories
        val reminderJson = root.optJSONObject("reminder")
        val reminder = if (reminderJson == null) {
            defaults.reminder
        } else {
            ReminderSettings(
                isEnabled = reminderJson.optBoolean("isEnabled", defaults.reminder.isEnabled),
                hour = reminderJson.optInt("hour", defaults.reminder.hour),
                minute = reminderJson.optInt("minute", defaults.reminder.minute),
            )
        }
        return AppSettings(
            categories = categories, // empty falls back to all four in the AppSettings constructor
            reminder = reminder,
            onboardingCompleted = root.optBoolean("onboardingCompleted", defaults.onboardingCompleted),
        )
    }
}
