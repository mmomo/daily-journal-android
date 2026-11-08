# Code Generation Plan: Unit 1 Domain (and project setup)

**This plan is the single source of truth for the Domain unit's Code Generation.** Steps run in order; each box is ticked as soon as its step is done.

## Unit Context

| Item | Value |
|---|---|
| Workspace root | `/Users/mmomo/Code/AIDLC/daily-journal-android` (greenfield) |
| Module | `:domain`, Kotlin JVM library, package `com.mmomo.dailyjournal.domain` |
| Also in this unit | Project setup for all four modules, CI workflow (`unit-of-work.md`, Q1:A) |
| Depends on | Nothing in the project |
| Provides to Data, App, Widget | Entities, policies, ports, `JournalUseCases`, `DeepLink`, `JournalError`, Gradle build, version catalog, CI |
| Database entities owned | None (storage is the Data unit's); the domain defines the `EntryRepository`, `PromptStateStore`, `SettingsStore` contracts |
| Design inputs | `construction/domain/functional-design/`, `nfr-requirements/`, `nfr-design/`; `inception/application-design/` (with refinements recorded in the functional design §8 and NFR design) |
| Environment | Android Studio and SDK installed (platform API 37, build tools 36); JDK 21 on the PATH; no Gradle installed (the wrapper is bootstrapped in Step 2) |

## Stories Covered (business-rule parts)

- [x] US-1.1, US-1.2, US-1.3, US-1.4 (prompt selection, refresh, categories)
- [x] US-2.1, US-2.2, US-2.3, US-2.4, US-2.5 (save, midnight grace, backfill, editability, Today state)
- [x] US-3.1, US-3.2, US-3.3 (list, search, export)
- [x] US-4.1, US-4.2, US-4.3, US-4.4 (streak)
- [x] US-5.1, US-5.2, US-5.3 (Today state for the widget, deep links, day boundaries)
- [x] US-6.1, US-6.2, US-6.3, US-6.4 (reminder configuration and planning)
- [x] US-7.1, US-7.2 (onboarding and settings use cases)

US-2.6 has no domain part. Each story is ticked when its domain code and tests are complete.

---

## Steps

### Part A: Project setup

- [x] **Step 1. Resolve versions.** Look up the latest stable releases (tech stack decisions, "use latest") on the official release sources: Gradle, Android Gradle Plugin, Kotlin, kotlinx-coroutines, kotlinx-datetime, Kotest, Kover; and the commit SHAs of the GitHub Actions used in CI. Check compatibility (AGP ↔ Gradle, Kotest ↔ Kotlin). Record the chosen versions in the code summary.
- [x] **Step 2. Gradle wrapper.** Download the chosen Gradle distribution once (checksum verified against gradle.org) into a temporary folder outside the repo, run `gradle wrapper` to create `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and `gradle-wrapper.properties` with `distributionSha256Sum`.
- [x] **Step 3. Root build files.** `settings.gradle.kts` (modules `:domain`, `:data`, `:app`, `:widget`; repositories `google()` and `mavenCentral()` declared only here, module repositories forbidden), root `build.gradle.kts` (plugins declared with `apply false`, Kover aggregation, warnings as errors for every Kotlin compilation, Java 17 bytecode), `gradle.properties` (JVM memory, AndroidX, configuration cache), `gradle/libs.versions.toml` (every version pinned).
- [x] **Step 4. Module skeletons.**
  - `domain/build.gradle.kts`: Kotlin JVM library; coroutines and datetime; Kotest, coroutines-test on the JUnit Platform
  - `data/`, `widget/`: Android library skeletons (`minSdk` 29, `compileSdk` latest installed, namespace `com.mmomo.dailyjournal.data` / `.widget`), `AndroidManifest.xml`, depending on `:domain` (and `:data` for widget)
  - `app/`: Android application skeleton (`applicationId com.mmomo.dailyjournal`, `minSdk` 29, `targetSdk` latest), minimal manifest with backup off and no permissions; no screens yet (App unit)
- [x] **Step 5. First build.** `./gradlew build` succeeds with the empty modules (proves wrapper, plugins, SDK and catalog work).

### Part B: Domain code (`domain/src/main/kotlin/com/mmomo/dailyjournal/domain/`)

- [x] **Step 6. Time.** `time/CalendarDay.kt` (2001-based `dayNumber`, `key`, `fromKey`, `adding`, `containing`, `startIn`, `noonIn`, normalizing constructor), `time/DayMath.kt` (instant for a day and hour:minute with DST gap handling), `time/StableHash.kt` (FNV-1a 64 over UTF-8, `gcd`).
- [x] **Step 7. Entities.** `entities/`: `JournalEntry` (with `MAX_LENGTH`), `Prompt`, `PromptCategory` (+ `categoryKey`), `DailyPromptState`, `StreakSummary`/`StreakStatus`, `EntryEditability`, `BackfillOffer`, `EntryDetail`, `TodayState`, `AppSettings`, `ReminderSettings` (hour, minute), `ScheduledReminder`, `ReminderAuthorization`, `ExportFormat`/`ExportDocument`, `DeepLink` (strict `parse`), `JournalError` (all nine cases).
- [x] **Step 8. Policies.** `policies/`: `TextValidation`, `PromptSelector` (exact iOS algorithm, three-level refresh relaxation), `EntryEditPolicy` (+ `lateSaveAllowed`, 5-second grace), `StreakCalculator`, `EntrySearchMatcher` (NFD, marks removed, case-folded), `JournalExporter` (iOS layouts), `ReminderPlanner` (7-day window), `PromptLibraryValidator`.
- [x] **Step 9. Ports.** `ports/`: `TimeSource`, `EntryRepository` (with `changes`), `PromptLibrary`, `PromptStateStore`, `SettingsStore`, `ReminderScheduler`, `WidgetRefresher`; each documented as main-safe.
- [x] **Step 10. Use cases.** `usecases/`: `Support.kt` (`withStorage`, `JournalWriteLock`, prompt resolver), the 14 use cases (with `SaveTodayEntryUseCase(text, day, editedAt)`, `ConfigureReminderUseCase` throwing `NotificationsDenied`), and `JournalUseCases` (ports, `onBackgroundFailure`, one shared write lock).

### Part C: Domain tests (`domain/src/test/kotlin/com/mmomo/dailyjournal/domain/`)

- [x] **Step 11. Test support.** `support/`: `FixedTimeSource`, in-memory fakes (including a failing repository), a small prompt library fixture and a 600-prompt-shaped generator, time helpers, `Arb` generators (days 2024–2032, the 8 time zones, biased instants, category sets, texts including empty, whitespace, accented, emoji, long and over-limit, entry-day sets, queries), reference implementations (longest run, simple search), timing helper.
- [x] **Step 12. iOS parity values.** Copy expected values from the iOS tests (hash values, editability and streak scenarios, export output) and produce a handful of daily picks and refresh picks by running the iOS selector once from the iOS repository (read-only; nothing is changed there). Store them in `support/IosParityFixtures.kt`.
- [x] **Step 13. Example-based tests.** One test per row of the scenario-to-test mapping in `business-rules.md` (US-1.1 … US-7.1, future-dated entries, midnight grace, text too long, misuse, failing storage, deep links), plus `CalendarDay`, `DayMath` (DST New York 2026-11-01, gap days) and iOS parity checks.
- [x] **Step 14. Property-based tests.** `property/`: every property in `business-logic-model.md` §10.1 (round-trips, invariants, idempotence, the streak and search oracles, stateful refresh and save sequences), random seed per run, shrinking on.
- [x] **Step 15. Performance smoke tests.** DOM-PERF-1 to 3 with warm-up and median timing.
- [x] **Step 16. Run all tests locally.** `./gradlew build` (warnings as errors) and `./gradlew koverHtmlReport`; fix any failure; record test counts and coverage in the summary.

### Part D: Supply chain, CI and documentation

- [x] **Step 17. Dependency verification.** Generate `gradle/verification-metadata.xml` with SHA-256 for every resolved artifact (`--write-verification-metadata sha256`), review it, and confirm a clean build passes with verification on.
- [x] **Step 18. CI workflow.** `.github/workflows/ci.yml`: push and pull request triggers; fixed Ubuntu runner; `build-and-test` job (wrapper validation, Temurin 17, Gradle setup, build and tests, Kover report, upload reports; `contents: read`); `dependency-submission` job (`contents: write`); every action pinned by commit SHA.
- [x] **Step 19. Documentation.** `aidlc-docs/construction/domain/code/code-summary.md` (files created, versions chosen, test counts, coverage, design refinements applied, compliance); update `README.md` (build and test commands, Dependabot setting to switch on).
- [ ] **Step 20. Commit and push, then check CI.** Commit the project setup and domain to `main` on GitHub (public repository) and confirm both CI jobs pass. If CI fails, fix and push again.

## Not Applicable Steps (from the standard template)

| Template step | Reason |
|---|---|
| API layer generation and tests | No network APIs |
| Repository layer generation and tests | Data unit |
| Frontend components | App and Widget units |
| Database migration scripts | Data unit (no storage here) |
| Deployment artifacts | Personal install; the APK comes from later units. CI is created here |

## Expected Scope

About 30 main source files and 25 test files in `:domain`, plus build files, the wrapper, verification metadata and the CI workflow.
