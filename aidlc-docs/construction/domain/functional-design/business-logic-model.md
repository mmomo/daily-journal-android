# Business Logic Model: Unit 1 Domain

**Base**: iOS `business-logic-model.md`, ported (plan Q1:A). Where the iOS document and the iOS code differ, **the code wins**, because Android must reproduce iOS behavior exactly (FR-1.4). Rule ids refer to `business-rules.md`. Pseudocode shows intent; Kotlin details are settled in Code Generation.

---

## 1. PromptSelector

### 1.1 Stable hash (must match iOS)
`fnv1a64(s): ULong` = FNV-1a 64-bit over the **UTF-8 bytes** of `s`: start `0xcbf29ce484222325`; for each byte `hash = (hash xor byte) * 0x100000001b3` (wrapping 64-bit multiply). Kotlin `hashCode()` is never used.

`gcd(a, b)` = Euclid on absolute values.

### 1.2 Daily sequence (identical to the iOS code)
```
pool    = library.filter { it.category in categories }.sortedBy { it.id }      // BR-P1
n       = pool.size
if n == 1: return pool[0]
key     = categoryKey(categories)                                             // "creativity,goals,..."
offset  = (fnv1a64("offset|" + key) % n.toULong()).toInt()
step    = (fnv1a64("step|"   + key) % n.toULong()).toInt()
if step == 0: step = 1
while gcd(step, n) != 1: step = step % (n - 1) + 1                           // iOS code, not "step + 1"
base    = mod(offset + mod(day.dayNumber, n) * step, n)                       // mod(v, n) = ((v % n) + n) % n
for k in 0 until n:
    candidate = pool[(base + k * step) % n]
    if candidate.id !in answeredRecently: return candidate                   // BR-P3
return pool[base]                                                            // BR-P9: everything excluded
```
All arithmetic in `Long` to avoid overflow (iOS `Int` is 64-bit). Because `gcd(step, n) = 1`, the index visits every prompt once in any n consecutive days; with n ≥ 90 there are no repeats within 30 days.

`answeredRecently` = prompt ids of entries dated `[d − 30, d − 1]`.

### 1.3 Refresh sequence (identical to the iOS code)
```
refreshedPrompt(day d, categories, refreshNumber r, shownToday, answeredRecently):
    pool, n as above
    if n <= 1: throw NoRefreshesLeft
    start = (fnv1a64("refresh|" + d.key + "|" + categoryKey(categories) + "|" + r) % n.toULong()).toInt()
    scan(excluded) = first pool[(start + k) % n], k in 0 until n, whose id !in excluded
    return scan(shownToday ∪ answeredRecently)            // BR-P6
        ?: scan(shownToday)                               // BR-P9 step 1
        ?: scan({ shownToday.last })                      // BR-P9 step 2
        ?: throw NoRefreshesLeft
```

### 1.4 Resolving "the prompt for day d on this device"
```
resolve(d):
    state = promptState.state(d)
    if state != null and library.prompt(state.promptId) != null: return (prompt, state)    // BR-P4
    p = dailyPick(d, settings.categories, answeredRecently(before d))                       // BR-P8 also lands here
    state = DailyPromptState(d, p.id, refreshesUsed = 0, shownPromptIds = [p.id], categoryKey(...))
    promptState.save(state, keepingFrom = today − 1)
    return (p, state)
```

---

## 2. EntryEditPolicy

```
editability(day, hasEntry, now, tz):
    today = CalendarDay.containing(now, tz)
    if day == today: return Editable(until = (today + 1).startIn(tz))
    if day == today − 1 and !hasEntry and now < today.noonIn(tz): return Backfillable(until = today.noonIn(tz))
    return Locked                                           // past, missed after deadline, future (BR-E6)

lateSaveAllowed(day, editedAt, now, tz):                    // BR-E4, Android
    midnight = (day + 1).startIn(tz)
    edited   = min(editedAt, now)                           // BR-SEC3
    return now >= midnight and edited < midnight and now < midnight + 5.seconds
```

DST: `startIn` and `noonIn` are computed by the calendar in `tz` (BR-T4). A time-zone change recomputes `today` from `now`; entry days never move (BR-T3).

---

## 3. StreakCalculator

