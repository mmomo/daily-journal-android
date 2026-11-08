# Code Summary: Unit 1 Domain (and project setup)

**Plan**: `aidlc-docs/construction/plans/domain-code-generation-plan.md` (all steps done)

## Versions Chosen (Step 1, "use latest" stable at 2026-11-07)

| Component | Version | Notes |
|---|---|---|
| Gradle | 9.9.0 | Wrapper with `distributionSha256Sum`; checksum checked against gradle.org |
| Android Gradle Plugin | 9.5.1 | 9.6.0 is still alpha |
| Kotlin | 2.4.21 | 2.5.0 is still beta |
| kotlinx-coroutines | 1.11.0 | `:domain` |
| kotlinx-datetime | 0.8.0 | `:domain` (`TimeZone`; instants are `kotlin.time.Instant`) |
| Kotest | 6.2.6 | Tests only (runner, assertions, property) |
| Kover | 0.9.11 | Coverage report |
| Android SDK | compile and target API 37, min API 29 | API 37 installed locally |
| Java | 17 bytecode | Builds with JDK 17 (CI) or newer (local JDK 21) |
| GitHub Actions | checkout v7.0.1, setup-java v6.2.0, gradle/actions v6.4.1, upload-artifact v7.0.2 | Pinned by commit SHA; runner `ubuntu-24.04` |

## Files Created

**Project setup**: `settings.gradle.kts` (modules, Google Maven and Maven Central only, project repositories forbidden), `build.gradle.kts` (Kover aggregation, warnings as errors, Java 17), `gradle.properties`, `gradle/libs.versions.toml`, `gradle/verification-metadata.xml` (270 components, SHA-256), Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`), `.github/workflows/ci.yml`.

**Module skeletons**: `domain/build.gradle.kts`; `data/`, `widget/` (Android libraries, empty manifests); `app/` (application `com.mmomo.dailyjournal`, manifest with backup off and no permissions). `local.properties` (SDK path) is git-ignored.

**Domain main code** (`domain/src/main/kotlin/com/mmomo/dailyjournal/domain/`, 26 files):
- `time/`: `CalendarDay`, `DayMath`, `StableHash`
- `entities/`: `Prompt` (with `PromptCategory`, `DailyPromptState`), `JournalEntry` (with `EntryEditability`, `BackfillOffer`, `EntryDetail`), `Streak`, `TodayState`, `Settings`, `Reminders`, `Export`, `DeepLink`, `JournalError`
- `policies/`: `PromptSelector`, `EntryEditPolicy`, `StreakCalculator`, `EntrySearchMatcher`, `JournalExporter`, `ReminderPlanner`, `PromptLibraryValidator`, `TextValidation`
- `ports/Ports.kt`: `TimeSource`, `EntryRepository`, `PromptLibrary`, `PromptStateStore`, `SettingsStore`, `ReminderScheduler`, `WidgetRefresher`
- `usecases/`: `Support` (`withStorage`, `JournalWriteLock`, `AfterWrite`, `PromptResolver`), `TodayUseCases`, `EntryUseCases`, `SettingsUseCases`, `JournalUseCases`

**Domain tests** (`domain/src/test/kotlin/com/mmomo/dailyjournal/domain/`, 20 files):
- `support/`: fixed clock, fakes, test environment, generators, reference implementations, timing helper, `IosParityFixtures` (104 values produced by running the iOS selector once)
- Example-based: `time/CalendarDayTest`, `policies/*Test`, `entities/EntitiesTest`, `usecases/*Test`
- Property-based: `property/TimePropertyTest`, `PolicyPropertyTest`, `StatefulPropertyTest`
- `performance/PerformanceSmokeTest`

## Test Results (local, Step 16)

| Kind | Tests | Result |
|---|---|---|
| Example-based | 91 | All pass |
| Property-based | 19 (17 properties + 2 stateful sequences; Kotest default 1,000 iterations unless limited) | All pass |
| Performance smoke | 3 | All pass (DOM-PERF-1 to 3) |
| **Total** | **113** | **All pass** |

- **iOS parity**: all 5 hash values, 90 daily picks (6 dates × 15 category sets), the answered-recently skip, 3 refreshes, the single-category refresh, and the DST noon, gap and start instants match the iOS code exactly.
- **Coverage (Kover, report only)**: lines 96.9%, instructions 97.4%, branches 85.7%.
- **PBT-08 check**: a temporary failing property (then deleted) printed the shrunk input and "Repeat this test by using seed …".
- **Verification check**: changing one checksum made the build fail on that artifact; restored afterwards.

## Design Refinements Applied

| Refinement | Where decided |
|---|---|
| `CalendarDay.dayNumber` from 2001-01-01 | Functional design (BR-T5) |
| `SaveTodayEntryUseCase.execute(text, day, editedAt)` with 5-second late-save grace | Functional design (BR-E4, C1:A) |
| `ConfigureReminderUseCase` throws `NotificationsDenied` | Functional design §8 |
| `ReminderSettings` with hour and minute (clamped) | Functional design §8 |
| `JournalError`: added `TextTooLong`, `NotificationsDenied`, `NothingToExport` | Domain entities §6 |
| One `JournalWriteLock` for save, backfill, refresh | NFR design §3 |
| `onBackgroundFailure` callback | NFR design logical components |
| **New in code generation**: `AfterWrite` helper makes the widget refresh and reminder sync after a write best effort, so a widget failure never fails a successful save (iOS behaves the same) | Code generation, step 10 |
| **New in code generation**: `DeepLink` is an enum (iOS has an enum too) instead of a sealed interface | Code generation, step 7 |
| **New in code generation**: BR-E7's future-dated check looks after *today*, not after the saved day (a late save just after midnight must not count today's entry as "future") | Code generation, step 10 |
| **New in code generation**: DST gaps resolved with `java.time` zone rules to match iOS `.nextTime` (Kotlin's default shifts by the gap length) | Code generation, step 6 |
| **New in code generation**: dependency submission runs only on pushes to `main` (GitHub's dependency graph tracks the default branch) and with lenient verification (the action injects its own plugin) | Code generation, step 18 |

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-03 (domain doesn't log; errors carry no journal text), 05 (strict `fromKey` and `DeepLink.parse`, 20,000-character limit), 10 (pinned versions, checksum verification, wrapper checksum, central repositories, pinned CI actions and runner, Dependabot via dependency submission, no unused dependencies), 11 (misuse rules tested), 13 (checksums and wrapper validation; tamper test), 15 (typed errors, cancellation preserved, no partial writes, best-effort side effects): compliant. Server-only rules N/A |
| Property-Based Testing | PBT-02 (round-trips: day keys, start and noon, deep links), 03 (invariants with boundary values), 04 (reminder plan idempotence), 05 (streak and search oracles; iOS comparison N/A with reason), 06 (stateful refresh and save sequences), 07 (domain generators per C2:A), 08 (seed printed, shrinking on, reports kept in CI), 09 (Kotest), 10 (example and property tests in separate files; every critical scenario has an example test): compliant |

## Stories (business-rule parts) Completed

US-1.1 to 1.4, US-2.1 to 2.5, US-3.1 to 3.3, US-4.1 to 4.4, US-5.1 to 5.3, US-6.1 to 6.4, US-7.1, US-7.2 (US-2.6 has no domain part).
