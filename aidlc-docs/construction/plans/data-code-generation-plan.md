# Code Generation Plan: Unit 2 Data

**This plan is the single source of truth for the Data unit's Code Generation.** Steps run in order; each box is ticked as soon as its step is done.

## Unit Context

| Item | Value |
|---|---|
| Workspace root | `/Users/mmomo/Code/AIDLC/daily-journal-android` |
| Module | `:data`, Android library, package `com.mmomo.dailyjournal.data` |
| Depends on | `:domain` (ports, entities, policies, `JournalUseCases`) |
| Provides to App and Widget | `JournalEnvironmentFactory`, `JournalEnvironment`, `JournalEnvironmentProvider`, `ClockEvents`, `ReminderNotifier`, `NotificationPermissionFlag` |
| Storage owned | Room table `entries` (v1); DataStore `journal_preferences` with `settings.v1`, `promptState.v1`, `notificationPermissionAsked.v1`; asset `prompts.json` |
| Design inputs | `construction/data/functional-design/`, `nfr-requirements/`, `nfr-design/`; domain contracts |

## Stories Covered (data parts)

- [x] US-1.1 (same 600 prompts as iOS), US-1.2, US-1.3 (prompt state persists)
- [x] US-1.4, US-7.1, US-7.2 (settings persist)
- [x] US-2.1, US-2.2, US-2.3 (entries stored, one per day, failures)
- [x] US-2.6 (private storage only)
- [x] US-3.1, US-3.2 (lists, ranges, search)
- [x] US-4.1 (entry days for the streak)
- [x] US-6.1, US-6.2, US-6.3, US-6.4 (permission state, scheduling, notifications)
- [x] US-2.2, US-4.3, US-5.3 (clock boundaries)

---

## Steps

### Part A: Build setup

- [x] **Step 1. Resolve versions.** Latest stable releases of Room, KSP (compatible with Kotlin 2.4.21), DataStore Preferences, AndroidX Core, Lifecycle (process), Robolectric (with API 37 support), AndroidX Test Core, JUnit 4 and the JUnit Vintage engine (only if needed, Step 13). Add them to the version catalog.
- [x] **Step 2. Shared test fixtures in `:domain`.** Apply `java-test-fixtures` to `:domain` and move the reusable test support (generators, fakes, `FixedTimeSource`, `TestEnvironment`, time helpers, references, timing, iOS fixtures) from `domain/src/test/.../support/` to `domain/src/testFixtures/...`. Domain tests must still pass unchanged (113).
- [x] **Step 3. `data/build.gradle.kts`.** KSP and the Room Gradle plugin (schema directory `data/schemas/`), dependencies, Robolectric settings (`unitTests.isIncludeAndroidResources = true`), JUnit Platform, test fixtures from `:domain`.
- [x] **Step 4. Prompt file.** Copy `prompts.json` from the iOS repository (read-only) to `data/src/main/assets/prompts.json`.

### Part B: Data code (`data/src/main/kotlin/com/mmomo/dailyjournal/data/`)