```
summary(entryDays, now, tz):
    today = containing(now, tz); y = today − 1; y2 = today − 2; noon = today.noonIn(tz)
    D = { d.dayNumber | d in entryDays, d <= today } ∪ ({today.dayNumber} if any d > today)
    run(x) = consecutive count in D ending at x (0 if x ∉ D), iterative

    if today ∈ D:
        if y ∉ D and now < noon and run(y2) > 0: current = 1;           status = AtRisk(run(y2), noon)   // BR-S2
        else:                                    current = run(today);  status = Active                  // BR-S1
    else if y ∈ D:                               current = run(y);      status = Active                  // BR-S3
    else if now < noon and run(y2) > 0:          current = run(y2);     status = AtRisk(run(y2), noon)   // BR-S4
    else:                                        current = 0;           status = None                    // BR-S5
    best = max(longestRun(D), current)                                                                  // BR-S6
    return StreakSummary(current, best, status, isTodayDone = today ∈ D)
```

Cost O(k log k) for k entry days; about 3,650 after ten years.

---

## 4. EntrySearchMatcher

```
normalize(s) = removeCombiningMarks(NFD(s)).lowercase()      // Android: java.text.Normalizer, root locale
matches(entry, query):
    q = query.trim()
    if q.isEmpty(): return true
    return normalize(entry.text).contains(normalize(q)) || normalize(entry.promptText).contains(normalize(q))
```

The Data unit may run the search inside the database for speed, but its results must equal this matcher's; a shared fixture checks both (as on iOS).

---

## 5. Text validation (BR-E2)

```
isValidText(text)  = text.any { !it.isUnicodeWhitespace() }
isWithinLimit(text) = text.codePointCount(0, text.length) <= 20_000
validate(text): if !isValidText → EmptyText; if !isWithinLimit → TextTooLong
```

---

## 6. JournalExporter (identical layouts to iOS)

**Markdown** (`daily-journal-2026-10-08.md`):
```
# Daily Journal

Exported 2026-10-08 · 3 entries

## 2026-10-01

> What small thing made today better?

Grateful for a quiet morning.

## 2026-10-05
...
```

**Plain text** (`daily-journal-2026-10-08.txt`):
```
DAILY JOURNAL
Exported 2026-10-08 - 3 entries

2026-10-01
Prompt: What small thing made today better?

Grateful for a quiet morning.

----------------------------------------

2026-10-05
...
```

Oldest first (BR-X2); entry text exactly as stored; line endings `\n`; "1 entry" singular. The Code Generation stage copies the exact iOS output for a fixed set of entries as a test fixture.

---

## 7. Reminder window (ReminderPlanner + SyncReminderWithTodayUseCase)

```
plan(settings, isAllowed, streak, now, tz):
    if !settings.isEnabled or !isAllowed: return []                              // BR-R1
    today = containing(now, tz)
    for offset in 0 until 7:                                                      // BR-R2
        d = today + offset
        fire = instant of d at settings.hour:minute in tz (next valid instant in a DST gap)   // BR-R3
        if fire <= now: continue
        if offset == 0 and streak.isTodayDone: continue
        body = "Your daily prompt is waiting."
        if offset == 0 and streak.status == Active and streak.current >= 1:
            body += " Keep your ${streak.current}-day streak going."              // BR-R4
        add ScheduledReminder("reminder-" + d.key, d, fire, "Time to journal", body, TodayEditor)   // BR-R5

SyncReminderWithToday.execute():
    reminders = plan(settings.load().reminder, scheduler.authorization() == ALLOWED,
                     StreakCalculator.summary(entries.entryDays(), now, tz), now, tz)
    scheduler.replacePending(reminders)
    // best effort: storage or scheduler errors are caught and logged without journal text
```

Running it twice gives the same result (idempotent).

---

## 8. Use Case Flows

