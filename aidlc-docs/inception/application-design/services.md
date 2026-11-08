# Services and Orchestration

The "service layer" is the set of domain use cases (`JournalUseCases`). ViewModels, receivers and the widget call them; use cases call ports. This document describes how they work together for each main flow. Detailed rules are in Functional Design.

**Common pattern (plan Q5:A)**: a ViewModel loads its state by calling a use case, then listens to `AppContainer.refreshSignals` (entry, settings and prompt-state changes plus `ClockEvents.boundaries`) and calls the use case again on each signal. Writes go through use cases only.

**Common side effects** (same as iOS): after any save, refresh, category change or onboarding, the use case calls `WidgetRefresher.reloadAll()` and, where reminders depend on it, `SyncReminderWithTodayUseCase`.

---

## S1. Open the app on Today (US-1.1, US-2.5, US-4.1)

1. `MainActivity` starts; `JournalNavGraph` checks `GetSettingsUseCase` → Onboarding or Today.
2. `TodayViewModel` calls `GetTodayStateUseCase`:
   - reads today's entry (or a future-dated one), resolves today's prompt through `PromptStateStore` or `PromptSelector` (and stores the pick), works out editability, the backfill offer and the streak.
3. If today isn't done, the editor gets focus and the keyboard opens (target: ready in about 1 s, NFR-5.1).
4. The ViewModel subscribes to `refreshSignals`; any change or clock boundary re-runs step 2.

## S2. Write and save today's entry (US-2.1, US-2.2)

1. `EntryEditorViewModel.onTextChange` updates text state (kept across screen rebuilds).
2. After a short pause, autosave calls `SaveTodayEntryUseCase(text, day)`; saves are serialized so autosave and Done never race.
3. The use case checks editability (`EntryEditPolicy`), validates text, writes through `EntryRepository`, refreshes the widget and syncs reminders.
4. `EntryRepository.changes` emits → Today's streak updates; the editor stays on screen (iOS keyboard fix).
5. Done saves once more and closes the editor; Back saves before leaving.
6. On failure (`StorageFailure`) the editor keeps the text and shows a generic message (NFR-6.8).

## S3. Midnight while editing (US-2.2, FR-8.1)

1. `ClockEvents` emits at 00:00.
2. `EntryEditorViewModel` re-checks editability for *its own* day. It becomes `Locked`: any pending text is saved first if the save is still allowed (saves that arrive after midnight for the old day are rejected by the use case, so the ViewModel flushes pending text when the boundary is close), then the editor turns read-only and shows the locked message.
3. `TodayViewModel` loads the new day, but keeps showing the locked entry until the user leaves it.

## S4. Refresh the prompt (US-1.3)

1. "New prompt" → `RefreshPromptUseCase`: rejects if today has an entry or the limit is reached; otherwise `PromptSelector.refreshedPrompt`, stores the new state, refreshes the widget.
2. `PromptStateStore.changes` emits → Today shows the new prompt and remaining count.

## S5. Backfill yesterday (US-2.3, US-4.2)

1. Today shows the offer from `TodayState.backfillOffer`; tapping opens the backfill sheet (`BackfillViewModel`) with yesterday's prompt.
2. Save → `BackfillYesterdayUseCase`: checks `EntryEditPolicy` (still before 12:00, yesterday missing), saves, refreshes the widget, syncs reminders.
3. Back with text → confirm discard. At 12:00, `ClockEvents` emits and the offer disappears; an open sheet shows that the window closed.

## S6. History, search and entry detail (US-3.1, US-3.2, US-2.4)

1. `HistoryViewModel` calls `ListEntriesUseCase`; typing debounces (about 250 ms) and calls `SearchEntriesUseCase`.
2. Tapping an entry navigates to `Route.Entry(dayKey)`; `EntryDetailViewModel` calls `GetEntryUseCase` and shows it read-only or with the editor if it's today's.
3. Back from the entry returns to History once; Back in History clears search, then returns to Today.

## S7. Settings: categories, reminder, export (US-1.4, US-6.1, US-3.3, US-7.2)

1. Each time Settings is shown, `SettingsViewModel` loads settings and asks `ReminderScheduler.authorization()` (FR-6.3: denied state shown on open, not only after toggling).
2. Category toggle → `UpdatePromptCategoriesUseCase` (rejects an empty set with a message; refreshes the widget).
3. Reminder on → if Android 13+ and permission not determined, the screen requests notification permission; the result goes to `ConfigureReminderUseCase`, which saves settings and schedules reminders only when allowed. Denied → unavailable state with a button opening the system notification settings.
4. Export → `ExportJournalUseCase` → `ExportSharer` writes the file to the app's private cache and opens the share sheet with temporary read permission (US-3.3 security scenario).

## S8. Reminders: schedule, fire, reschedule (US-6.2, US-6.3, US-6.4)

1. `SyncReminderWithTodayUseCase` asks `ReminderPlanner` for the next days' reminders (skipping today when done) and calls `ReminderScheduler.replacePending`.
2. Triggers: app start, after saves and backfills, reminder settings changes, and `RescheduleReceiver` on boot, time or time-zone change and app update.
3. At fire time `ReminderReceiver` re-checks that today isn't done (in case of a late save), posts the notification through `ReminderNotifier` with today's prompt and an explicit, immutable tap intent, then syncs again to keep the schedule filled.
4. Tapping the notification → S10.

## S9. Widget update (US-5.1, US-5.3)

1. `PromptStreakWidget.provideGlance` calls `WidgetStateLoader.load(context)`, which gets the environment through `JournalEnvironmentProvider` and calls `GetTodayStateUseCase` (or returns `NeedsSetup` before onboarding).
2. Updates are triggered by `WidgetRefresher.reloadAll()` after writes, and by `WidgetUpdateScheduler` at the next midnight and 12:00, so the widget changes without the app running.
3. Prompt picks made by the widget are stored the same way as in the app, so the app and widget always agree (US-1.2).

## S10. Deep links from the widget and reminders (US-5.2, US-6.3, NFR-6.3)

1. The tap intent is explicit (targets `MainActivity`) and immutable.
2. `MainActivity.onCreate` / `onNewIntent` → `DeepLinkRouter.route`: `DeepLink.parse`; known links clear the back stack to Today (and open the backfill sheet for `BackfillEditor` if still offered); unknown links just show Today.
3. Today focuses the editor if today isn't done (S1 step 3).

## S11. Onboarding (US-7.1)

1. `OnboardingViewModel` step 1: categories (all selected by default, at least one required); step 2: optional reminder (permission requested only if turned on).
2. Finish → `CompleteOnboardingUseCase` saves settings, configures the reminder if chosen (completing even if permission is denied), refreshes the widget; navigation replaces Onboarding with Today.

## S12. Errors (NFR-6.8, NFR-6.4)

- Storage errors become `JournalError.StorageFailure` in the data layer; ViewModels show generic messages and keep user input.
- `JournalApplication` installs a top-level handler that logs the exception class (never journal text) before the default crash handling; release builds strip debug and verbose logs (NFR-6.2).
