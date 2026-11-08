package com.mmomo.dailyjournal.domain.support

import kotlin.time.Duration
import kotlin.time.measureTime

/** Median duration of [runs] executions after [warmUp] untimed ones (NFR design §7). */
inline fun medianTime(warmUp: Int = 5, runs: Int = 9, block: () -> Unit): Duration {
    repeat(warmUp) { block() }
    val times = (1..runs).map { measureTime(block) }.sorted()
    return times[times.size / 2]
}
