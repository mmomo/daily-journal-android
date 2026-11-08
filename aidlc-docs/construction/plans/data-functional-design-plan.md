# Functional Design Plan: Data Unit

**Unit**: Data (`:data`, Android library). See `aidlc-docs/inception/application-design/unit-of-work.md`.
**What it does**: implements the domain ports on Android (entries, settings, per-day prompt state, prompt library, clock, reminders) and wires everything together (`JournalEnvironmentFactory`).
**Inputs**: domain ports and contracts (`construction/domain/functional-design/business-logic-model.md` §9), application design (`components.md` §2), requirements NFR-2, NFR-6.7, NFR-7.2.

**Already decided** (not asked again): one entry per day enforced by storage; entries, settings and prompt state stored only in the app's private storage; no backup; the same `prompts.json` as iOS with a 600-prompt check; only today's and yesterday's prompt states kept; non-destructive database changes between versions (NFR-7.2). Storage library and reminder mechanism are chosen in NFR Requirements.

**Categories considered**:
- Domain model and data flow: Questions 1–3
- Error handling and business scenarios: Questions 4–6
- Integration points (Android system events): Question 6
- **Frontend components**: N/A (no UI in this unit)
- Property-based testing (PBT-01): the design lists the data unit's testable properties (round-trips of stored formats, search equivalence)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should settings and the per-day prompt state be stored?

A) As JSON documents under fixed keys, the same shape as iOS (`settings.v1`, `promptState.v1`), so the stored format is identical across platforms and easy to version

B) As individual typed values (one key per setting: categories, reminder on/off, hour, minute, onboarding), with the prompt state in the database next to entries

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
How should search work at the storage level? The domain checks every result with its accent-insensitive matcher either way.

A) Load all entries and filter in memory with the domain matcher. Simple and always identical to the domain rules; about 3,650 entries after ten years is small

B) Store an accent-free, lowercase copy of each entry's text and prompt in the database and search those columns, then confirm with the domain matcher. Faster for very large journals, but more to store and keep in sync

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How should stored dates and times be represented in the database?

A) Day as its `YYYY-MM-DD` key (unique) plus its day number for sorting and ranges; created and updated times as UTC milliseconds since 1970. The same values the iOS store keeps

B) Day as its key only; times as ISO-8601 text

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
What should happen if stored settings or prompt state can't be read (corrupted or from an unknown future version)?

A) Same as iOS: fall back to the defaults (settings) or to "no state" (prompt state, so the day's prompt is picked again), log only the error type, and overwrite with a valid value on the next save

B) Show an error and stop until the user resets the app

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
What should happen if an entry row can't be read (for example an invalid day key)?

A) Skip that row in lists, the streak and search, log only its id and the error type, and leave the row untouched in storage so nothing is deleted (same as iOS)

B) Fail the whole operation with a storage error

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
When should the clock signal (`ClockEvents`) tell screens that the day may have changed?

A) At the next midnight and next 12:00, when the system time, date or time zone changes, and when the app returns to the foreground (in case the phone slept through a boundary)

B) Only at midnight and 12:00, and on system time or time zone changes

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Design Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/data/functional-design/domain-entities.md`: stored data model (entry table, settings and prompt state formats, prompt file), keys, constraints, versioning
- [x] 2. Write `aidlc-docs/construction/data/functional-design/business-rules.md`: rules for each port implementation (one per day, ordering, search, pruning, corrupt data, prompt file validation and the 600-prompt check, reminder registration, clock boundaries, environment wiring), with acceptance mapping to stories
- [x] 3. Write `aidlc-docs/construction/data/functional-design/business-logic-model.md`: flows for each port (read, write, change signals), mapping between stored rows and domain entities, migration rules, reminder and clock flows, factory wiring
- [x] 4. Add the Testable Properties section (PBT-01): round-trips (entry ↔ row, settings and prompt state ↔ stored form), search equivalence with the domain matcher, ordering and pruning invariants, generators reused from the domain tests where possible
- [x] 5. Check consistency with the domain contracts, requirements and stories; list open points
