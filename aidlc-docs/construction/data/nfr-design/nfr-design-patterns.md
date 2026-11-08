# NFR Design Patterns: Unit 2 Data

**Inputs**: `data-nfr-design-plan.md` (Q1–Q4, all A), data NFR requirements and tech stack, data functional design, domain NFR design.

---

## 1. Change Signals (Q1:A)

| Port | Source of `changes` |
|---|---|
| `EntryRepository` | Room's invalidation tracking on the `entries` table, mapped to `Unit`. Any write to the table, from any code path, emits. The first emission Room sends on subscription is dropped, so subscribers only hear about real changes |
| `SettingsStore`, `PromptStateStore` | DataStore's `data` flow, narrowed to the store's own key with `distinctUntilChanged`, first value dropped |

## 2. Prompt Library Loading (Q2:A)

- `BundledPromptLibrary` reads, parses and validates `prompts.json` in its constructor, which `JournalEnvironmentFactory.create` calls at app start.
- A missing or invalid file throws during environment creation; the App unit shows the startup error screen with a "Try again" action (like iOS).
- Parsed prompts and the id map stay in memory for the process lifetime (about 100 KB).
- Parsing happens on the caller's thread at startup; the App unit creates the environment off the main thread.

## 3. Corruption Handling (Q3:A, DATA-REL-1)

| Level | Pattern |
|---|---|
| Whole preferences file unreadable | DataStore `ReplaceFileCorruptionHandler` replaces it with empty preferences and logs the error type. Settings return to defaults, so onboarding shows again; prompt state is recomputed; entries (database) are untouched |
| One stored JSON value unreadable | The store's decoder catches the error, logs the type, and returns defaults (settings) or no state (prompt state); the next save overwrites it |
| DataStore read I/O error | Caught in the `data` flow and treated as empty preferences (logged), as the DataStore guidance recommends |
| Entry row with an invalid day key | Skipped by the mapper, logged by id and error type, never deleted (DATA-E5) |
| Database write conflict (second entry for a day) | Room throws; propagates as an exception and the domain reports `StorageFailure` (DATA-E1) |

## 4. Clock Timer (Q4:A)

```
boundaries = callbackFlow {
    register receiver: TIME_SET, DATE_CHANGED, TIMEZONE_CHANGED      → trySend(Unit)
    add ProcessLifecycleOwner observer: ON_START                     → trySend(Unit)
    launch timer loop: delay until next midnight or 12:00 (recomputed from TimeSource each time) → trySend(Unit)
    awaitClose { unregister receiver; remove observer }
}.conflate()
```

- Runs only while collected (screens open); nothing remains registered afterwards (DATA-C3).
- Coroutine delays may be late if the phone sleeps; the foreground signal and time-change broadcasts cover that.
- The widget's own boundary updates are scheduled separately in the Widget unit.

## 5. Threading and Lifetimes

| Pattern | Design |
|---|---|
| Main-safe ports | Every port function switches to an injected IO dispatcher (`Dispatchers.IO` in production, a test dispatcher in tests) |
| One instance per process | One Room database and one DataStore, created by `JournalEnvironmentFactory` and held by the App container; DataStore created through a single top-level property delegate so two instances on the same file are impossible |
| Prompt state atomicity | `DataStore.edit { }` does the read-modify-write and pruning in one transaction (DATA-S5) |

## 6. Error Mapping and Logging

| Pattern | Design |
|---|---|
| Errors to the domain | Ports throw the underlying exception (Room, DataStore, I/O); the domain's `withStorage` turns it into `StorageFailure` with the class name only |
| Logging (Q8:B of NFR requirements) | `android.util.Log` with the tag `DailyJournal`; messages contain only ids, counts and exception class names. Each logging line in this unit is reviewed for this during Code Generation and listed in the code summary |

## 7. Intents and Receivers (security)

| Item | Pattern |
|---|---|
| Notification tap | `PendingIntent.getActivity` with an explicit intent to the app's main activity, `FLAG_IMMUTABLE`, data = the deep link URI |
| Reminder alarm | `PendingIntent.getBroadcast` with an explicit intent to the App unit's reminder receiver (not exported), `FLAG_IMMUTABLE`, fixed request code so re-registering replaces the previous alarm (one at a time, Q5:A) |
| Alarm call | `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, fireAt, …)` (inexact, Q4:A); cancel uses the same pending intent |
| Notification id | `day.dayNumber` (one per day) |

The reminder receiver and restart receiver are classes in the App unit; the Data unit takes the receiver class as a constructor parameter to build the explicit intent, so `:data` doesn't depend on `:app`.

## 8. Migrations and Schema

- Room database version 1, `exportSchema = true`, schema JSON written to `data/schemas/` and committed.
- No `fallbackToDestructiveMigration` anywhere.
- Each future version adds a `Migration` and a test (Robolectric `MigrationTestHelper`) moving real data from the previous version.

## 9. Testing Patterns

| Pattern | Design |
|---|---|
| Android classes in JVM tests | Robolectric; Room in-memory database built with `allowMainThreadQueries()` only in tests; DataStore on a temporary file per test |
| Runner | Kotest specs where Robolectric works under its runner; otherwise JUnit 4 + Robolectric via the JUnit Vintage engine for Android-dependent example tests (decided and recorded in Code Generation) |
| Shared fixtures | `:domain` applies `java-test-fixtures`; generators, fakes and `TestEnvironment` move to `domain/src/testFixtures/` and are used by both units' tests |
| End-to-end | The domain use cases run against the real in-memory Room database and a temporary DataStore |

## 10. Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01 (private files), 03 (logging pattern and per-line review), 05 (bound parameters), 06 (no special permissions), 10 (official libraries), 11 (explicit immutable intents, non-exported receivers, no `:app` dependency), 13 and 15 (corruption handling at every level, typed errors): compliant. Server-only rules N/A |
| Property-Based Testing | Shared generators (PBT-07); stateful repository and pruning properties (PBT-06) planned for Code Generation; seeds and shrinking as in the domain (PBT-08): compliant |
