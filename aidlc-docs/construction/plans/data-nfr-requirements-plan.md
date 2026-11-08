# NFR Requirements Plan: Data Unit

**Inputs**: `construction/data/functional-design/` (approved), domain NFR requirements and tech stack (project-wide policies apply: latest stable versions pinned, official Kotlin/AndroidX/Google libraries only in shipped code, checksum verification, Kotest), requirements NFR-2, NFR-5.2, NFR-6, NFR-7.2.

**Already decided** (not asked again): private storage only, no backup, no extra database encryption (C4:A); iOS-shaped JSON for settings and prompt state; search by loading all entries; one entry per day by a unique key; non-destructive migrations.

**Categories considered**:
- Tech stack (storage, JSON, reminders): Questions 1–5
- Performance: Question 6
- Maintainability and testing: Questions 7–8
- Security (logging): Question 8
- **Scalability**: N/A (one user, about 3,650 entries in ten years; covered by Question 6)
- **Availability**: N/A (on-device; data-loss risk accepted in NFR-2.3)
- **Usability**: N/A (no UI in this unit)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
Which library should store the entries?

A) Room (AndroidX): typed queries checked at compile time, built-in change notifications, and tools for testing migrations. Adds a code generator (KSP) to the build

B) The plain AndroidX SQLite API without Room: no code generator, but queries, mapping and change signals are written by hand

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
Which library should store settings and the prompt state?

A) Jetpack DataStore (Preferences): asynchronous, safe atomic updates, and a built-in `Flow` of changes

B) Android's built-in SharedPreferences: no extra library, but synchronous and change signals are hand-written

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How should JSON (prompt file, settings, prompt state) be read and written?

A) kotlinx-serialization (official Kotlin library): typed classes, ignores unknown fields, fills defaults for missing ones

B) Android's built-in `org.json`: no extra library, but parsing and defaults are written by hand

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 4
How exact must reminders be? Android may delay ordinary alarms by several minutes to save battery. Exact alarms need the "Alarms & reminders" special permission on Android 12+, which Android 14+ doesn't grant by default, so the user would have to turn it on in system settings.

A) Inexact: use battery-friendly alarms that may arrive a few minutes after the set time; no extra permission

B) Exact: ask for the "Alarms & reminders" permission; if the user doesn't grant it, fall back to inexact alarms

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
How many of the 7 planned reminders should be registered with Android at a time?

A) Only the next one; when it fires (or whenever reminders are recomputed), the following one is registered. Fewer system alarms; the plan is recomputed often anyway

B) All 7 planned reminders, like iOS

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
What speed should the data layer guarantee (NFR-5.2)?

A) Explicit targets checked by tests: with 3,650 entries, loading all entries plus searching under 200 ms, and reading Today's data (entry, entry days, settings, prompt state) under 100 ms, on the test machine; device speed confirmed in Build and Test

B) No explicit data-layer targets; check speed on the device later

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 7
How should the data layer be tested? Room, DataStore and notifications need Android classes.

A) Mostly fast JVM tests using Robolectric (a test-only library that simulates Android, maintained with Google's involvement), run in the normal `build` locally and in CI; no emulator needed

B) Android instrumented tests on an emulator or phone (closest to reality, but slower, and CI needs an emulator)

C) Both: JVM tests with Robolectric for everything, plus a few instrumented tests for database migrations and notifications run manually during Build and Test

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8
How should the data layer log?

A) Through a tiny `JournalLog` helper over Android's logger that only accepts ids, counts and error types (never free text), so journal text can't be logged by accident (NFR-6.2)

B) Android's logger directly, with reviews checking that no journal text is logged

C) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/data/nfr-requirements/nfr-requirements.md`: performance, reliability (corrupt data, failures, migrations), security (private storage, logging, intents, permissions), maintainability (tests, PBT), N/A categories
- [x] 2. Write `aidlc-docs/construction/data/nfr-requirements/tech-stack-decisions.md`: libraries with versions policy, reminder mechanism, test tooling, generators shared from the domain, with rationale
- [x] 3. Security and PBT compliance review
- [x] 4. Check consistency with the data functional design and project-wide policies
