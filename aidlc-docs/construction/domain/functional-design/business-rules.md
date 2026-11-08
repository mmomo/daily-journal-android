# Business Rules: Unit 1 Domain

**Base**: iOS `business-rules.md`, ported with the same ids and wording (plan Q1:A). Android differences are marked **Android:**. The iOS merge rules (BR-M1–M6) are dropped: they exist only for iCloud sync.

Notation: **today** = `CalendarDay.containing(now, timeZone)`; **yesterday** = today − 1; **midnight(d)** = `(d+1).startIn(tz)`; **noon(d)** = `d.noonIn(tz)`. Examples use America/Mexico_City (UTC−6, no DST) unless stated.

---

## Time (BR-T)

| ID | Rule | Example |
|---|---|---|
| BR-T1 | Every rule gets "now" and the time zone from `TimeSource`. No domain code reads the system clock directly | Tests inject a fixed instant |
| BR-T2 | Days are Gregorian civil dates in the device's **current** time zone | Any device calendar setting still journals on 2026-10-08 |
| BR-T3 | An entry's day is fixed when it's created and never recomputed from timestamps | Entry for 10-08 stays 10-08 after moving to Tokyo |
| BR-T4 | Day boundaries are computed by the calendar, never as "+86,400 s" | 2026-11-01 in New York is 25 h long; still one day |
| BR-T5 **(Android)** | `dayNumber` counts days from 2001-01-01, exactly as on iOS, because prompt selection depends on it | 2001-01-01 → 0; 2026-10-08 → 9411 |

## Prompts (BR-P)

| ID | Rule | Stories |
|---|---|---|
| BR-P1 | The eligible pool is every library prompt whose category is selected, sorted by `id` | US-1.4 |
| BR-P2 | Each day's prompt is a deterministic function of (day, selected categories, the prompt ids answered in the previous 30 days). **Android:** the same function as iOS, bit for bit, so the same inputs give the same prompt on both platforms | US-1.2 |
| BR-P3 | No prompt is the daily pick twice within any 30 consecutive days while the categories stay the same. A daily pick that matches a prompt answered in the previous 30 days is skipped | US-1.2 |
| BR-P4 | The first time a day's prompt is needed on the device, it's computed and saved in `DailyPromptState`. After that it's read from the state, so changing categories mid-day doesn't change it | US-1.4 |
| BR-P5 | Up to **3** refreshes per day (`refreshesUsed` 0…3). The count resets with the new day's state | US-1.3 |
| BR-P6 | A refresh picks a prompt from the **current** selected categories that isn't in today's `shownPromptIds` and wasn't answered in the previous 30 days. The result is deterministic for (day, categories, refresh number) | US-1.3, US-1.4 |
| BR-P7 | Refreshing is refused (`EntryLocked`) once today is done (an entry exists for today, or a future-dated entry exists) | US-1.3 |
| BR-P8 | If the stored `promptId` no longer exists in the library, the daily pick is recomputed and replaces it | n/a |
| BR-P9 | If every prompt in the pool is excluded, exclusions are relaxed in this order: answered-in-30-days, then everything shown today except the current prompt. A refresh with a pool of size 1 throws `NoRefreshesLeft`. **Android:** this is the order the iOS code uses (its design document lists only the first two steps) | n/a |

## Entries (BR-E)

