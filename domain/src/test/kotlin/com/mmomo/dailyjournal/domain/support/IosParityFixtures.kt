package com.mmomo.dailyjournal.domain.support

/**
 * Values produced by the iOS `JournalDomain` code (run once from the iOS repository, read-only) for the
 * real 600-prompt library. Android must reproduce them exactly (FR-1.4, domain functional design Q8:B).
 * Generated during Domain Code Generation, step 12; do not edit by hand.
 */
object IosParityFixtures {
    /** FNV-1a 64-bit values (unsigned, decimal). */
    val hashes: List<Pair<String, ULong>> = listOf(
        "" to 14695981039346656037uL,
        "a" to 12638187200555641996uL,
        "offset|gratitude" to 7366296299021212081uL,
        "step|creativity,goals,gratitude,reflection" to 16248264171361518033uL,
        "café ☕ 日記" to 1519610188335775654uL,
    )

    /** Daily pick with nothing answered recently: category key, day key, expected prompt id. */
    val dailyPicks: List<Triple<String, String, String>> = listOf(
        Triple("gratitude", "2000-12-31", "gratitude-085"),
        Triple("gratitude", "2001-01-01", "gratitude-032"),
        Triple("gratitude", "2024-02-29", "gratitude-055"),
        Triple("gratitude", "2026-10-08", "gratitude-149"),
        Triple("gratitude", "2026-10-09", "gratitude-096"),
        Triple("gratitude", "2032-12-31", "gratitude-121"),
        Triple("reflection", "2000-12-31", "reflection-097"),
        Triple("reflection", "2001-01-01", "reflection-128"),
        Triple("reflection", "2024-02-29", "reflection-007"),
        Triple("reflection", "2026-10-08", "reflection-119"),
        Triple("reflection", "2026-10-09", "reflection-150"),
        Triple("reflection", "2032-12-31", "reflection-025"),
        Triple("gratitude,reflection", "2000-12-31", "reflection-064"),
        Triple("gratitude,reflection", "2001-01-01", "reflection-057"),
        Triple("gratitude,reflection", "2024-02-29", "gratitude-094"),
        Triple("gratitude,reflection", "2026-10-08", "gratitude-030"),
        Triple("gratitude,reflection", "2026-10-09", "gratitude-023"),
        Triple("gratitude,reflection", "2032-12-31", "reflection-148"),
        Triple("goals", "2000-12-31", "goals-054"),
        Triple("goals", "2001-01-01", "goals-107"),
        Triple("goals", "2024-02-29", "goals-084"),
        Triple("goals", "2026-10-08", "goals-140"),
        Triple("goals", "2026-10-09", "goals-043"),
        Triple("goals", "2032-12-31", "goals-018"),
        Triple("goals,gratitude", "2000-12-31", "gratitude-029"),
        Triple("goals,gratitude", "2001-01-01", "goals-010"),
        Triple("goals,gratitude", "2024-02-29", "gratitude-089"),
        Triple("goals,gratitude", "2026-10-08", "gratitude-001"),
        Triple("goals,gratitude", "2026-10-09", "gratitude-132"),
        Triple("goals,gratitude", "2032-12-31", "goals-107"),
        Triple("goals,reflection", "2000-12-31", "reflection-079"),
        Triple("goals,reflection", "2001-01-01", "goals-120"),
        Triple("goals,reflection", "2024-02-29", "reflection-139"),
        Triple("goals,reflection", "2026-10-08", "goals-021"),
        Triple("goals,reflection", "2026-10-09", "reflection-062"),
        Triple("goals,reflection", "2032-12-31", "goals-037"),
        Triple("goals,gratitude,reflection", "2000-12-31", "reflection-038"),
        Triple("goals,gratitude,reflection", "2001-01-01", "reflection-001"),
        Triple("goals,gratitude,reflection", "2024-02-29", "goals-068"),
        Triple("goals,gratitude,reflection", "2026-10-08", "reflection-094"),
        Triple("goals,gratitude,reflection", "2026-10-09", "reflection-057"),
        Triple("goals,gratitude,reflection", "2032-12-31", "reflection-032"),
        Triple("creativity", "2000-12-31", "creativity-086"),
        Triple("creativity", "2001-01-01", "creativity-133"),
        Triple("creativity", "2024-02-29", "creativity-056"),
        Triple("creativity", "2026-10-08", "creativity-100"),
        Triple("creativity", "2026-10-09", "creativity-147"),
        Triple("creativity", "2032-12-31", "creativity-122"),
        Triple("creativity,gratitude", "2000-12-31", "creativity-003"),
        Triple("creativity,gratitude", "2001-01-01", "gratitude-146"),
        Triple("creativity,gratitude", "2024-02-29", "gratitude-033"),
        Triple("creativity,gratitude", "2026-10-08", "creativity-119"),
        Triple("creativity,gratitude", "2026-10-09", "creativity-112"),
        Triple("creativity,gratitude", "2032-12-31", "creativity-087"),
        Triple("creativity,reflection", "2000-12-31", "reflection-127"),
        Triple("creativity,reflection", "2001-01-01", "creativity-086"),
        Triple("creativity,reflection", "2024-02-29", "reflection-067"),
        Triple("creativity,reflection", "2026-10-08", "reflection-035"),
        Triple("creativity,reflection", "2026-10-09", "reflection-144"),
        Triple("creativity,reflection", "2032-12-31", "reflection-019"),
        Triple("creativity,gratitude,reflection", "2000-12-31", "reflection-082"),
        Triple("creativity,gratitude,reflection", "2001-01-01", "gratitude-021"),
        Triple("creativity,gratitude,reflection", "2024-02-29", "creativity-022"),
        Triple("creativity,gratitude,reflection", "2026-10-08", "gratitude-150"),
        Triple("creativity,gratitude,reflection", "2026-10-09", "creativity-089"),
        Triple("creativity,gratitude,reflection", "2032-12-31", "gratitude-064"),
        Triple("creativity,goals", "2000-12-31", "creativity-130"),
        Triple("creativity,goals", "2001-01-01", "goals-107"),
        Triple("creativity,goals", "2024-02-29", "goals-100"),
        Triple("creativity,goals", "2026-10-08", "goals-104"),
        Triple("creativity,goals", "2026-10-09", "creativity-081"),
        Triple("creativity,goals", "2032-12-31", "creativity-106"),
        Triple("creativity,goals,gratitude", "2000-12-31", "creativity-115"),
        Triple("creativity,goals,gratitude", "2001-01-01", "gratitude-092"),
        Triple("creativity,goals,gratitude", "2024-02-29", "gratitude-085"),
        Triple("creativity,goals,gratitude", "2026-10-08", "gratitude-089"),
        Triple("creativity,goals,gratitude", "2026-10-09", "goals-066"),
        Triple("creativity,goals,gratitude", "2032-12-31", "gratitude-091"),
        Triple("creativity,goals,reflection", "2000-12-31", "reflection-009"),
        Triple("creativity,goals,reflection", "2001-01-01", "reflection-040"),
        Triple("creativity,goals,reflection", "2024-02-29", "goals-069"),
        Triple("creativity,goals,reflection", "2026-10-08", "creativity-031"),
        Triple("creativity,goals,reflection", "2026-10-09", "creativity-062"),
        Triple("creativity,goals,reflection", "2032-12-31", "reflection-087"),
        Triple("creativity,goals,gratitude,reflection", "2000-12-31", "gratitude-004"),
        Triple("creativity,goals,gratitude,reflection", "2001-01-01", "gratitude-041"),
        Triple("creativity,goals,gratitude,reflection", "2024-02-29", "creativity-124"),
        Triple("creativity,goals,gratitude,reflection", "2026-10-08", "reflection-098"),
        Triple("creativity,goals,gratitude,reflection", "2026-10-09", "reflection-135"),
        Triple("creativity,goals,gratitude,reflection", "2032-12-31", "goals-010"),
    )

    /** All categories, 2026-10-08: the daily pick, and the pick when that prompt was answered recently. */
    const val DAILY_ALL_2026_10_08 = "reflection-098"
    const val DAILY_ALL_2026_10_08_SKIPPING_ANSWERED = "reflection-135"

    /** All categories, 2026-10-08: refreshes 1, 2, 3, each excluding the prompts shown before it. */
    val refreshesAll20261008: List<String> = listOf("reflection-125", "gratitude-064", "goals-003")

    /** Gratitude only, 2026-10-08: refresh 1 after the daily pick. */
    const val REFRESH_GRATITUDE_2026_10_08 = "gratitude-002"

    /** America/New_York instants as epoch seconds. */
    const val NEW_YORK_NOON_2026_03_08 = 1772985600L
    const val NEW_YORK_0230_IN_GAP_2026_03_08 = 1772953200L
    const val NEW_YORK_START_2026_11_01 = 1793505600L
    const val DAY_NUMBER_2026_10_08 = 9411
}
