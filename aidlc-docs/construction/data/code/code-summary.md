# Code Summary: Unit 2 Data

**Plan**: `aidlc-docs/construction/plans/data-code-generation-plan.md`

## Versions Added (Step 1, latest stable at 2026-11-07)

| Library | Version | Scope |
|---|---|---|
| Room (runtime, ktx, compiler, testing) + Room Gradle plugin | 2.8.5 | `:data` |
| KSP | 2.3.13 | Room code generation |
| DataStore Preferences | 1.3.0 | `:data` |
| AndroidX Core (ktx) | 1.19.1 | `:data` (notifications, permissions) |
| AndroidX Lifecycle (process) | 2.11.0 | `:data` (foreground signal) |
| Robolectric | 4.17 | Tests only; runs on simulated SDK 36 (its newest) |
| AndroidX Test Core (ktx) | 1.7.0 | Tests only |
| JUnit 4 | 4.13.2 | Tests only (Robolectric runner) |
| JUnit Vintage engine | 5.13.4 | Tests only; matches JUnit Platform 1.13.4 used by Kotest 6.2.6 (the 6.x line needs Platform 6) |

## Files Created or Changed

**Build**: `gradle/libs.versions.toml` (new versions, libraries, plugins), root `build.gradle.kts` (KSP and Room plugins, Kover includes `:data`), `data/build.gradle.kts` (Room schema directory, Robolectric settings, JVM flags, test fixtures), `gradle/verification-metadata.xml` (424 components).

**Domain (test code only)**: `domain/build.gradle.kts` applies `java-test-fixtures`; test support moved from `domain/src/test/.../support/` to `domain/src/testFixtures/.../support/`. Fixed the category generator (it sometimes failed to produce 4 distinct categories; it now picks one of the 15 combinations by bit mask). All 113 domain tests still pass.

**Data main code** (`data/src/main/kotlin/com/mmomo/dailyjournal/data/`, 18 files):
- `database/`: `EntryEntity`, `EntryDao`, `JournalDatabase` (v1, schema exported to `data/schemas/…/1.json`), `DatabaseEntryRepository`
- `stores/`: `SettingsJson`, `PromptStateJson` (iOS shapes, `org.json`), `JournalPreferences` (single DataStore, corruption handler, safe reads), `PreferencesSettingsStore`, `PreferencesPromptStateStore`, `NotificationPermissionFlag`
- `prompts/BundledPromptLibrary` (with `PromptFileException`)
- `system/`: `SystemTimeSource`, `ClockEvents` (with the pure `nextBoundary`)
- `reminders/`: `AndroidReminderScheduler`, `ReminderNotifier`, `ReminderIntents`
- `composition/`: `JournalEnvironment` (with `JournalEnvironmentProvider`), `JournalEnvironmentFactory`

**Data resources**: `data/src/main/assets/prompts.json` (identical to the iOS file; SHA-256 checked in a test), `data/src/main/AndroidManifest.xml` (`POST_NOTIFICATIONS`).

**Data tests** (`data/src/test/kotlin/…`, 10 files): `support/DataTestSupport` (temporary DataStore and database, test receiver and activity, iOS JSON samples and prompt hash), `PromptFileTest`, `JsonCodecTest`, `StoresTest`, `RepositoryTest`, `RemindersTest`, `EndToEndTest`, `ClockBoundaryTest`, `property/StoragePropertyTest`, `property/ClockPropertyTest`.

## Test Runner Decision (Step 13)

A trial Kotest spec couldn't get an Android `Context`: Kotest's runner doesn't set up Robolectric. So tests that need Android (Room, DataStore, `org.json`, notifications, alarms) are **JUnit 4 + Robolectric** tests run on the JUnit Platform through the Vintage engine, and use Kotest assertions and Kotest's `checkAll` for properties (seed printed on failure as before). Pure tests (clock boundaries) stay Kotest specs. Robolectric also needs the test JVM flags `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` and `--add-opens=java.base/java.io=ALL-UNNAMED` on recent JDKs.

## Results (local, Step 16)

