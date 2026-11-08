# Components

**Inputs**: `requirements.md`, `stories.md`, `application-design-plan.md` (answers Q1–Q9 all A), iOS design `demo/aidlc-docs/inception/application-design/components.md`.

**Design decisions** (from the plan):

| # | Decision |
|---|---|
| Q1 | Each screen has a ViewModel that calls use cases and exposes screen state; composables only display it |
| Q2 | One use case per user action, same names as iOS, one `execute` function each |
| Q3 | Domain defines ports (interfaces); data implements them |
| Q4 | Manual constructor injection through one `AppContainer` created in `Application.onCreate`; receivers and the widget reach the same environment through `JournalEnvironmentProvider` |
| Q5 | Reactive: the data layer exposes change `Flow`s and a clock-boundary `Flow`; ViewModels re-run use cases when they emit |
| Q6 | The widget runs the domain use cases itself |
| Q7 | Navigation Compose with typed routes, single activity |
| Q8 | Only official Kotlin, AndroidX and Google libraries in shipped code; test-only libraries allowed |
| Q9 | The iOS domain is ported file by file to Kotlin, with the same types, policies and use case names, and the iOS test scenarios reused |

**How Q5 and Q9 fit together**: use cases keep the iOS shape (one-shot `suspend fun execute`), so behavior matches iOS exactly. Reactivity is added around them: ports gain a `changes: Flow<Unit>` signal, and ViewModels call the use cases again whenever a change or clock boundary is emitted. No business rule lives in a `Flow` operator.

