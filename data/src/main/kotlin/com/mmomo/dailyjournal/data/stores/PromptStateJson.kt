package com.mmomo.dailyjournal.data.stores

import com.mmomo.dailyjournal.domain.entities.DailyPromptState
import com.mmomo.dailyjournal.domain.time.CalendarDay
import org.json.JSONArray
import org.json.JSONObject

/**
 * `promptState.v1` JSON in the iOS shape: a map from day key to state (data model §3). Field names
 * match iOS (`promptID`, `shownPromptIDs`). Invalid entries are dropped; unreadable input decodes to
 * `null` so the caller can log it and use an empty map (DATA-S3).
 */
object PromptStateJson {
    fun encode(states: Map<CalendarDay, DailyPromptState>): String {
        val root = JSONObject()
        for ((day, state) in states) {
            root.put(
                day.key,
                JSONObject()
                    .put("day", JSONObject().put("year", state.day.year).put("month", state.day.month).put("day", state.day.day))
                    .put("promptID", state.promptId)
                    .put("refreshesUsed", state.refreshesUsed)
                    .put("shownPromptIDs", JSONArray(state.shownPromptIds))
                    .put("categoriesKey", state.categoriesKey),
            )
        }
        return root.toString()
    }

    fun decodeOrNull(json: String?): Map<CalendarDay, DailyPromptState>? {
        if (json == null) return null
        val root = try {
            JSONObject(json)
        } catch (_: Exception) {
            return null
        }
        val result = LinkedHashMap<CalendarDay, DailyPromptState>()
        for (key in root.keys()) {
            val day = CalendarDay.fromKey(key) ?: continue
            val item = root.optJSONObject(key) ?: continue
            val promptId = item.optString("promptID").takeIf { it.isNotEmpty() } ?: continue
            val shownArray = item.optJSONArray("shownPromptIDs")
            val shown = if (shownArray == null) {
                listOf(promptId)
            } else {
                (0 until shownArray.length()).map { shownArray.optString(it) }.filter { it.isNotEmpty() }
            }
            result[day] = DailyPromptState(
                day = day,
                promptId = promptId,
                refreshesUsed = item.optInt("refreshesUsed", 0).coerceIn(0, 3),
                shownPromptIds = shown,
                categoriesKey = item.optString("categoriesKey", ""),
            )
        }
        return result
    }
}
