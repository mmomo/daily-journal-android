# NFR Design Patterns: Unit 1 Domain (and project setup)

**Inputs**: `domain-nfr-design-plan.md` (Q1–Q7, all A), `nfr-requirements.md`, `tech-stack-decisions.md`, domain functional design.

---

## 1. Error Handling: typed exceptions (Q1:A)

| Aspect | Design | NFR |
|---|---|---|
| Contract | Use cases throw only the `JournalError` subtypes listed in `domain-entities.md` §6 | DOM-REL-1 |
| Unexpected failures | A helper `withStorage { … }` wraps every port call: a `JournalError` passes through; any other exception becomes `JournalError.StorageFailure(reason = exception class name)`. The original exception is attached as the `cause` for debugging but never shown to users | DOM-REL-1, DOM-SEC-1 |
| Cancellation | `CancellationException` is always rethrown unchanged (never wrapped), so coroutine cancellation keeps working | Kotlin coroutines contract |
| No journal text in errors | Error values carry ids and class names only; `toString()` of `StorageFailure` shows the reason, never entry text or prompts | DOM-SEC-1, NFR-6.2 |
| Validate before writing | Each use case checks text, editability and limits first, then makes exactly one write | DOM-REL-2 |

Same pattern as iOS (`withStorage` in `Support.swift`), so behavior and tests line up.

## 2. Retries: none in the domain (Q2:A)