**Not ported from iOS** (only needed for iCloud sync, which Android doesn't have, NFR-2.1): `DayEntryMerger`, `ReconcileSyncedEntriesUseCase`, `RemoteChangeObserver`, `EntryRepository.duplicateDays()` and `delete(ids)`. Because there is no sync, the data layer enforces one entry per day with a unique constraint.

**Technology placeholders**: the storage library and the reminder scheduling mechanism are chosen in NFR Requirements. Implementations are named by role here (for example "entry database"); Room, DataStore and AlarmManager are the expected choices.

---

## 1. Domain module (`:domain`, pure Kotlin, no Android dependencies)

### 1.1 Entities and value types

| Component | Purpose | iOS counterpart |
|---|---|---|
| `CalendarDay` | Year, month, day identifying a journal day independently of time zone (NFR-3.2); comparable; stable `key` ("2026-10-08") and `dayNumber` | `CalendarDay` |
| `JournalEntry` | One day's entry: id, day, text, prompt id and prompt text snapshot, created and updated instants | `JournalEntry` |
| `Prompt` / `PromptCategory` | Library prompt (id, category, text) and the four categories | same |
| `DailyPromptState` | Per-day state: chosen prompt id, refreshes used, prompt ids shown today, categories key | same |
| `StreakSummary` / `StreakStatus` | Current and best streak; active, at risk (restorable count, until) or none; today done | same |
| `EntryEditability` | Editable until an instant, backfillable until an instant, or locked | same |
| `TodayState` / `BackfillOffer` | Everything the Today screen and widget need | same |
| `EntryDetail` | An entry plus its current editability (History detail) | same |
| `AppSettings` / `ReminderSettings` | Categories, reminder on/off and time of day, onboarding completed | same |
| `ScheduledReminder` / `ReminderAuthorization` | A reminder to schedule (fire instant, day, prompt text, deep link) and the permission state | same |
| `ExportFormat` / `ExportDocument` | Markdown or plain text; file name and contents | same |
| `DeepLink` | Typed routes used by the widget and reminders (today's editor, backfill editor) with parse and format; unknown input parses to `null` (NFR-6.3) | same |
| `JournalError` | Typed domain errors (entry locked, backfill not available, no refreshes left, empty text, categories empty, storage failure) | same |

### 1.2 Policies (pure functions, no I/O)

| Component | Responsibility | Stories |
|---|---|---|
| `PromptSelector` | Deterministic daily pick and refresh candidates from the selected categories, avoiding prompts answered in the last 30 days; ported exactly, including the stride and hash | US-1.1 to US-1.4 |
| `StableHash` | FNV-1a 64-bit hash, identical to iOS, so prompt picks match across platforms | US-1.2 |
| `DayMath` | Calendar arithmetic: day of an instant in a time zone, start of day, noon, adding days (DST-safe) | NFR-3 |
| `EntryEditPolicy` | Editability of a day at an instant (today until midnight; missing yesterday until 12:00; else locked) | US-2.2 to US-2.4 |
| `StreakCalculator` | Current and best streak and at-risk status from the set of entry days | US-4.1 to US-4.4 |
| `JournalExporter` | Entries to Markdown or plain text, same format as iOS | US-3.3 |
| `EntrySearchMatcher` | Case- and accent-insensitive substring match on entry and prompt text | US-3.2 |
| `ReminderPlanner` | Which reminder instants to schedule for the next days, skipping today when done | US-6.2 |
| `PromptLibraryValidator` | Validates the prompt list (id format, category match, 10–200 characters, no duplicates, minimum counts) | US-1.1, NFR-6.7 |

### 1.3 Ports (interfaces implemented in `:data`)

| Port | Responsibility | Change from iOS |
|---|---|---|
| `TimeSource` | Current instant and time zone; injectable fixed clock in tests | none |
| `EntryRepository` | Read, save and search entries | adds `changes: Flow<Unit>`; drops merge functions |
| `PromptLibrary` | Read-only bundled prompts | none |
| `PromptStateStore` | Per-day prompt state, keeping only today and yesterday | adds `changes` |
| `SettingsStore` | Read and write settings | adds `changes` |
| `ReminderScheduler` | Permission status and replacing the scheduled reminders | permission *request* moves to the UI (Android needs an activity to show the dialog) |
| `WidgetRefresher` | Ask the widget to update | none |

### 1.4 Use cases (one per user action, names as iOS)

| Use case | Triggered by | Stories |
|---|---|---|
| `GetTodayStateUseCase` | Today screen, widget update, clock boundary | US-1.1, US-2.5, US-4.x, US-5.1 |
| `RefreshPromptUseCase` | "New prompt" button | US-1.3 |
| `SaveTodayEntryUseCase` | Autosave, Done, Back while editing | US-2.1, US-2.2 |
| `BackfillYesterdayUseCase` | Save in the backfill editor | US-2.3 |
| `GetEntryUseCase` | Opening an entry from History | US-2.4 |
| `ListEntriesUseCase` | History screen | US-3.1 |
| `SearchEntriesUseCase` | History search | US-3.2 |
| `ExportJournalUseCase` | Export actions | US-3.3 |
| `GetStreakUseCase` | Streak (also inside GetTodayState) | US-4.x |
| `GetSettingsUseCase` | Settings, onboarding, widget | US-7.2 |
| `UpdatePromptCategoriesUseCase` | Category toggles | US-1.4 |
| `ConfigureReminderUseCase` | Reminder toggle or time | US-6.1 |
| `SyncReminderWithTodayUseCase` | After save, app start, boot, time change, app update | US-6.2, US-6.4 |
| `CompleteOnboardingUseCase` | Finishing onboarding | US-7.1 |
| `JournalUseCases` | Holds all use cases, built from the ports | n/a |

---

## 2. Data module (`:data`, Android library)

| Component | Implements | Responsibility |
|---|---|---|
| `EntryRecord` + entry DAO | (storage model) | One row per entry, unique on day key; stores the day separately from timestamps |
| `JournalDatabase` | n/a | Opens the entry database in the app's private storage; versioned schema with non-destructive migrations only (NFR-7.2) |
| `DatabaseEntryRepository` | `EntryRepository` | Maps rows to `JournalEntry`, runs queries off the main thread, emits `changes` when the table changes |
| `BundledPromptLibrary` | `PromptLibrary` | Loads `prompts.json` (same file as iOS) from the module's assets once and validates it |
| `PreferencesSettingsStore` | `SettingsStore` | Settings in private key-value storage; emits `changes` |
| `PreferencesPromptStateStore` | `PromptStateStore` | Per-day prompt state in private key-value storage; emits `changes` |
| `SystemTimeSource` | `TimeSource` | System clock and current time zone |
| `ClockEvents` | n/a | `Flow` that emits at the next midnight and 12:00, and when the system time, date or time zone changes |
| `AndroidReminderScheduler` | `ReminderScheduler` | Schedules reminder alarms (mechanism chosen in NFR Requirements), reports notification permission state |
| `ReminderNotifier` | n/a | Builds and posts the reminder notification with today's prompt and an explicit, immutable tap intent |
| `JournalEnvironmentFactory` | n/a | Builds the ports and `JournalUseCases` for production or in-memory tests; takes a `WidgetRefresher` from the caller (the app passes the widget's implementation) |
| `JournalEnvironmentProvider` | n/a | Interface the `Application` implements, so the widget and receivers get the shared environment without depending on `:app` |

---

## 3. App module (`:app`, Android application)

| Component | Responsibility | Stories |
|---|---|---|
| `JournalApplication` | Creates the `AppContainer` once; installs the top-level error handler (NFR-6.8) | All |
| `AppContainer` | Holds `JournalUseCases`, `ClockEvents` and the data-layer change flows for the activity and ViewModels; the Application exposes the same environment to the widget through `JournalEnvironmentProvider` | All |
| `MainActivity` | Single activity; hosts the navigation graph; receives deep links from the widget and reminders and routes them to Today (clearing the back stack) | US-5.2, US-6.3 |
| `JournalNavGraph` + routes | Typed routes: Onboarding, Today, History, Entry(day), Settings; Back behavior per stories | US-3.1, US-7.x |
| `DeepLinkRouter` | Parses incoming links with `DeepLink`; unknown links go to Today and do nothing else | NFR-6.3 |
| `OnboardingViewModel` + screen | Two steps: categories, optional reminder (requests notification permission when turned on) | US-7.1, US-6.1 |
| `TodayViewModel` + screen | Streak, backfill offer, prompt and refresh, editor or saved entry; reloads on changes and clock boundaries | US-1.x, US-2.1, US-2.3, US-2.5, US-4.x |
| `EntryEditorViewModel` + editor | Text state that survives screen rebuilds; autosave; Done; Back saves; detects the midnight lock and shows the locked message without swapping in the new day (FR-8.1) | US-2.1, US-2.2, US-2.5 |
| `BackfillViewModel` + sheet | Yesterday's prompt and editor; Back asks before discarding | US-2.3 |
| `HistoryViewModel` + screen | List, debounced search, Back clears search first | US-3.1, US-3.2 |
| `EntryDetailViewModel` + screen | Read-only or editable entry | US-2.4 |
| `SettingsViewModel` + screen | Categories, reminder with permission state checked every time the screen is shown (FR-6.3), export with the "only copy" note | US-7.2, US-1.4, US-6.1, US-3.3, US-2.6 |
| `ExportSharer` | Writes the export to the app's private cache and shares it through the share sheet with temporary read access only | US-3.3 |
| `ReminderReceiver` | Fires at reminder time: checks today isn't done, posts the notification, schedules the next one | US-6.2 |
| `RescheduleReceiver` | On boot, time or time-zone change and app update: runs `SyncReminderWithTodayUseCase` | US-6.4 |
| Design system (`JournalTheme`, `PrimaryButton`, `Card`) | iOS look: orange accent, cards on grouped background, serif prompt, light and dark | NFR-1.5 |
| Manifest configuration | Backup and device-transfer opt-out, no internet permission, notification permission declaration, receivers not exported except where the system requires | NFR-2.2, NFR-2.4 |

---

## 4. Widget module (`:widget`, Android library)

| Component | Responsibility | Stories |
|---|---|---|
| `PromptStreakWidget` (Glance) | Small widget: prompt (truncated, serif), streak badge with done and at-risk states, setup invitation before onboarding; tap opens Today's editor through an explicit intent | US-5.1, US-5.2 |
| `PromptStreakWidgetReceiver` | Widget provider registered with the system | US-5.1 |
| `WidgetStateLoader` | Gets `TodayState` from `GetTodayStateUseCase` through `JournalEnvironmentProvider` | US-5.1, US-1.2 |
| `WidgetUpdateScheduler` | Schedules widget updates at the next midnight and 12:00 so the widget changes without the app running | US-5.3 |
| `GlanceWidgetRefresher` | Implements the domain `WidgetRefresher` port by updating `PromptStreakWidget`; lives here because it needs the widget class, and `:data` can't depend on `:widget` | US-5.3 |

---

## 5. Story Coverage

| Story | Main components |
|---|---|
| US-1.1 | `GetTodayStateUseCase`, `PromptSelector`, `BundledPromptLibrary`, `PromptLibraryValidator`, `TodayViewModel` |
| US-1.2 | `PromptSelector`, `StableHash`, `PreferencesPromptStateStore`, `WidgetStateLoader` |
| US-1.3 | `RefreshPromptUseCase`, `PromptSelector`, `TodayViewModel` |
| US-1.4 | `UpdatePromptCategoriesUseCase`, `SettingsViewModel`, `OnboardingViewModel` |
| US-2.1 | `SaveTodayEntryUseCase`, `EntryEditorViewModel`, `DatabaseEntryRepository` |
| US-2.2 | `EntryEditPolicy`, `EntryEditorViewModel`, `ClockEvents` |
| US-2.3 | `BackfillYesterdayUseCase`, `EntryEditPolicy`, `BackfillViewModel` |
| US-2.4 | `GetEntryUseCase`, `EntryDetailViewModel` |
| US-2.5 | `MainActivity`, `TodayViewModel`, `EntryEditorViewModel` |
| US-2.6 | Manifest configuration, `JournalDatabase` (private storage), `SettingsViewModel` note |
| US-3.1 | `ListEntriesUseCase`, `HistoryViewModel`, `JournalNavGraph` |
| US-3.2 | `SearchEntriesUseCase`, `EntrySearchMatcher`, `HistoryViewModel` |
| US-3.3 | `ExportJournalUseCase`, `JournalExporter`, `ExportSharer` |
| US-4.1 to US-4.4 | `StreakCalculator`, `GetStreakUseCase`, `GetTodayStateUseCase`, Today streak card |
| US-5.1 | `PromptStreakWidget`, `WidgetStateLoader` |
| US-5.2 | `PromptStreakWidget` tap intent, `MainActivity`, `DeepLinkRouter` |
| US-5.3 | `WidgetUpdateScheduler`, `GlanceWidgetRefresher` |
| US-6.1 | `ConfigureReminderUseCase`, `AndroidReminderScheduler`, `SettingsViewModel`, `OnboardingViewModel` |
| US-6.2 | `ReminderPlanner`, `SyncReminderWithTodayUseCase`, `ReminderReceiver` |
| US-6.3 | `ReminderNotifier` tap intent, `MainActivity`, `DeepLinkRouter` |
| US-6.4 | `RescheduleReceiver`, `SyncReminderWithTodayUseCase` |
| US-7.1 | `CompleteOnboardingUseCase`, `OnboardingViewModel` |
| US-7.2 | `SettingsViewModel`, settings stores |
