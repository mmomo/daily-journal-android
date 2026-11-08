# NFR Design Plan: Data Unit

**Inputs**: `construction/data/nfr-requirements/` (approved), data functional design, domain NFR design (patterns already set: typed errors through `withStorage`, one write lock, main-safe ports, best-effort side effects).

**Fixed by the platform or earlier decisions** (not asked): one Room database and one DataStore per process (DataStore forbids two instances on the same file); storage work on an injectable IO dispatcher; explicit immutable intents; only the next reminder registered with a fixed request code.

**Categories considered**:
- **Resilience patterns**: Questions 2 and 3 (bad prompt file, corrupted preferences file)
- **Performance patterns**: Question 2 (loading the prompt library)
- **Logical components and integration**: Questions 1 and 4 (change signals, clock timer)
- **Security patterns**: already settled in NFR Requirements (DATA-SEC-1 to 8); applied in the design document
- **Scalability patterns**: N/A (one user, about 3,650 entries)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
Where should the "entries changed" signal come from?

A) Room's built-in table observation: Room notices every write to the `entries` table and emits automatically, even for writes added later in other code

B) A hand-made signal the repository emits after each of its own writes (simple, but any future write path must remember to emit)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
When should the prompt library be loaded and validated?

A) Eagerly, when the environment is created at app start: a broken prompt file shows the startup error immediately, and the parsed prompts stay in memory (600 short prompts, about 100 KB)

B) Lazily, on first use: slightly faster app start, but a broken file would only surface when Today first loads

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
What should happen if the preferences file itself is corrupted (not just one value)? Settings, prompt state and the permission flag live in it; entries are in the database and aren't affected.

A) Replace it with an empty file and log the error type. Settings return to defaults, so onboarding shows again; the journal itself is untouched. The app keeps working

B) Keep failing with a storage error until the user clears the app's data from system settings

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
How should `ClockEvents` wait for the next midnight or 12:00 while a screen is open?

A) An in-app coroutine timer while something is listening, plus the system time-change broadcasts and the "app came to the foreground" signal to catch anything missed while the phone slept. No system alarms for screens (the widget has its own scheduler in the Widget unit)

B) A system alarm for each boundary, even when nothing is listening

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/data/nfr-design/nfr-design-patterns.md`: change signals, prompt loading, corruption handling, clock timer, threading and dispatchers, singletons, error mapping, logging, intents and receivers, migrations and schema export, testing patterns (Robolectric, shared fixtures)
- [x] 2. Write `aidlc-docs/construction/data/nfr-design/logical-components.md`: in-process components and how they connect (database, DataStore, stores, library, scheduler, notifier, clock, factory, provider)
- [x] 3. Security and PBT compliance review
- [x] 4. Check consistency with the data NFR requirements, functional design and domain contracts
