package com.mmomo.dailyjournal.domain.support

import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

val mexicoCity: TimeZone = TimeZone.of("America/Mexico_City")
val newYork: TimeZone = TimeZone.of("America/New_York")

/** Settable clock for deterministic tests (NFR-4.2). */
class FixedTimeSource(override var now: Instant, override var timeZone: TimeZone = mexicoCity) : TimeSource

private val minuteFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val secondFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** Local wall-clock time ("2026-10-08 21:00" or with seconds) in [zone]. */
fun instant(text: String, zone: TimeZone = mexicoCity): Instant {
    val format = if (text.length > 16) secondFormat else minuteFormat
    return LocalDateTime.parse(text, format).atZone(zone.toJavaZoneId()).toInstant().toKotlinInstant()
}

fun day(key: String): CalendarDay = requireNotNull(CalendarDay.fromKey(key)) { "Bad day key $key" }