| ID | Rule | Stories |
|---|---|---|
| BR-E1 | At most one entry per day | US-2.1 |
| BR-E2 | Text is valid if it has at least one non-whitespace character. It's stored exactly as typed. **Android:** at most **20,000 characters** (Unicode code points); longer text is rejected with `TextTooLong` and nothing is saved (Q3:B). How the editor reacts (message at the limit, a paste past the limit rejected whole) is defined in the App unit (clarifications C3:B, C4:B) | US-2.1 |
| BR-E3 | Saving today: create if none exists (prompt = today's current prompt, frozen), or update text and `updatedAt` if one exists. Only while `now < midnight(today)`, except as allowed by BR-E4 | US-2.1, US-2.2 |
| BR-E4 | **Android (C1:A, replaces the iOS rule):** each save carries `editedAt`, the instant the text was last changed. A save for day *d* that arrives after midnight(*d*) is **accepted** only if `editedAt < midnight(d)` and `now < midnight(d) + 5 seconds`; it creates or updates *d*'s entry exactly as BR-E3. Any other save for a past day is rejected (`EntryLocked`). This keeps text typed before midnight (FR-8.1) even though autosave waits about a second after typing stops | US-2.2 |
| BR-E5 | Editability: day == today → `Editable(until = midnight(today))`. Day == yesterday, no entry, and `now < noon(today)` → `Backfillable(until = noon(today))`. Otherwise `Locked` | US-2.2–2.4 |
| BR-E6 | Future-dated days (day > today, after westward travel) are `Locked` (Q4:A) | n/a |
| BR-E7 | While a future-dated entry exists, **today counts as done**: Today shows that entry read-only, and no new entry can be created for today (`EntryLocked`) (Q4:A) | n/a |
| BR-E8 | Backfill creates an entry for yesterday with yesterday's prompt: the device's `DailyPromptState` for yesterday if present, else the deterministic daily pick. No refresh | US-2.3 |
| BR-E9 | Only yesterday can be backfilled | US-2.3 |
| BR-E10 | `createdAt` is the instant of the first save; `updatedAt` the instant of the latest save. **Android:** for a save accepted under BR-E4's grace, both use `now` (the actual save time), so `updatedAt` may be a few seconds after midnight | n/a |

## Streak (BR-S)

Let **D** = the set of entry days ≤ today, plus today if any entry day > today. `run(x)` = number of consecutive days in D ending at x (0 if x ∉ D).

| ID | Rule | Example (D, now) → result |
|---|---|---|
| BR-S1 | Today ∈ D and (yesterday ∈ D, or now ≥ noon(today), or run(today−2) = 0) → current = run(today), **active** | {10-06,07,08}, 10-08 21:00 → 3 active |
| BR-S2 | Today ∈ D, yesterday ∉ D, now < noon(today), run(today−2) > 0 → current = 1, **atRisk(run(today−2), noon(today))** | {10-05,06,07,09}, 10-09 09:00 → 1 atRisk(3) |
| BR-S3 | Today ∉ D, yesterday ∈ D → current = run(yesterday), **active** | {10-06,07}, 10-08 09:00 → 2 active |
| BR-S4 | Today ∉ D, yesterday ∉ D, now < noon(today), run(today−2) > 0 → current = run(today−2), **atRisk(same, noon(today))** | {10-05,06,07}, 10-09 09:00 → 3 atRisk(3) |
| BR-S5 | Otherwise current = 0, **none** | {10-05,06,07}, 10-09 12:00 → 0 none |
| BR-S6 | best = max(longest run in D, current) | Runs 12 and 4 → best 12 |
| BR-S7 | isTodayDone = today ∈ D | n/a |
| BR-S8 | The streak is always recalculated from current data, never stored | US-4.1 |

## Search (BR-Q)

| ID | Rule |
|---|---|
| BR-Q1 | The query is trimmed. Empty → all entries, newest first |
| BR-Q2 | An entry matches if its `text` or `promptText` contains the whole query as one substring, ignoring case and accents. **Android:** both sides are compared after Unicode NFD decomposition with combining marks removed, then case-folded, which gives the same results as iOS for the test fixtures |
| BR-Q3 | Results are newest day first |

## Export (BR-X)

| ID | Rule |
|---|---|
| BR-X1 | Throws `NothingToExport` when there are no entries |
| BR-X2 | Entries are oldest day first; future-dated entries included in date order |
| BR-X3 | File name: `daily-journal-YYYY-MM-DD.md` or `.txt` (export day) |
| BR-X4 | Layouts in `business-logic-model.md` §6, identical to iOS. Entry text is written exactly as stored |

## Reminders (BR-R)

| ID | Rule |
|---|---|
| BR-R1 | If reminders are disabled or notifications aren't allowed, all pending reminders are removed |
| BR-R2 | Otherwise the pending set is replaced with a **7-day window** (Q5:A). Day d is in the window if d ∈ [today, today+6], fire(d) > now, and d isn't done (only today can be done). **Android:** the Data unit decides how many of these to register with the system |
| BR-R3 | fire(d) = d at the configured hour:minute in the current time zone; if a DST gap removes that time, the next valid instant |
| BR-R4 | Title "Time to journal". Body "Your daily prompt is waiting." For **today's** reminder only, when the streak is active with current ≥ 1, add " Keep your N-day streak going." |
| BR-R5 | Every reminder carries `DeepLink.TodayEditor` |
| BR-R6 | The window is recalculated on: app start and return to foreground, day change, save, backfill, reminder setting changes, and **Android:** phone restart, time or time-zone change, app update, and when a reminder fires. (The iOS sync trigger is dropped) |
| BR-R7 | Turning reminders on when notifications aren't allowed saves the setting as **off** and throws `NotificationsDenied`. **Android:** the permission dialog itself is shown by the App unit (Android needs an activity); the use case receives the resulting state |

## Settings and Onboarding (BR-G)

| ID | Rule |
|---|---|
| BR-G1 | Defaults: all 4 categories, reminder off at 20:00, onboarding not completed |
| BR-G2 | The category set can never be empty (`CategoriesEmpty`) |
| BR-G3 | Changing categories reloads the widget; it doesn't change today's already-chosen prompt (BR-P4) |
| BR-G4 | Completing onboarding saves categories, applies the reminder choice (BR-R7), sets `onboardingCompleted`, and reloads the widget. If notifications are denied, onboarding still completes with the reminder off and the error is returned for display |
| BR-G5 | Before onboarding is complete, `TodayState.onboardingCompleted == false`; the widget shows a placeholder and deep links wait until onboarding finishes |

## Misuse and Security (BR-SEC, Android)

| ID | Rule | Source |
|---|---|---|
| BR-SEC1 | The rules follow the phone's clock and time zone and don't try to detect tampering. Changing them never throws an unexpected error, never moves or deletes an entry, and never produces a negative or inconsistent streak | NFR-6.6, US-1.3, US-4.1, US-4.3 |
| BR-SEC2 | Refresh state is per day: moving the clock to another date and back gives that date its own count; the original date's count is unchanged | US-1.3 misuse scenario |
| BR-SEC3 | `editedAt` later than `now` (clock moved back during editing) is treated as `now` for BR-E4 | NFR-6.6 |
| BR-SEC4 | `DeepLink.parse` and `CalendarDay.fromKey` accept only known, well-formed input and return `null` otherwise | NFR-6.3 |
| BR-SEC5 | `JournalError.StorageFailure` carries a technical reason for logs only; no domain error message contains journal text | NFR-6.2, NFR-6.8 |

---

## Acceptance Scenario → Test Mapping (checklist step 5)

Example-based tests (plan PBT-10: every critical scenario keeps an explicit example test).

| Story scenario | Rule(s) | Test target |
|---|---|---|
| US-1.1 prompt on Today, new day | BR-P1, P2, P4 | GetTodayStateUseCase |
| US-1.1 same library as iOS | (Data unit) | Prompt file check in Data |
| US-1.2 same after relaunch, app = widget, no 30-day repeats, same as iOS | BR-P2, P3, P4, T5 | PromptSelector (30-day sweep, all 15 category sets), hand-copied iOS values (Q8:B) |
| US-1.3 refresh, limit, reset, persists, none after save, clock misuse | BR-P5, P6, P7, SEC2 | RefreshPromptUseCase |
| US-1.4 select categories, at least one, today stable | BR-G2, G3, P4 | UpdatePromptCategoriesUseCase, GetTodayState |
| US-2.1 save, empty rejected, one per day, prompt frozen, save fails | BR-E1, E2, E3, SEC5 | SaveTodayEntryUseCase (failing fake repository) |
| US-2.1 **(Android)** text too long | BR-E2 | SaveTodayEntryUseCase, BackfillYesterdayUseCase |
| US-2.2 edit same day, locked after midnight | BR-E3, E5 | SaveTodayEntryUseCase, EntryEditPolicy |
| US-2.2 **(Android)** typing across midnight: edit at 23:59:59.5 saved at 00:00:02 accepted; saved at 00:00:06 rejected; edit at 00:00:01 for yesterday rejected | BR-E4, E10, SEC3 | SaveTodayEntryUseCase |
| US-2.3 backfill offered, saved, window closes, not if exists, only yesterday | BR-E5, E8, E9 | BackfillYesterdayUseCase, EntryEditPolicy |
| US-2.4 past entry locked, today editable | BR-E5 | GetEntryUseCase |
| US-3.1 time zone change keeps the day | BR-T3 | CalendarDay, GetTodayState with a changed zone |
| US-3.2 match text, prompt, accents, none | BR-Q1–Q3 | EntrySearchMatcher, SearchEntriesUseCase |
| US-3.3 Markdown, plain text, nothing to export, same format as iOS | BR-X1–X4 | JournalExporter (iOS layout fixture), ExportJournalUseCase |
| US-4.1 streak cases, DST New York 2026-11-01, time-zone misuse | BR-S1, S3, S5, T4, SEC1 | StreakCalculator |
| US-4.2 at risk, backfill restores, today written yesterday missing | BR-S2, S4 | StreakCalculator, BackfillYesterday |
| US-4.3 reset at cutoff, restart, two missed days, clock moved back | BR-S5, S1, SEC1 | StreakCalculator |
| US-4.4 best, new record, calculated | BR-S6, S8 | StreakCalculator |
| US-5.2, US-6.3 known and unknown links | BR-SEC4 | DeepLink |
| US-6.1 enable, change time, denied, turn off | BR-R1, R2, R7 | ConfigureReminderUseCase (fake scheduler) |
| US-6.2 skip when done, fires when not done, backfill doesn't silence today | BR-R2, R4 | ReminderPlanner, SyncReminderWithTodayUseCase |
| US-7.1 onboarding saves, optional reminder, denied still completes | BR-G4 | CompleteOnboardingUseCase |
| Future-dated entry | BR-E6, E7, S (definition of D) | StreakCalculator, GetTodayState, SaveTodayEntry |