- [x] **Step 5. Database.** `database/EntryEntity.kt` (columns from the data model, unique `day_key`, index on `day_number`), `database/EntryDao.kt` (bound-parameter queries, insert, update), `database/JournalDatabase.kt` (version 1, exported schema, no destructive fallback).
- [x] **Step 6. Entry repository.** `database/DatabaseEntryRepository.kt`: mapping, skip unreadable rows with logging (ids and error types only), DATA-E1 insert-or-update with the "different id on the same day fails" rule, ranges, search through the domain matcher, Room invalidation `changes` (first value dropped), IO dispatcher.
- [x] **Step 7. JSON codecs.** `stores/SettingsJson.kt`, `stores/PromptStateJson.kt` with `org.json`: iOS shapes, defaults for missing fields, unknown fields ignored, never throwing on bad input.
- [x] **Step 8. Preferences.** `stores/JournalPreferences.kt` (single DataStore with `ReplaceFileCorruptionHandler`), `stores/PreferencesSettingsStore.kt`, `stores/PreferencesPromptStateStore.kt` (atomic save and prune), `stores/NotificationPermissionFlag.kt`; change flows narrowed to each key.
- [x] **Step 9. Prompt library.** `prompts/BundledPromptLibrary.kt`: load at construction, `version == 1`, map categories, validate, id map.
- [x] **Step 10. Clock.** `system/SystemTimeSource.kt`, `system/ClockEvents.kt` (timer to the next midnight or 12:00, time-change broadcasts, foreground signal; listens only while collected), plus a pure `nextBoundary(now, zone)` function for tests.
- [x] **Step 11. Reminders.** `reminders/AndroidReminderScheduler.kt` (authorization mapping per DATA-R1, one inexact alarm for the first planned reminder, cancel, explicit immutable intent to a receiver class passed in), `reminders/ReminderNotifier.kt` (channel, content, per-day id, explicit immutable tap intent to an activity class passed in), `reminders/ReminderIntents.kt` (extras for the reminder's day and text).
- [x] **Step 12. Wiring.** `composition/JournalEnvironment.kt`, `composition/JournalEnvironmentProvider.kt`, `composition/JournalEnvironmentFactory.kt` (`create(context, widgets, onBackgroundFailure, reminderReceiver, mainActivity)` and `createInMemory(...)`).

### Part C: Data tests (`data/src/test/kotlin/com/mmomo/dailyjournal/data/`)

- [x] **Step 13. Test runner check.** Run one Robolectric test under Kotest's JUnit Platform runner. If it isn't reliable, use JUnit 4 + Robolectric through the JUnit Vintage engine for Android-dependent tests, keep Kotest for pure tests, and record the choice.
- [x] **Step 14. Example-based tests.** One per row of the data story mapping: prompt file (600 prompts, 150 per category, ids, same as the iOS file), settings (round trip, defaults, corrupt value, unknown fields, iOS JSON sample), prompt state (save, prune, concurrent saves, iOS JSON sample, corrupt value), repository (insert, update, different id same day fails, ranges, after, search, skip unreadable row, change signal), authorization mapping, notification content and tap intent, alarm registration and cancel, clock boundary computation (midnight, noon, DST days), and the domain use cases end to end on Room and DataStore.
- [x] **Step 15. Property-based tests.** Every property in the data business logic model §8: row, settings and prompt state round-trips; decoding arbitrary text never throws; ranges and search equal filtering all entries; stateful repository saves against a map model; pruning; clock boundaries.
- [x] **Step 16. Run everything locally.** `./gradlew build koverXmlReport koverHtmlReport` with warnings as errors; record test counts and coverage; review every log line in `:data` for journal text (DATA-SEC-2) and list them in the summary.

### Part D: Supply chain, docs, CI

- [x] **Step 17. Dependency verification.** Regenerate `gradle/verification-metadata.xml` with `--no-configuration-cache`; add Linux variants of any platform-specific artifacts (checksums cross-checked against the repository's published SHA-1); confirm a clean build.
- [x] **Step 18. Documentation.** `aidlc-docs/construction/data/code/code-summary.md` (files, versions, runner decision, tests, coverage, log line review, compliance); README updates.
- [x] **Step 19. Commit and push, then check CI.** *(Done: `Build and test` passes at `068fb99` after three CI fixes recorded in the code summary.)* Commit to `main`, push, and confirm `Build and test` passes (`Submit dependencies` stays failing until the dependency graph setting is switched on).

## Not Applicable Steps

| Template step | Reason |
|---|---|
| API layer | No network APIs |
| Frontend components | App and Widget units |
| Database migration scripts | Schema version 1; the exported schema is the baseline for future migrations |
| Deployment artifacts | Personal install; APK in the App unit |

## Expected Scope

About 18 main source files and 12 test files in `:data`, the moved test fixtures in `:domain`, build and catalog changes, `prompts.json`, the Room schema JSON.
