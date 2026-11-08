# Functional Design Plan: Domain Unit

**Unit**: Domain (`:domain`, pure Kotlin). See `aidlc-docs/inception/application-design/unit-of-work.md`.
**Stories**: the business-rule parts of every story except US-2.6 (`unit-of-work-story-map.md`).
**Starting point**: the approved iOS domain functional design (`demo/aidlc-docs/construction/domain/functional-design/`), which defines the rules by id: time (BR-T1–T4), prompts (BR-P1–P9), entries (BR-E1–E10), streak (BR-S1–S8), merge (BR-M1–M6), search (BR-Q1–Q3), export (BR-X1–X4), reminders (BR-R1–R7), settings and onboarding (BR-G1–G5).

**Categories considered**:
- Business logic, domain model, business rules, data flow, error handling, business scenarios: Questions 1–5
- **Integration points**: N/A for this unit. The domain talks only to its own ports; no external systems (NFR-2.1)
- **Frontend components**: N/A. The domain has no UI; screens are in the App unit
- **Property-based testing** (PBT extension, rule PBT-01): Questions 6–8

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should the iOS business rules be carried over?

A) Port them with the same rule ids and wording (BR-T, BR-P, BR-E, BR-S, BR-Q, BR-X, BR-R, BR-G), drop the sync-only merge rules (BR-M), and mark every Android difference "Android:", so the two designs can be compared rule by rule

B) Rewrite the rules from the Android requirements and stories

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
**Saving across midnight.** The iOS rule BR-E4 rejects every save to yesterday's entry once midnight has passed. But requirement FR-8.1 says text typed before midnight must be kept. With autosave waiting about a second after typing stops, the last words typed just before midnight would arrive after midnight and be rejected. How should the domain handle this?

A) Each save carries the instant the text was last edited. A save for yesterday's entry is accepted if that edit happened before midnight and the save arrives within a short grace of a few seconds after midnight; anything later is rejected as on iOS

B) Keep BR-E4 strictly as on iOS; the editor saves immediately (without the autosave pause) in the last few seconds before midnight, and accepts that a word typed in the final moment might be lost

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 3
Should entries have a maximum length? iOS has none (BR-E2). The Security extension asks for length limits on inputs (SECURITY-05); there are no APIs here, but a limit also protects storage and export.

A) No maximum, same as iOS

B) A generous maximum of 20,000 characters (about 3,500 words, far beyond a daily entry); the editor shows a notice near the limit and stops accepting text past it

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 4
iOS treats an entry dated after today (possible after travelling west across time zones) as "today is done", shows it read-only, and blocks writing a new one until that date arrives (BR-E6, BR-E7). Keep this on Android?

A) Yes, same as iOS

B) No: ignore future-dated entries for today, and allow writing today's entry anyway

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
iOS plans reminders for a 7-day window because iOS limits how many notifications an app can schedule (BR-R2). How should the Android domain plan reminders?

A) Keep the same 7-day plan in the domain, so the rule and its tests match iOS; the Data unit decides how many of those reminders to actually register with Android

B) Plan only the next reminder; each reminder schedules the following one when it fires

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
What range of inputs should the property-based tests generate (PBT-07)?

A) Wide: dates from 1970 to 2100; every time zone in the standard time-zone database, including daylight-saving zones and the extremes (UTC−12 to UTC+14); entry text including empty, whitespace-only, emoji, accented and very long strings; all 15 non-empty category combinations

B) Narrow: dates within a few years of today, a handful of representative time zones, plain text

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 7
How should property-based test runs be made reproducible (PBT-08)?

A) A new random seed on every run, printed when a test fails so the failure can be replayed exactly; CI does the same (finds more edge cases over time)

B) A fixed seed everywhere, so every run checks exactly the same inputs

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8
How should the tests prove the Android prompt selection matches iOS exactly (requirement FR-1.4)?

A) Generate a fixture file once from the iOS code (for example the hash of fixed strings and the daily pick for 1,000 dates across all 15 category combinations) and commit it to the Android tests as an oracle (PBT-05). The fixture contains only dates, category sets and prompt ids, which are already public in this repo

B) Copy a handful of expected values from the iOS tests by hand

C) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Design Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/domain/functional-design/domain-entities.md`: every entity and value type with fields, constraints and invariants (Kotlin-neutral where possible)
- [x] 2. Write `aidlc-docs/construction/domain/functional-design/business-rules.md`: all rules per Question 1, with the decisions from Questions 2–5, and the security misuse rules (clock and time-zone changes, NFR-6.6)
- [x] 3. Write `aidlc-docs/construction/domain/functional-design/business-logic-model.md`: algorithms (prompt selection and hash, refresh, editability, streak, search normalization, export layouts, reminder planning, today state) and use case flows with error cases
- [x] 4. Add a **Testable Properties** section (PBT-01): properties per component with their category (round-trip, invariant, idempotence, oracle, stateful), generator ranges per Question 6, seed policy per Question 7, and the iOS oracle fixture per Question 8
- [x] 5. Map every acceptance scenario from the E1, E2 and E4 stories (and the business-rule parts of other stories) to an example-based test case
- [x] 6. Check consistency with requirements, stories and application design; list any open points
