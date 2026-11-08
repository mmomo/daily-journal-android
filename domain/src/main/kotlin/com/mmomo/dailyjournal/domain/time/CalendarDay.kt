package com.mmomo.dailyjournal.domain.time

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * A civil (Gregorian) date that identifies a journal day independently of any time zone.
 *
 * Entries keep the day they were written on; it is never recomputed from timestamps, so travelling
 * across time zones does not move an entry to another day (BR-T3). Ported from iOS `CalendarDay`.
 */
class CalendarDay private constructor(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDay> {

    /** Days since 2001-01-01 (day 0), exactly as on iOS; prompt selection depends on it (BR-T5). */
    val dayNumber: Int get() = dayNumberOf(year, month, day)

    /** Stable storage and route key, e.g. `2026-10-08`. */
    val key: String
        get() = year.toString().padStart(4, '0') + "-" +
            month.toString().padStart(2, '0') + "-" +
            day.toString().padStart(2, '0')

    /** Moves by whole calendar days; never uses time intervals, so DST cannot shift it (BR-T4). */
    fun adding(days: Int): CalendarDay = fromDayNumber(dayNumber + days)

    /** First instant of this day in [timeZone] (handles days that don't start at 00:00). */
    fun startIn(timeZone: TimeZone): Instant = DayMath.startOfDay(this, timeZone)

    /** 12:00 local on this day, or the next valid instant if a DST gap removes it. */
    fun noonIn(timeZone: TimeZone): Instant = DayMath.instant(this, hour = 12, minute = 0, timeZone = timeZone)

    override fun compareTo(other: CalendarDay): Int = dayNumber.compareTo(other.dayNumber)

    override fun equals(other: Any?): Boolean =
        other is CalendarDay && year == other.year && month == other.month && day == other.day

    override fun hashCode(): Int = dayNumber

    override fun toString(): String = key

    companion object {
        /** Creates a day from its parts. Out-of-range parts are normalized like iOS (2026-02-30 → 2026-03-02). */
        operator fun invoke(year: Int, month: Int, day: Int): CalendarDay = fromDayNumber(dayNumberOf(year, month, day))

        /** The day with the given [dayNumber] (days since 2001-01-01). */
        fun fromDayNumber(dayNumber: Int): CalendarDay {
            val (y, m, d) = civilFromDays(dayNumber + EPOCH_OFFSET)
            return CalendarDay(y, m, d)
        }

        /** The Gregorian day containing [instant] in [timeZone] (BR-T2). */
        fun containing(instant: Instant, timeZone: TimeZone): CalendarDay {
            val date = DayMath.localDate(instant, timeZone)
            return CalendarDay(date.year, date.monthValue, date.dayOfMonth)
        }

        /**
         * Parses a `YYYY-MM-DD` key. Returns `null` for anything malformed or not a real date (NFR-6.3).
         * Stricter than iOS: only ASCII digits are accepted (no signs).
         */
        fun fromKey(key: String?): CalendarDay? {
            if (key == null) return null
            val pieces = key.split("-")
            if (pieces.size != 3) return null
            val (ys, ms, ds) = pieces
            if (ys.length != 4 || ms.length != 2 || ds.length != 2) return null
            if (!(ys + ms + ds).all { it in '0'..'9' }) return null
            val y = ys.toInt()
            val m = ms.toInt()
            val d = ds.toInt()
            if (m !in 1..12 || d < 1 || d > daysInMonth(y, m)) return null
            return CalendarDay(y, m, d)
        }

        internal fun daysInMonth(year: Int, month: Int): Int =
            dayNumberOf(year, month + 1, 1) - dayNumberOf(year, month, 1)

        // Civil date arithmetic (H. Hinnant's days_from_civil / civil_from_days), identical to iOS.

        private val EPOCH_OFFSET = daysFromCivil(2001, 1, 1)

        private fun dayNumberOf(year: Int, month: Int, day: Int): Int {
            val monthIndex = month - 1
            val y = year + Math.floorDiv(monthIndex, 12)
            val m = Math.floorMod(monthIndex, 12) + 1
            return daysFromCivil(y, m, 1) + (day - 1) - EPOCH_OFFSET
        }

        /** Days since 1970-01-01 for a proleptic Gregorian date. */
        private fun daysFromCivil(year: Int, month: Int, day: Int): Int {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097 + doe - 719_468
        }

        private fun civilFromDays(days: Int): Triple<Int, Int, Int> {
            val z = days + 719_468
            val era = (if (z >= 0) z else z - 146_096) / 146_097
            val doe = z - era * 146_097
            val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            val y = yoe + era * 400 + if (m <= 2) 1 else 0
            return Triple(y, m, d)
        }
    }
}