| Module | Tests | Result |
|---|---|---|
| Domain | 113 | All pass |
| Data | 43 (35 example-based, 8 property-based) | All pass |
| **Total** | **156** | **All pass** |

- Full `./gradlew build` with warnings as errors and Android Lint: passes.
- Coverage (Kover, report only): `:data` lines 86.6%, branches 81.5%; whole project lines 93.5%, branches 85.8%.
- iOS parity: settings and prompt-state JSON produced by the iOS code decode to the same values; the prompt file matches the iOS file byte for byte; the end-to-end test gets the same daily pick and first refresh as iOS through real storage.

## Log Line Review (DATA-SEC-2)

| File | Message |
|---|---|
| `database/DatabaseEntryRepository.kt` | "Skipping unreadable entry {row id}: InvalidDayKey" |
| `stores/PreferencesSettingsStore.kt` | "Stored settings are unreadable; using defaults" |
| `stores/PreferencesPromptStateStore.kt` | "Stored prompt state is unreadable; starting fresh" |
| `stores/JournalPreferences.kt` | "Preferences file was corrupted and has been reset: {exception class}" |
| `stores/JournalPreferences.kt` | "Preferences read failed: {exception class}" |

None can contain entry text, prompt text or settings values. A test also checks that a skipped row's text never reaches the log.

## Fixes and Refinements During Generation

| Change | Why |
|---|---|
| `POST_NOTIFICATIONS` declared in the data module's manifest | Android Lint flagged that `ReminderNotifier` posts notifications without it; the data module owns notification posting |
| `ClockEvents` adds and removes its lifecycle observer through the main looper | `Dispatchers.Main` isn't available in this module, and a coroutine launched during close might never run |
| Alarm assertions use the registered `PendingIntent` looked up like Android does | Robolectric 4.17's `ScheduledAlarm.operation` is deprecated with no replacement getter |
| Domain test support moved to test fixtures; category generator fixed | Shared generators for data tests (PBT-07); the old generator was flaky |

## CI (Step 19)

| Commit | Build and test |
|---|---|
| `2d234ed` Data unit | Failed: `guava-parent-33.2.1-jre.pom` checksum missing (KSP classpath on Linux) |
| `6111bff` Add that checksum | Failed: two more parent POMs missing |
| `dd7e8d6` Regenerate with `--refresh-dependencies` | Verification passed; Robolectric failed on Java 17 |
| next | CI switched to Java 21 (see below) |

- Root cause of the missing POMs: metadata cached by earlier lenient builds wasn't re-fetched, so `--write-verification-metadata` never saw it. Regenerating with `--refresh-dependencies` recorded everything; the README command now includes the flag.
- Robolectric's simulated Android 15+ requires Java 21 to run. CI now uses Temurin 21 (local builds already used 21); compiled code still targets Java 17 bytecode. Recorded in the Domain tech stack decisions.

## Known Limitations

- **Robolectric runtime download**: Robolectric fetches its simulated Android runtime from Maven Central by itself at test time, outside Gradle's checksum list. Test-only; nothing from it ships in the app.
- **Dependency graph**: CI's `Submit dependencies` job keeps failing until the repository's dependency graph is switched on (open since the Domain unit).

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01 (private storage), 03 (log lines reviewed, test for no text), 05 (Room bound parameters), 06 (only the notification permission), 10 (official libraries, pinned, verified), 11 (explicit immutable intents, receiver and activity passed in), 13 and 15 (corruption handling, defensive JSON, validated prompt file): compliant. Server-only rules N/A |
| Property-Based Testing | PBT-02 (row, settings and prompt-state round-trips), 03 (pruning, clock and decoding invariants), 05 (ranges and search vs filtering everything), 06 (stateful repository saves), 07 (shared domain generators), 08 (seeds printed, shrinking on), 09 (Kotest), 10 (example and property tests in separate files): compliant |

## Stories (data parts) Completed

US-1.1 to 1.4, US-2.1 to 2.3, US-2.6, US-3.1, US-3.2, US-4.1, US-5.3 (clock), US-6.1 to 6.4, US-7.1, US-7.2.
