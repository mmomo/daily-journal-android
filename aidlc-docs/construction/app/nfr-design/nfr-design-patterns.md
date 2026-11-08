# NFR Design Patterns: Unit 3 App

**Inputs**: `app-nfr-design-plan.md` (Q1 A, Q2 A), App NFR requirements and tech stack, application design services S1–S12, Data unit environment.

## 1. Startup and Environment (Q1:A, APP-PERF-1, APP-REL-4)

```
JournalApplication.onCreate():                       // main thread, returns immediately
    install top-level error handler (logs exception class only, then the system handler)
    StrictMode (debug builds only)
    startup = AppStartup(scope = application scope, dispatcher = IO)
    startup.begin()                                  // creates the environment on a background thread

AppStartup:
    state: StateFlow<StartState> = Loading | Ready(container) | Failed(reason type)
    begin(): launch(IO) { container = AppContainer(JournalEnvironmentFactory.create(...)) } → Ready, or Failed on any exception
    retry(): Failed → Loading → begin()
    awaitEnvironment(): suspend until Ready (throws if Failed)
```

- **Data unit change (recorded in App Code Generation)**: `JournalEnvironmentProvider` changes from `val environment` to `suspend fun awaitEnvironment(): JournalEnvironment`, so the widget and receivers wait for background creation instead of blocking. The Application implements it through `AppStartup`.
- After creation, the app runs `SyncReminderWithTodayUseCase` once (reminders recomputed on app start, APP-REL-5).

## 2. Startup State in the UI (Q2:A)

`MainActivity` collects `AppStartup.state`:

| State | Screen |
|---|---|
| Loading | Plain background with the app's colors (the "first frame"); no spinner unless it lasts over 300 ms |
| Failed | "Can't open your journal" with a "Try again" button calling `retry()` |
| Ready | Navigation host; start destination is Onboarding or Today from `GetSettingsUseCase` |

## 3. Refresh Signals in ViewModels

Each screen ViewModel:
```
init: load()
viewModelScope.launch { environment.refreshSignals.collect { load() } }   // data changes and clock boundaries
```
Loads are conflated (a new signal while loading triggers one more load afterwards, not many).

## 4. Editor: Autosave, Done, Back, Midnight (APP-REL-1 to 3)

| Event | Pattern |
|---|---|
| Typing | Text and `editedAt` (now) kept in `SavedStateHandle`; autosave after 0.8 s without typing |
| Autosave / Done / Back | `SaveTodayEntryUseCase(text, day, editedAt)`; Done and Back save immediately (cancelling the pending autosave) |
| Clock boundary while editing | If a save is pending, it runs at once with its `editedAt` (the domain accepts it within 5 s after midnight); then editability for the editor's **own** day is re-checked; if locked, the editor turns read-only with "This entry is now locked" and Today keeps showing it until the user leaves |
| Save failure | Text stays; status shows "Couldn't save. Try again." |
| Text over the limit | Typing stops at 20,000 characters with "You've reached the maximum length for one entry"; a paste that would exceed it is rejected whole (C3:B, C4:B) |

## 5. Back Handling (Q3:B, classic)

| Screen | `BackHandler` behavior |
|---|---|
| Editor with unsaved text | Save, then navigate back |
| Backfill with text | Ask "Discard yesterday's entry?" (Discard / Keep writing) |
| History with a search query | Clear the query first |
| Onboarding step 2 | Go to step 1 |
| Everything else | Default (Entry → History → Today → leave app) |

## 6. Notification Permission (FR-6.3)

- On Android 13+, turning the reminder on when the state is `NOT_DETERMINED` launches the system permission request; when it closes, `NotificationPermissionFlag.markAsked()` runs, then `ConfigureReminderUseCase`.
- Settings re-reads `ReminderScheduler.authorization()` every time it becomes visible (lifecycle `ON_RESUME`), so "denied" shows on opening and returning from system settings updates it.
- "Open settings" uses the system's app notification settings screen.

## 7. Deep Links (APP-SEC-5, US-5.2, US-6.3)

```
MainActivity.onCreate / onNewIntent(intent):
    link = DeepLink.parse(intent.dataString)            // null for anything unknown
    pendingLink = link (kept until startup is Ready and onboarding is done; BR-G5)
on Ready and onboarded:
    navigate to Today, clearing the back stack (popUpTo start, launchSingleTop)
    TodayEditor   → focus the editor if today isn't done
    BackfillEditor→ open the backfill sheet if still offered
    null          → just show Today
```

`MainActivity` uses `launchMode="singleTop"`-equivalent behavior through `onNewIntent`, so a tap while the app is open routes in place.

## 8. Receivers

| Receiver | Pattern |
|---|---|
| `ReminderReceiver` (not exported) | `goAsync()`; on a background scope with a 9-second limit: await environment → if today isn't done, post the notification for today's prompt → `SyncReminderWithTodayUseCase` (registers the next one) → `finish()` |
| `RescheduleReceiver` (not exported; protected system broadcasts: boot completed, time set, time-zone changed, package replaced) | `goAsync()`; same limit: await environment → `SyncReminderWithTodayUseCase` → `finish()` |

Failures are caught and logged by exception class only.

## 9. Release Hardening and Logging (APP-SEC-1, 2, 6, 7)

- `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`, `signingConfig = debug` (C1:B).
- R8 rule: `-assumenosideeffects class android.util.Log { v(...); d(...); i(...); }` removes verbose, debug and info logs; `w` and `e` stay (they never contain journal text).
- Keep rules only where libraries need them (typed routes use kotlinx-serialization, whose rules ship with the library).
- Manifest: `allowBackup="false"`, `dataExtractionRules` and `fullBackupContent` excluding everything, no internet permission.

## 10. Error Wording (domain errors → screens)

| Error | Message |
|---|---|
| `EmptyText` | Done stays disabled (no message) |
| `TextTooLong` | "You've reached the maximum length for one entry." |
| `EntryLocked` | "This entry is locked." |
| `BackfillNotAvailable` | "Yesterday can no longer be filled in." |
| `NoRefreshesLeft` | Button shows "No refreshes left today" |
| `CategoriesEmpty` | "Choose at least one category." |
| `NotificationsDenied` | Reminder shown as unavailable with "Open settings" |
| `NothingToExport` | Export actions disabled with "No entries to export yet." |
| `StorageFailure` | "Couldn't save. Try again." (or "Couldn't load your journal.") |

## 11. Test Patterns (Q5:A)

- ViewModels take the use cases, a `TimeSource` and dispatchers through their constructors; tests build them over the domain's `TestEnvironment` (fake ports) with `kotlinx-coroutines-test` and a test dispatcher.
- Pure ViewModel tests use Kotest; any test needing Android classes uses JUnit 4 + Robolectric (as in the Data unit).
- Stateful property (PBT-06): random sequences of edit, pause, Done, Back and midnight events against the editor ViewModel, checked against a model: the stored entry always equals the last text edited before midnight, and nothing is saved after the lock.

## 12. Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01, 03, 05, 06, 09, 11, 15 applied as above; 10 by policy. Compliant; server-only rules N/A |
| Property-Based Testing | PBT-06 (editor state machine), PBT-07 and 08 via the shared generators and Kotest; other rules N/A (no business rules in this unit) |
