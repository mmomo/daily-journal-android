package com.mmomo.dailyjournal.domain.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * Calendar helpers. All day arithmetic uses the ISO (proleptic Gregorian) calendar in the device's
 * current time zone (BR-T2). DST behavior matches iOS `DayMath`.
 */
object DayMath {

    internal fun localDate(instant: Instant, timeZone: TimeZone): LocalDate =
        instant.toJavaInstant().atZone(timeZone.toJavaZoneId()).toLocalDate()

    fun startOfDay(day: CalendarDay, timeZone: TimeZone): Instant =
        LocalDate.of(day.year, day.month, day.day).atStartOfDay(timeZone.toJavaZoneId()).toInstant().toKotlinInstant()

    /**
     * The instant of `hour:minute` local time on [day]. If a DST gap removes that wall-clock time,
     * returns the next valid instant (the end of the gap), like iOS `.nextTime` (BR-R3). If the time
     * falls in an overlap, the earlier instant is used.
     */
    fun instant(day: CalendarDay, hour: Int, minute: Int, timeZone: TimeZone): Instant {
        val start = startOfDay(day, timeZone)
        if (hour == 0 && minute == 0) return start
        val zone = timeZone.toJavaZoneId()
        val local = LocalDateTime.of(day.year, day.month, day.day, hour, minute)
        val rules = zone.rules
        val result = if (rules.getValidOffsets(local).isEmpty()) {
            rules.getTransition(local).instant.toKotlinInstant()
        } else {
            local.atZone(zone).toInstant().toKotlinInstant()
        }
        // A wall-clock time before the day's (late) first instant doesn't exist that day.
        return if (result < start) start else result
    }
}
