# Logical Components: Unit 2 Data

No infrastructure components (queues, caches, circuit breakers): everything runs inside the app on one phone.

## Components

| Component | Responsibility | Lifetime |
|---|---|---|
| `JournalDatabase` (Room) | Entries table v1, exported schema, invalidation tracking | One per process |
| `EntryDao` | Bound-parameter queries: by day key, by day-number range, after a day, all; insert; update | With the database |
| `DatabaseEntryRepository` | `EntryRepository`: row ↔ entry mapping, skip unreadable rows, change signal from Room, IO dispatcher | One per process |
| Preferences DataStore `journal_preferences` | Holds `settings.v1`, `promptState.v1`, `notificationPermissionAsked.v1`; corruption handler | One per process |
| `PreferencesSettingsStore` | `SettingsStore`: iOS-shaped JSON with `org.json`, defaults on errors, change signal | One per process |
| `PreferencesPromptStateStore` | `PromptStateStore`: JSON map, atomic save and prune, change signal | One per process |
| `NotificationPermissionFlag` | Reads and writes `notificationPermissionAsked.v1` (written by the App unit after the dialog) | One per process |
| `BundledPromptLibrary` | Loads and validates `prompts.json` at creation; id map | One per process |
| `SystemTimeSource` | System clock and the current zone on every read | One per process |
| `ClockEvents` | Boundary flow: timer, time-change broadcasts, foreground signal | One per process; listens only while collected |
| `AndroidReminderScheduler` | `ReminderScheduler`: authorization mapping, one inexact alarm for the next reminder, explicit immutable intents | One per process |
| `ReminderNotifier` | Channel, notification content, tap intent | One per process |
| `JournalEnvironmentFactory` | Builds everything above plus `JournalUseCases`; `createInMemory` for tests | Called once by the App |
| `JournalEnvironment` | Holds the use cases, `ClockEvents` and change flows for the App container and widget | One per process |
| `JournalEnvironmentProvider` | Interface the Application implements | n/a |

## Connections

```
App (Application.onCreate, off the main thread)
  -> JournalEnvironmentFactory.create(context, widgetRefresher, onBackgroundFailure, receiver classes)
       -> JournalDatabase -> EntryDao -> DatabaseEntryRepository ----\
       -> DataStore -> PreferencesSettingsStore, PreferencesPromptStateStore, NotificationPermissionFlag
       -> BundledPromptLibrary (load + validate now; failure -> startup error)
       -> SystemTimeSource, ClockEvents
       -> AndroidReminderScheduler (+ ReminderNotifier)
       -> JournalUseCases(all of the above)  <---------------------/
  <- JournalEnvironment (use cases, clock events, change flows)

Screens: change flows + ClockEvents.boundaries -> re-run use cases
Widget / receivers: JournalEnvironmentProvider -> same JournalEnvironment
```

## Test Support (shared through `domain/src/testFixtures/`)

Generators, fakes, `FixedTimeSource`, `TestEnvironment`, reference implementations, timing helper — moved from `domain/src/test/support/` so the data tests can reuse them.
