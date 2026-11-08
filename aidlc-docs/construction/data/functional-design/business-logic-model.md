# Business Logic Model: Unit 2 Data

How each port implementation behaves. Rule ids refer to `business-rules.md`; storage and scheduling technology are settled in NFR Requirements and Code Generation.

---

## 1. Entry Repository

```
save(entry):                                   // DATA-E1
    row = toRow(entry)                         // day_key, day_number from entry.day; times in UTC ms
    if a row with row.id exists: update it
    else insert (fails if day_key already used by another id → exception → StorageFailure)
    emit changes                               // DATA-E6

entry(day)            = rowByDayKey(day.key)?.toDomainOrSkip()
entries(from, through)= rows where day_number in from.dayNumber..through.dayNumber, mapped, unreadable skipped
entriesAfter(day)     = rows where day_number > day.dayNumber, mapped
allEntries()          = all rows, mapped
entryDays()           = allEntries().map { it.day }.toSet()
search(query)         = allEntries().filter { EntrySearchMatcher.matches(it, query) }    // DATA-E4

toDomainOrSkip(row):                           // DATA-E5
    day = CalendarDay.fromKey(row.day_key) ?: log("unreadable entry", row.id, "InvalidDayKey"); return null
    return JournalEntry(row.id, day, row.text, row.prompt_id, row.prompt_text,
                        Instant.fromEpochMilliseconds(row.created_at), Instant.fromEpochMilliseconds(row.updated_at))
```

## 2. Settings Store

```
load():
    json = preferences["settings.v1"] ?: return AppSettings()                 // DATA-S2
    try decode (ignore unknown fields, defaults for missing) → AppSettings(categories, ReminderSettings(...), onboarding)
    catch: log(error type); return AppSettings()

save(settings):
    preferences["settings.v1"] = encode(settings)   // iOS shape (domain-entities §2)
    emit changes
```

## 3. Prompt State Store

```
state(day)               = loadMap()[day.key]
save(state, keepingFrom) = atomically:                                         // DATA-S4, S5
    map = loadMap(); map[state.day.key] = state
    map = map.filterKeys { CalendarDay.fromKey(it)?.let { d -> d >= keepingFrom } ?: false }
    preferences["promptState.v1"] = encode(map)
  then emit changes
loadMap() = decode or empty map on any error (log error type)                   // DATA-S3
```

## 4. Prompt Library

```
load once:
    file = decode(assets/prompts.json); require(file.version == 1)            // DATA-P1
    prompts = file.prompts.map { Prompt(id, PromptCategory.fromRawValue(category) ?: invalid, text) }
    PromptLibraryValidator.validate(prompts)                                   // DATA-P2
    byId = prompts.associateBy { it.id }
```

## 5. Reminders

```
authorization():                                                               // DATA-R1
    if !notificationsEnabledForApp: return DENIED
    if sdk >= 33 and !permissionGranted:
        return if preferences["notificationPermissionAsked.v1"] then DENIED else NOT_DETERMINED
    return ALLOWED

replacePending(reminders):                                                     // DATA-R2
    cancel everything previously registered by this app
    register reminders (how many and how: NFR Requirements)

On fire (App unit's ReminderReceiver): re-check today isn't done, then ReminderNotifier.post(reminder), then sync again.

ReminderNotifier.post(reminder):                                               // DATA-R4
    ensure channel "Daily reminder"
    notificationId = reminder.day.dayNumber
    tap = explicit, immutable intent to the main activity with data = reminder.deepLink.toUri()
    show(title, body, tap, autoCancel)
```

## 6. Clock Events

```
boundaries: Flow<Unit> = callbackFlow while collected:                         // DATA-C2, C3
    register receiver for TIME_SET, DATE_CHANGED, TIMEZONE_CHANGED → emit
    observe app foreground (process lifecycle ON_START) → emit
    loop:
        now = time.now; tz = time.timeZone; today = containing(now, tz)
        next = min(future of { today.noonIn(tz), today.adding(1).startIn(tz) })
        delay(next - now); emit
    on close: unregister everything
```

## 7. Wiring

```
JournalEnvironmentFactory.create(context, widgets, onBackgroundFailure):      // DATA-W1
    db = open private database (schema v1, explicit migrations only)
    prefs = private preferences
    time = SystemTimeSource()
    useCases = JournalUseCases(time, DatabaseEntryRepository(db), BundledPromptLibrary(context),
                               PreferencesPromptStateStore(prefs), PreferencesSettingsStore(prefs),
                               AndroidReminderScheduler(context, prefs), widgets, onBackgroundFailure)
    return JournalEnvironment(useCases, ClockEvents(context, time), change flows)
```

---

## 8. Testable Properties (PBT-01)

| Component | Property | Category |
|---|---|---|
| Entry row mapping | `toDomain(toRow(entry)) == entry` (times at millisecond precision) for generated entries, including emoji, accents and 20,000-character texts | Round-trip |
| Settings JSON | `decode(encode(settings)) == settings` for generated settings | Round-trip |
| Prompt state JSON | `decode(encode(map)) == map` for generated maps | Round-trip |
| Settings and prompt state decoding | Arbitrary text never throws; it decodes to valid values or the defaults | Invariant |
| Repository ranges | `entries(from, through)` equals `allEntries()` filtered by the range; `entriesAfter(d)` likewise | Oracle |
| Repository search | `search(q)` equals `allEntries()` filtered with `EntrySearchMatcher` | Oracle |
| Repository saves (stateful) | Over a random sequence of saves of entries for random days, the store equals a map model `day → entry`, and a different id for an existing day always fails | Stateful |
| Prompt state pruning | After `save(state, keepingFrom)`, no stored state is older than `keepingFrom`, and the saved state is present when its day ≥ `keepingFrom` | Invariant |
| Clock boundaries | The next boundary is always after `now`, is either the next midnight or 12:00, and lies within 24 hours (plus at most one DST hour) | Invariant |

Generators: the domain's test generators (days, zones, texts, category sets) are shared with the data tests through a test-fixtures source set on `:domain`, so both units test with the same inputs (PBT-07: reusable generators).

**PBT-05 for iOS**: the iOS JSON samples in §2–§3 are checked as example-based fixtures (iOS code can't run inside Kotlin tests).

---

## 9. Open Points

1. Exact reminder registration (all 7 planned reminders, or only the next) and exactness (exact vs inexact alarms) are decided in NFR Requirements.
2. `notificationPermissionAsked.v1` is new on Android (not in iOS); it is written by the App unit right after the permission dialog.
