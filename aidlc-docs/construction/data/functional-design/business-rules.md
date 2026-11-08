# Business Rules: Unit 2 Data

Rule ids `DATA-…`. Domain rules (`BR-…`) are referenced where the data unit carries them out.

## Entries (DATA-E)

| ID | Rule | Source |
|---|---|---|
| DATA-E1 | `save` inserts a new row or updates the row with the same `id`. If a **different** entry already exists for the same day, the save fails with a storage error; it never overwrites another entry's text | BR-E1, NFR-6.8 |
| DATA-E2 | `entry(day)` returns the row whose `day_key` equals the day's key, or nothing | BR-E1 |
| DATA-E3 | `entries(from, through)` returns rows with `from.dayNumber ≤ day_number ≤ through.dayNumber`; `entriesAfter(day)` rows with `day_number > day.dayNumber`; `allEntries` all readable rows; `entryDays` their days. Order is unspecified (the domain sorts) | Domain ports |
| DATA-E4 | `search(query)` loads all readable entries and keeps those matching `EntrySearchMatcher` (Q2:A), so storage results always equal the domain rules | BR-Q, Q2:A |
| DATA-E5 | A row whose `day_key` doesn't parse is skipped by every read, logged with its `id` and the error type only, and left untouched in storage (Q5:A) | NFR-6.2, Q5:A |
| DATA-E6 | Every successful write emits one `changes` signal after the write is committed | Application design Q5 |
| DATA-E7 | All database work runs off the main thread inside the repository (main-safe ports) | NFR design §5 |
| DATA-E8 | Database errors propagate as exceptions; the domain turns them into `StorageFailure`. No partial rows are written (each save is one statement) | NFR-6.8 |
| DATA-E9 | Schema changes in later versions use explicit, tested migrations; destructive fallback is never enabled | NFR-7.2 |

## Settings and Prompt State (DATA-S)

| ID | Rule | Source |
|---|---|---|
| DATA-S1 | Settings are stored as the iOS-shaped JSON under `settings.v1`; prompt state as the iOS-shaped JSON map under `promptState.v1` | Q1:A |
| DATA-S2 | Unreadable or missing settings → defaults (all categories, reminder off at 20:00, onboarding not completed); the error type is logged; the next save overwrites with valid JSON (Q4:A) | BR-G1, Q4:A |
| DATA-S3 | Unreadable or missing prompt state → no state; the domain then picks the day's prompt again (deterministic, so normally the same prompt) | BR-P4, Q4:A |
| DATA-S4 | `save(state, keepingFrom)` stores the state and removes states for days before `keepingFrom`, in one update | BR-P4 |
| DATA-S5 | Read-modify-write updates of the prompt state map are atomic (no lost update when two callers save at once) | NFR design §3 |
| DATA-S6 | Every successful save emits one `changes` signal | Application design Q5 |
| DATA-S7 | Unknown JSON fields are ignored and missing ones take defaults, so older and newer app versions can read each other's settings | NFR-7.2 |

## Prompt Library (DATA-P)

| ID | Rule | Source |
|---|---|---|
| DATA-P1 | `prompts.json` is loaded once from the module's assets; `version` must be 1 | Domain BL model §9.1 |
| DATA-P2 | The list is validated with `PromptLibraryValidator`; failure means the library is unusable and environment creation fails (the App shows a startup error, as iOS does) | NFR-6.7 |
| DATA-P3 | A Data test checks the shipped file: exactly 600 prompts, 150 per category, ids `<category>-001` … `-150`, and the same ids and texts as the iOS file at the time of copying | FR-1.2, unit of work Q5:A |
| DATA-P4 | Lookup by id is constant time | Performance |

## Reminders (DATA-R)

| ID | Rule | Source |
|---|---|---|
| DATA-R1 | `authorization()`: `ALLOWED` if notifications are enabled for the app (and, on Android 13+, the permission is granted); on Android 13+ without the permission, `NOT_DETERMINED` if the dialog was never shown (`notificationPermissionAsked.v1` is false), otherwise `DENIED`; on Android 10–12, `DENIED` if the user turned the app's notifications off | BR-R7, FR-6.3 |
| DATA-R2 | `replacePending(reminders)` cancels every reminder this app registered and registers the new list; registering an empty list cancels all. How many of the planned reminders are registered with the system, and how exactly, is decided in NFR Requirements | BR-R1, BR-R2 |
| DATA-R3 | Nothing about pending reminders needs storing: after a restart, time change or app update, the App unit re-runs `SyncReminderWithTodayUseCase`, which recomputes them from settings | FR-6.5 |
| DATA-R4 | `ReminderNotifier.post` shows the reminder's title and body on a "Daily reminder" notification channel, with one notification id per day (a newer one for the same day replaces the older), and a tap intent that is explicit (targets the app's main activity) and immutable, carrying the reminder's deep link | BR-R4, BR-R5, NFR-6.3 |

## Clock (DATA-C)

| ID | Rule | Source |
|---|---|---|
| DATA-C1 | `SystemTimeSource.now` is the system clock; `timeZone` is the **current** system time zone, read on every access (so a time-zone change is seen immediately) | BR-T1, BR-T2 |
| DATA-C2 | `ClockEvents.boundaries` emits at the next local midnight and next 12:00, when the system time, date or time zone changes, and when the app returns to the foreground (Q6:A). After each emission the next boundary is recomputed from the current time and zone | US-2.2, US-4.3, Q6:A |
| DATA-C3 | `ClockEvents` listens to system broadcasts only while something collects the flow (no leaks) | NFR-6 |

## Wiring (DATA-W)

| ID | Rule | Source |
|---|---|---|
| DATA-W1 | `JournalEnvironmentFactory.create(context, widgets, onBackgroundFailure)` builds the stores, the prompt library and one `JournalUseCases`; the App creates it once per process | NFR design §3 |
| DATA-W2 | `JournalEnvironmentFactory.createInMemory(...)` builds the same wiring over an in-memory database and in-memory preferences for tests and previews | Application design |
| DATA-W3 | `JournalEnvironmentProvider` is the interface the Application implements so the widget and receivers reach the same environment | Application design |
| DATA-W4 | Logs from this unit never include entry text, prompt text or settings values; only ids, counts and error types | NFR-6.2 |

## Story and Requirement Mapping (example-based tests)

| Story / requirement | Rules | Test |
|---|---|---|
| US-1.1 same library as iOS | DATA-P1–P3 | Prompt file test |
| US-1.2, US-1.3 prompt and refresh persist across relaunch | DATA-S1, S4, S5 | Prompt state store tests (save, reload, prune, concurrent saves) |
| US-1.4, US-7.2 settings persist | DATA-S1, S2, S7 | Settings store tests (round trip, defaults, corrupt input, unknown fields, iOS JSON sample) |
| US-2.1 one entry per day, save failure | DATA-E1, E6, E8 | Repository tests (insert, update, different id same day fails, change signal) |
| US-3.1 history and time-zone change | DATA-E3, E5 | Repository range and skip-bad-row tests |
| US-3.2 search, large journal | DATA-E4 | Repository search equals matcher; 3,650-entry speed check |
| US-6.1, US-6.3 permission state, notification tap | DATA-R1, R4 | Authorization mapping and notification content tests |
| US-2.2, US-4.3, US-5.3 midnight and 12:00 | DATA-C2 | ClockEvents boundary computation tests |
| US-2.6 privacy | DATA-E, S (private storage), W4 | No-journal-text-in-logs check |
| All use cases end to end | DATA-W1, W2 | Domain use cases on real storage (in-memory database) |
