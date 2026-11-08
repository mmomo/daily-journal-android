# NFR Design Plan: Domain Unit

**Inputs**: `aidlc-docs/construction/domain/nfr-requirements/` (approved), domain functional design, application design.

**Categories considered**:
- **Resilience patterns**: Questions 1–3 (error handling, retries, best-effort work)
- **Performance patterns**: Questions 4–5
- **Security patterns**: Question 6 (validation of values at construction)
- **Concurrency** (a resilience concern on a phone, where autosave, the Done button, the widget and reminders can call use cases at the same time): Question 3
- **Testing pattern for the performance targets**: Question 7
- **Scalability patterns**: N/A. One user, about 3,650 entries in ten years; no scaling mechanism needed
- **Logical infrastructure components** (queues, caches, circuit breakers, load balancers): N/A as infrastructure. The domain has no servers or network; the only "components" it needs are in-process (a lock per Question 3 and possibly a cache per Question 4)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should use cases report errors to their callers?

A) Exceptions, like iOS: use cases throw the typed `JournalError`s; a small helper turns any unexpected port exception into `StorageFailure`, so callers only ever see the documented errors

B) Result values: every use case returns a success-or-error value (for example a sealed `Outcome`) instead of throwing; callers must handle both cases explicitly

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
Should the domain retry failed storage operations?

A) No automatic retries in the domain. A failed save reports `StorageFailure`; the user's text stays in the editor, and the next autosave or Done tap tries again naturally

B) Retry a failed save once automatically before reporting `StorageFailure`

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
Two saves for the same day can arrive at once (an autosave and the Done button), and refresh can be tapped twice quickly. Where should these be kept in order?

A) Inside the domain: `SaveTodayEntryUseCase`, `BackfillYesterdayUseCase` and `RefreshPromptUseCase` each hold a lock (a coroutine `Mutex`) so calls run one at a time, whoever calls them (editor, widget, reminders)

B) In the App unit only, like iOS: the editor ViewModel queues its saves and the refresh button is disabled while a refresh runs; the domain assumes callers don't overlap

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
Should `PromptSelector` cache its work? For each category combination it builds a sorted pool and a stride; with 600 prompts this takes well under a millisecond.

A) No cache: compute on every call (simplest; already far inside the 10 ms target)

B) Cache the pool and stride per category combination for the lifetime of the selector (at most 15 entries)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
Which thread should domain code run on?

A) The domain doesn't choose threads: its own work is small and fast, and the data layer's port functions are "main-safe" (they move storage work off the main thread themselves, the Android convention)

B) Every use case switches to a background dispatcher itself

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
What should happen if code tries to create an impossible `CalendarDay` (for example month 13 or 31 February)?

A) Same as iOS: the value is normalized (month 13 of 2026 becomes January 2027), which keeps parity with the iOS tests; untrusted text still goes through `CalendarDay.fromKey`, which rejects invalid dates

B) Reject it: creating an impossible day throws immediately; only `fromKey` and `containing` produce days from outside data

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7
How should the performance smoke tests (DOM-PERF-1 to 3) run?

A) As normal tests in the regular test task, so they run locally and in CI every time

B) In a separate Gradle task that CI runs on every push but that isn't part of the normal local test run (keeps local runs fast and avoids timing noise on a busy laptop)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/domain/nfr-design/nfr-design-patterns.md`: error handling, retries, concurrency, caching, threading, validation, performance measurement, supply-chain and CI patterns (from the tech stack decisions), each traced to its NFR requirement
- [x] 2. Write `aidlc-docs/construction/domain/nfr-design/logical-components.md`: in-process components (locks, caches, error helper, generators and test support), build and CI components, with responsibilities and how they connect
- [x] 3. Security and PBT compliance review
- [x] 4. Check consistency with the NFR requirements, functional design and application design
