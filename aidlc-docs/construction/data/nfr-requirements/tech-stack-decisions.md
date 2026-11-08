# Tech Stack Decisions: Unit 2 Data

Project-wide policy (Domain unit): latest stable versions at the start of this unit's Code Generation, pinned exactly in `gradle/libs.versions.toml`; official Kotlin, AndroidX and Google libraries only in shipped code; checksums in `gradle/verification-metadata.xml` (regenerated with `--no-configuration-cache`, Linux and macOS variants of platform-specific artifacts recorded).

| Area | Choice | Rationale |
|---|---|---|
| Entry storage | **Room** (AndroidX), with its KSP code generator; in-memory database for tests; schema JSON exported to `data/schemas/` and committed | Q1:A; compile-checked queries, change signals through `Flow`, migration testing (DATA-REL-4) |
| Settings, prompt state, permission flag | **Jetpack DataStore Preferences** (AndroidX), one private file `journal_preferences`, string values holding the iOS-shaped JSON | Q2:A; atomic `edit`, `Flow` of changes (DATA-S5, S6) |
| JSON | Android's built-in **`org.json`** (`JSONObject`, `JSONArray`); hand-written mapping with explicit defaults for missing fields and ignoring unknown ones | Q3:B; no extra library |
| Reminders | `AlarmManager` with **inexact**, battery-friendly alarms that still fire in Doze (`setAndAllowWhileIdle`), **one registered at a time** (the next planned reminder) | Q4:A, Q5:A; no special permission |
| Notifications | `NotificationManagerCompat` (AndroidX Core) and a "Daily reminder" channel | DATA-R4 |
| App foreground signal | AndroidX Lifecycle process owner (`ProcessLifecycleOwner`) | DATA-C2 |
| Coroutines | kotlinx-coroutines (already in the project), `Dispatchers.IO` inside the data layer | DATA-PERF-2 |
| Logging | Android's `Log` directly; ids, counts and error types only | Q8:B |
| Tests | Kotest (runner, assertions, property) on the JUnit Platform, **Robolectric** for Android classes, AndroidX Test Core for a test `Context`, Room's testing artifact, kotlinx-coroutines-test | Q7:A |
| Shared generators | `java-test-fixtures` on `:domain`, so `:data` tests reuse the domain generators and fakes | DATA-MAIN-3 |

**Robolectric with Kotest**: Robolectric is built for JUnit 4 runners. If running it under Kotest's JUnit Platform runner isn't reliable with the chosen versions, the Android-dependent example tests use JUnit 4 with Robolectric through the JUnit Vintage engine, while property-based tests keep using Kotest (for pure mapping code that doesn't need Android). This is decided and recorded during Code Generation.

**Versions to resolve at Code Generation**: Room, KSP (compatible with Kotlin 2.4.21), DataStore, AndroidX Core, Lifecycle, Robolectric, AndroidX Test Core, and the matching compile SDK support in Robolectric (API 37).

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01 (private storage), 03 (platform logger, no journal text; enforced by review per Q8:B), 05 (bound parameters), 06 (no special permissions), 10 (official libraries, pinned, verified), 11 (explicit immutable intents, non-exported receiver), 13 and 15 (defensive JSON parsing, validated prompt file): compliant. Server-only rules N/A |
| Property-Based Testing | PBT-09 (Kotest, already selected) and PBT-07 (shared generators): compliant |
