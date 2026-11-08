package com.mmomo.dailyjournal.domain.entities

import java.net.URI
import java.net.URISyntaxException

/** Routes used by the widget and reminders. Same URIs as iOS. */
enum class DeepLink(val host: String) {
    TODAY_EDITOR("today"),
    BACKFILL_EDITOR("backfill"),
    ;

    fun toUri(): String = "$SCHEME://$host"

    companion object {
        const val SCHEME = "dailyjournal"

        /**
         * Accepts only the `dailyjournal` scheme with a known host and no path; query and fragment are ignored.
         * Anything else returns `null` (NFR-6.3, BR-SEC4).
         */
        fun parse(uri: String?): DeepLink? {
            if (uri.isNullOrEmpty()) return null
            val parsed = try {
                URI(uri)
            } catch (_: URISyntaxException) {
                return null
            }
            if (!parsed.scheme.equals(SCHEME, ignoreCase = true)) return null
            val path = parsed.rawPath
            if (!path.isNullOrEmpty() && path != "/") return null
            val host = parsed.host?.lowercase() ?: return null
            return entries.firstOrNull { it.host == host }
        }
    }
}