| # | Use case | Flow | Errors |
|---|---|---|---|
| 1 | GetTodayState | today; settings; entry(today), else the earliest entry after today → shown entry. Prompt = shown entry's frozen prompt, or `resolve(today)`. refreshesRemaining = done ? 0 : 3 − used. Editability via policy (`Locked` if the shown entry is future-dated). Backfill offer if yesterday is `Backfillable`, prompt = `resolve(yesterday)`. Streak via calculator | `StorageFailure` |
| 2 | RefreshPrompt | If today has an entry or a future entry exists → `EntryLocked`. `(p, s) = resolve(today)`; if `s.refreshesUsed >= 3` → `NoRefreshesLeft`. `next = refreshedPrompt(r = used + 1)`. Save state (promptId, used + 1, shown + next). Widget reload | `EntryLocked`, `NoRefreshesLeft`, `StorageFailure` |
| 3 | SaveTodayEntry(text, day, editedAt) **(refined: `editedAt` added, BR-E4)** | Validate (BR-E2). If a future-dated entry exists → `EntryLocked` (BR-E7). If `day == today`: editability must be `Editable`. Else if `lateSaveAllowed(day, editedAt, now, tz)`: allowed. Else → `EntryLocked`. Create with `resolve(day)` or update text and `updatedAt = now`. Save. Reminder sync. Widget reload | `EmptyText`, `TextTooLong`, `EntryLocked`, `StorageFailure` |
| 4 | BackfillYesterday(text, day) | Validate. Editability(yesterday, hasEntry) must be `Backfillable`, else `BackfillNotAvailable`. Create with `resolve(yesterday)`. Save. Widget reload. Reminder sync | `EmptyText`, `TextTooLong`, `BackfillNotAvailable`, `StorageFailure` |
| 5 | GetEntry(day) | entry(day) and its editability | `StorageFailure` |
| 6 | ListEntries | allEntries, newest first | `StorageFailure` |
| 7 | SearchEntries(q) | BR-Q1–Q3 | `StorageFailure` |
| 8 | ExportJournal(format) | allEntries; empty → `NothingToExport`; exporter | `NothingToExport`, `StorageFailure` |
| 9 | GetStreak | calculator(entryDays) | `StorageFailure` |
| 10 | GetSettings | settings.load() | `StorageFailure` |
| 11 | UpdatePromptCategories(set) | Empty → `CategoriesEmpty`. Save. Widget reload | `CategoriesEmpty`, `StorageFailure` |
| 12 | ConfigureReminder(r) | If enabling and `authorization() != ALLOWED` → save with reminder off, throw `NotificationsDenied`. Else save, then SyncReminderWithToday | `NotificationsDenied`, `StorageFailure` |
| 13 | SyncReminderWithToday | §7 | none (best effort) |
| 14 | CompleteOnboarding(categories, reminder?) | Empty → `CategoriesEmpty`. Save categories. If a reminder is given → ConfigureReminder (catch `NotificationsDenied`, remember it). Set onboardingCompleted. Widget reload. Rethrow a remembered denial after completing (BR-G4) | `CategoriesEmpty`, `NotificationsDenied` (non-blocking) |

Not ported: iOS use case 15, ReconcileSyncedEntries (sync only).

**Refinements to `component-methods.md`** (applied in Code Generation):
- `SaveTodayEntryUseCase.execute(text, day, editedAt: Instant)`
- `ConfigureReminderUseCase.execute(reminder)` throws `NotificationsDenied` instead of returning the authorization (same as iOS)
- `ReminderSettings` uses `hour` and `minute`
- `CalendarDay.dayNumber` counts from 2001-01-01
- `JournalError` gains `TextTooLong`, `NotificationsDenied`, `NothingToExport`

---

## 9. Contracts for the Data Unit

### 9.1 `prompts.json`
Same file and format as iOS: `{"version": 1, "prompts": [{"id", "category", "text"}]}`. Validation: version 1; ids unique and matching `^(gratitude|reflection|goals|creativity)-\d{3}$`; id prefix equals category; trimmed text 10–200 characters; no duplicate texts (case-insensitive); ≥ 90 per category and ≥ 365 total. **Android:** the Data test also checks exactly 600 prompts, 150 per category.

### 9.2 Entries
One row per day, unique on the day key; `dayNumber` stored for sorting and ranges; text up to 20,000 code points. The storage library and schema are decided in the Data unit.

### 9.3 Settings and prompt state
Stored privately on the device; only today's and yesterday's prompt states kept. Format decided in the Data unit (iOS uses JSON for both; matching it is optional on Android).

---

## 10. Testable Properties (PBT-01)

### 10.1 Properties by component

