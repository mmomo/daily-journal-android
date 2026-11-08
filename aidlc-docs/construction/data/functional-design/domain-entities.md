# Data Model: Unit 2 Data (`:data`)

**Plan**: `data-functional-design-plan.md` (Q1–Q6, all A). Everything is stored in the app's private storage only (NFR-2); nothing is backed up or synced.

---

## 1. Entries (database table `entries`)

| Column | Type | Constraint | Domain field |
|---|---|---|---|
| `id` | text | primary key | `JournalEntry.id` (UUID text) |
| `day_key` | text | **unique**, not null | `day.key` (`YYYY-MM-DD`) |
| `day_number` | integer | not null, indexed | `day.dayNumber` (days since 2001-01-01) |
| `text` | text | not null | `text` (up to 20,000 code points) |
| `prompt_id` | text | not null | `promptId` |
| `prompt_text` | text | not null | `promptText` |
| `created_at` | integer | not null | `createdAt`, UTC milliseconds since 1970 |
| `updated_at` | integer | not null | `updatedAt`, UTC milliseconds since 1970 |

- **One entry per day** is enforced by the unique `day_key` (Q3:A, application design).
- `day_number` serves sorting and range queries; it is always derived from `day_key` when writing.
- No relationships; no deletion (FR-2.9).
- Times keep millisecond precision; finer precision of `Instant` is dropped on write (round-trip is equal at millisecond precision).
- **Schema version 1.** Later versions add explicit migrations; destructive fallback is never enabled (NFR-7.2).

## 2. Settings (private key-value storage, key `settings.v1`) — same JSON as iOS (Q1:A)

```json
{
  "categories": ["gratitude", "reflection", "goals", "creativity"],
  "reminder": { "isEnabled": false, "hour": 20, "minute": 0 },
  "onboardingCompleted": false
}
```

- Category raw values as in the domain; order in the array has no meaning.
- Unknown fields are ignored when reading (forward compatibility); missing fields take their defaults.

## 3. Per-day prompt state (key `promptState.v1`) — same JSON as iOS (Q1:A)

```json
{
  "2026-10-08": {
    "day": { "year": 2026, "month": 10, "day": 8 },
    "promptID": "reflection-098",
    "refreshesUsed": 1,
    "shownPromptIDs": ["reflection-098", "reflection-125"],
    "categoriesKey": "creativity,goals,gratitude,reflection"
  }
}
```

- A map from day key to state; only today's and yesterday's entries are kept.
- Field names match the iOS JSON exactly (`promptID`, `shownPromptIDs`).

## 4. Notification permission flag (key `notificationPermissionAsked.v1`, Android)

`true` once the App unit has shown the Android 13+ permission dialog. Needed to tell `NOT_DETERMINED` (never asked) from `DENIED` (asked and refused), which Android doesn't report directly.

## 5. Prompt library (asset `data/src/main/assets/prompts.json`)

A copy of the iOS file (unit of work Q5:A): `{"version": 1, "prompts": [{"id", "category", "text"}]}`, 600 prompts, 150 per category, ids `<category>-001` to `-150`.

## 6. Mapping to Domain Types

| Stored | Domain | Rule |
|---|---|---|
| Entry row | `JournalEntry` | `day_key` parsed with `CalendarDay.fromKey`; rows that don't parse are skipped (DATA-E5) |
| `settings.v1` | `AppSettings` | Built through the `AppSettings` constructor, so empty categories fall back to all four and the reminder time is clamped |
| `promptState.v1` | `Map<CalendarDay, DailyPromptState>` | Map keys parsed with `fromKey`; invalid keys dropped |
| `prompts.json` | `List<Prompt>` | Validated with `PromptLibraryValidator` plus the 600-prompt check |
