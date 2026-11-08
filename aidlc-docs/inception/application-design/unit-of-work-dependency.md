# Unit of Work Dependencies

## Dependency Matrix

Rows depend on columns ("X" = compile-time dependency).

| Unit \ depends on | Domain | Data | App | Widget |
|---|---|---|---|---|
| Domain | | | | |
| Data | X | | | |
| App | X | X | | X (from Unit 4 on) |
| Widget | X | X | | |

No cycles. The Widget unit depends only on Domain and Data. The App depends on Widget only to pass `GlanceWidgetRefresher` to the data factory and to package the widget; until Unit 4 exists, the App uses a no-op refresher, so Unit 3 can be completed and tested without Unit 4.

## Build Order (Q2:A)

```
1. Domain  ->  2. Data  ->  3. App  ->  4. Widget  ->  Build and Test
```

Each arrow means "fully designed, coded, tested and approved before the next unit starts".

## Interfaces Each Unit Provides

| From | To | Interface |
|---|---|---|
| Domain | Data | Port interfaces to implement (`EntryRepository`, `PromptStateStore`, `SettingsStore`, `PromptLibrary`, `TimeSource`, `ReminderScheduler`, `WidgetRefresher`), entity types, `PromptLibraryValidator` |
| Domain | App, Widget | `JournalUseCases`, entities (`TodayState`, `StreakSummary`, …), `DeepLink`, `JournalError` |
| Domain (setup) | All | Gradle build, version catalog, module skeletons, CI workflow |
| Data | App | `JournalEnvironmentFactory.create(context, widgets)`, `JournalEnvironment` (use cases, change signals, `ClockEvents`), `JournalEnvironmentProvider`, `ReminderNotifier` |
| Data | Widget | `JournalEnvironmentProvider`, `JournalEnvironment` |
| Widget | App | `GlanceWidgetRefresher`, widget receiver to register in the manifest |

## What Can Be Tested at the End of Each Unit

| After unit | Testable | How |
|---|---|---|
| 1 Domain | Every business rule: prompt selection (matching iOS), refresh limit, editing and backfill rules, streak, search, export, reminder planning | JVM unit and property-based tests (`./gradlew :domain:test`), in CI |
| 2 Data | Storage round-trips, one entry per day, migrations, prompt file, use cases end to end on real storage | Data tests locally and in CI |
| 3 App | The whole app on a device or emulator except the widget; ViewModel behavior | ViewModel tests; manual run |
| 4 Widget | Widget content, updates, tap to Today | Widget tests; manual check on the Home Screen |
| Build and Test | Everything together, CI green, manual checklist (reminders after restart, midnight and 12:00 behavior, nothing leaves the phone) | Full build and test instructions |

## Validation

- Matches `component-dependency.md`: presentation → data → domain, domain depends on nothing in the project.
- Corrected during this stage: `GlanceWidgetRefresher` moved from `:data` to `:widget` (it needs the widget class, and `:data` can't depend on `:widget`); the data factory now receives the refresher from the app.
- Every unit can be built and tested before the next starts.