- A failed save throws `StorageFailure` immediately. The editor keeps the text (App unit), and the next autosave or Done tap is the retry.
- `SyncReminderWithTodayUseCase` is best effort (DOM-REL-5): it catches every failure except cancellation and returns quietly; its next trigger (save, app start, restart) tries again. It reports failures through an optional callback so the Data or App unit can log the error type (the domain itself doesn't log, DOM-SEC-1).

## 3. Concurrency: one write lock in the domain (Q3:A)

| Aspect | Design |
|---|---|
| Lock | One `JournalWriteLock` (a coroutine `Mutex`) per `JournalUseCases` instance, created in its constructor |
| Who takes it | `SaveTodayEntryUseCase`, `BackfillYesterdayUseCase`, `RefreshPromptUseCase`: the use cases that read and then write entries or today's prompt state |
| Why one shared lock | A save and a refresh touch the same data (a save freezes today's prompt; a refresh changes it). With one lock they can't interleave, so an entry is never saved with a prompt that was replaced mid-save. Saves are rare and fast, so sharing a lock costs nothing noticeable |
| Scope | One process has one `JournalUseCases` (the App container, shared with the widget and receivers through `JournalEnvironmentProvider`), so the lock covers every caller |
| Read-only use cases | `GetTodayState`, `GetEntry`, `ListEntries`, `SearchEntries`, `GetStreak`, `ExportJournal`, `GetSettings` don't take the lock. `GetTodayState` may store a first daily pick, but that pick is deterministic, so two concurrent first picks write the same value |
| Settings writes | `UpdatePromptCategories`, `ConfigureReminder`, `CompleteOnboarding` are user actions on a single screen and don't race; no lock |
| Order guarantee | Calls holding the lock run one at a time in arrival order (a fair `Mutex`), so the last save to arrive is the one stored |

Difference from iOS: iOS ordered saves inside the editor view. Android does it in the domain, so the widget, receivers and every screen are covered.

## 4. Performance

| Pattern | Design | NFR |
|---|---|---|
| No caching in `PromptSelector` (Q4:A) | Pool and stride computed on every call; about 600 prompts, well under 1 ms | DOM-PERF-2 |
| Streak computed fresh | From the set of entry days each time (BR-S8); one pass over sorted day numbers | DOM-PERF-1 |
| Primitive day math | `CalendarDay` arithmetic on `dayNumber` integers; time-zone lookups only where an instant is involved | DOM-PERF-1, 2 |
| Search normalization | Query normalized once per search; each entry's texts normalized during matching. The Data unit may move search into the database but must give identical results | DOM-PERF-3 |

## 5. Threading (Q5:A)

- The domain doesn't switch threads or hold its own dispatcher. Its own work is small and CPU-only.
- Port implementations in the Data unit are **main-safe**: they move storage work to a background dispatcher themselves. This is recorded as a contract on every port interface (KDoc) and verified in the Data unit.
- Tests use `kotlinx-coroutines-test` with a test dispatcher, so suspend calls run deterministically.

## 6. Validation and Safe Construction (Q6:A, security)

| Value | Pattern | NFR |
|---|---|---|
| `CalendarDay(year, month, day)` | Normalizes out-of-range parts like iOS (month 13 of 2026 → January 2027; 31 February → 3 March), keeping parity with the iOS tests | BR-T, iOS parity |
| `CalendarDay.fromKey(text)` | Strict: only `YYYY-MM-DD` with a real date; anything else → `null`. Used for every value from outside the domain (route arguments, stored keys) | DOM-SEC-2, BR-SEC4 |
| `DeepLink.parse(uri)` | Allowlist of scheme `dailyjournal` and hosts `today` and `backfill`; everything else → `null` | DOM-SEC-2 |
| Entry text | `validate(text)` before any write: blank → `EmptyText`, over 20,000 code points → `TextTooLong` | DOM-SEC-3 |
| Settings | `UpdatePromptCategories` rejects an empty set; reminder hour and minute are range-checked when settings are built | BR-G2 |
| Edit timestamps | `editedAt` later than `now` is treated as `now` | BR-SEC3 |

## 7. Measuring Performance (Q7:A)

- The three performance smoke tests live in the normal test task, so they run locally and in CI every time.
- Each test warms up first, then takes the **median** of several timed runs and compares it with the target (50 ms, 10 ms, 100 ms). The targets are generous compared with the expected times (a few milliseconds), so a busy laptop doesn't cause false failures.
- Test data: 3,650 consecutive and gapped entry days, the full 600-prompt library shape, 30 days of answered prompts.

## 8. Supply Chain and CI (from tech stack decisions)

| Pattern | Design | NFR |
|---|---|---|
| Pinned versions | Every version in `gradle/libs.versions.toml`; modules refer to catalog aliases only | DOM-SEC-4 |
| Verified downloads | `gradle/verification-metadata.xml` with SHA-256 for every artifact; updated deliberately with `--write-verification-metadata sha256` when a dependency changes | DOM-SEC-5 |
| Verified wrapper | `distributionSha256Sum` in `gradle-wrapper.properties`; CI validates the wrapper JAR | DOM-SEC-6 |
| Central repositories | Declared once in `settings.gradle.kts` (`google()`, `mavenCentral()`), with modules forbidden from adding their own | DOM-SEC-7 |
| Least-privilege CI | Workflow default `permissions: contents: read`; only the dependency submission job gets `contents: write` | DOM-SEC-11 |
| Pinned CI tools | Actions by full commit SHA with the version as a comment; fixed runner image; Temurin 17 | DOM-SEC-10 |
| Reports kept | Test reports (including PBT seeds of failures) and the Kover report uploaded as artifacts | DOM-MAIN-3, DOM-MAIN-5 |

## 9. Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-03 (no domain logging; no journal text in errors), 05 (strict parsing, length limit), 10 (pinned, verified, scanned, SBOM export, pinned CI), 11 (misuse handled, concerns isolated: validation in value types, ordering in one lock), 13 (checksums, wrapper validation), 15 (typed errors, cancellation preserved, no partial writes): compliant. Server-only rules N/A. No blocking findings |
| Property-Based Testing | PBT-08 (seed printed, reports kept in CI, shrinking on): compliant. The write lock is covered by the stateful properties already planned (one entry per day after any sequence of saves; at most 3 refreshes) |