| Component | Property | Category |
|---|---|---|
| `CalendarDay` | `fromKey(d.key) == d` for every generated day | Round-trip |
| `CalendarDay` | `d.adding(n).dayNumber == d.dayNumber + n`; `d.adding(n).adding(-n) == d`; ordering by `dayNumber` matches ordering by (year, month, day) | Invariant |
| `CalendarDay` | `containing(d.startIn(tz), tz) == d` and `containing(d.noonIn(tz), tz) == d` for every day and zone, including DST days | Round-trip |
| `StableHash` | Deterministic: same string → same value | Invariant |
| `PromptSelector.dailyPrompt` | Deterministic; result is in the selected categories; over any n consecutive days (n = pool size) every prompt appears exactly once when nothing is excluded; never returns a prompt in `answeredRecently` while others remain | Invariant |
| `PromptSelector.refreshedPrompt` | Never returns a prompt in `shownToday` while unshown prompts remain; deterministic for the same inputs | Invariant |
| `EntryEditPolicy` | Only today is `Editable`; only a missing yesterday before noon is `Backfillable`; every other day is `Locked` | Invariant |
| `lateSaveAllowed` | Never allows a save more than 5 s after midnight or for an edit at or after midnight | Invariant |
| `StreakCalculator` | `0 ≤ current ≤ |D|`; `best ≥ current`; adding a day to D never lowers current or best at the same instant | Invariant |
| `StreakCalculator` | `best` equals a simple reference: sort the days and count the longest consecutive run | Oracle (PBT-05) |
| `isValidText` / limit | Text is valid exactly when it has a non-whitespace character and at most 20,000 code points | Invariant |
| `EntrySearchMatcher` | Results ⊆ entries; empty query matches all; adding accents or changing case in the query doesn't change matches | Invariant |
| `EntrySearchMatcher` | Matches a simple reference (normalize both, `contains`) | Oracle |
| `JournalExporter` | Each entry appears exactly once, in date order, with its text unchanged | Invariant |
| `ReminderPlanner` | At most 7 reminders, at most one per day, all in the future, none for today when done; planning twice gives the same list | Invariant, Idempotence |
| `DeepLink` | `parse(link.toUri()) == link`; random strings parse to `null` unless they are a known link | Round-trip, Invariant |
| `RefreshPromptUseCase` (stateful) | Over any sequence of refreshes on one day (with a fake store), at most 3 succeed and the shown list has no duplicates; model: a counter and a list | Stateful (PBT-06) |
| `SaveTodayEntryUseCase` (stateful) | Over any sequence of saves on one day, exactly one entry exists and it holds the last valid text; model: an optional string | Stateful (PBT-06) |

**PBT-05 for the iOS comparison: N/A.** The reference implementation is Swift code in another repository and can't run inside Kotlin tests. Parity with iOS is checked with hand-copied iOS values as example-based tests (plan Q8:B).

**No PBT properties**: `TodayState`, `BackfillOffer`, `EntryDetail` and other plain data holders (no behavior); `GetSettingsUseCase`, `ListEntriesUseCase`, `GetEntryUseCase` (pass-through; covered by examples).

### 10.2 Generators (PBT-07; clarification C2:A)

Reusable generators in `domain/src/test/.../support/`:

| Generator | Values |
|---|---|
| Day | 2024-01-01 to 2032-12-31 (leap years 2024 and 2028, many DST changes), plus the edges of that range |
| Time zone | 8 zones: `America/Mexico_City`, `America/New_York`, `Europe/London`, `Asia/Kolkata` (UTC+5:30), `Asia/Kathmandu` (UTC+5:45), `Australia/Lord_Howe` (30-minute DST), `Pacific/Kiritimati` (UTC+14), `Etc/GMT+12` (UTC−12) |
| Instant | A generated day and zone plus a time of day, biased toward 00:00, 11:59:59, 12:00 and 23:59:59 |
| Category set | All 15 non-empty combinations |
| Entry text | Empty, whitespace-only (spaces, tabs, newlines, non-breaking space), short plain, accented, emoji, long (up to 20,000 code points) and just over the limit |
| Entry-day set | Random sets of generated days, including empty, consecutive runs and gaps |
| Query | Substrings of generated texts with case and accents changed, plus unrelated strings |

### 10.3 Seeds and reproducibility (PBT-08; plan Q7:A)
A new random seed on every run. On failure the framework prints the seed and the shrunk minimal input; the failing case can be replayed by fixing that seed. CI behaves the same and keeps the test output. Shrinking stays enabled. Each failure found becomes a permanent example-based regression test (PBT-10).

### 10.4 Framework (PBT-09)
Kotest property testing (requirements NFR-4.3); confirmed with versions in NFR Requirements.

---

## 11. Open Points

1. **iOS parity values** (Q8:B): hand-copied from the iOS tests where they exist (hash values, streak and editability scenarios). For daily picks, a handful of values will be produced by running the iOS selector once and copying the results into the Android tests.
2. **Whitespace and accent handling** may differ from iOS for unusual Unicode characters outside the test generators (for example zero-width characters). Accepted: behavior is identical for all text in the generators and iOS fixtures.
