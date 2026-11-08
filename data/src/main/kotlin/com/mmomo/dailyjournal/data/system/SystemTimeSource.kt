package com.mmomo.dailyjournal.data.system

import com.mmomo.dailyjournal.domain.ports.TimeSource
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** System clock and the **current** system time zone, read on every access (DATA-C1). */
class SystemTimeSource : TimeSource {
    override val now: Instant get() = Clock.System.now()
    override val timeZone: TimeZone get() = TimeZone.currentSystemDefault()
}
