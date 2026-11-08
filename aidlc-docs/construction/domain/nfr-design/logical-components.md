# Logical Components: Unit 1 Domain (and project setup)

The domain has no infrastructure components (no queues, caches, circuit breakers or load balancers): it runs inside the app on one phone. Its NFR components are small in-process helpers, test support, and the build and CI setup.

---

## 1. In-process Components (`:domain`, main source)

| Component | Responsibility | Used by | NFR |
|---|---|---|---|
| `JournalWriteLock` | One coroutine `Mutex` shared by the writing use cases; runs saves, backfills and refreshes one at a time in arrival order | `SaveTodayEntryUseCase`, `BackfillYesterdayUseCase`, `RefreshPromptUseCase` | DOM-REL-2, Q3:A |
| `withStorage` helper | Wraps port calls; passes `JournalError` and `CancellationException` through, turns anything else into `StorageFailure` | Every use case that calls a port | DOM-REL-1, DOM-SEC-1 |
| `TextValidation` | `isValidText`, code-point limit, `validate(text)` | Save and backfill use cases | DOM-SEC-3 |
| Strict parsers | `CalendarDay.fromKey`, `DeepLink.parse` | Data unit (stored keys), App unit (routes, intents) | DOM-SEC-2 |
| Failure callback | Optional `(Throwable) -> Unit` given to `JournalUseCases` so best-effort failures (reminder sync) can be logged by the caller without journal text | `SyncReminderWithTodayUseCase` | DOM-REL-5, DOM-SEC-1 |

`JournalUseCases` constructor (refined): ports as before, plus `onBackgroundFailure: (Throwable) -> Unit = {}`. It creates one `JournalWriteLock` and passes it to the three writing use cases.

## 2. Test Support (`:domain`, test source)

| Component | Responsibility | NFR |
|---|---|---|
| `FixedTimeSource` | Settable `now` and time zone | NFR-4.2 |
| In-memory fakes | `FakeEntryRepository`, `FakePromptStateStore`, `FakeSettingsStore`, `FakeReminderScheduler`, `FakeWidgetRefresher`, `FakePromptLibrary`; a failing repository variant for `StorageFailure` tests | DOM-MAIN-2 |
| Generators (`Arb`s) | Days 2024–2032, the 8 time zones, instants biased to 00:00 / 12:00 / 23:59:59, category sets, entry texts (empty, whitespace, accented, emoji, long, over limit), entry-day sets, queries | PBT-07 |
| Reference implementations | Simple longest-run counter and simple search matcher, used as oracles | PBT-05 |
| Stateful models | A counter and list for refreshes; an optional string for saves | PBT-06 |
| iOS parity fixtures | Hand-copied iOS values: hash results, daily picks, export output | DOM-MAIN-6 |
| Timing helper | Warm-up plus median of several runs for the performance smoke tests | DOM-PERF, Q7:A |

## 3. Build Components (project root)

| Component | Responsibility | NFR |
|---|---|---|
| `settings.gradle.kts` | Includes `:domain`, `:data`, `:app`, `:widget`; declares the only repositories (Google Maven, Maven Central) and forbids module-level repositories | DOM-SEC-7 |
| `gradle/libs.versions.toml` | Every version and plugin, pinned | DOM-SEC-4 |
| `gradle/verification-metadata.xml` | SHA-256 checksums of every downloaded artifact | DOM-SEC-5 |
| Gradle wrapper | Pinned Gradle version and distribution checksum | DOM-SEC-6 |
| Root `build.gradle.kts` | Plugin versions (applied in modules), Kover aggregation, warnings-as-errors for all Kotlin compilations, Java 17 toolchain | DOM-MAIN-4, DOM-MAIN-5 |
| Module build files | `:domain` as a Kotlin JVM library with Kotest on the JUnit Platform; `:data`, `:app`, `:widget` as minimal Android skeletons (`minSdk` 29) to be filled by their units | Unit 1 setup |

## 4. CI Components (`.github/workflows/ci.yml`)

| Job | Steps | Permissions |
|---|---|---|
| `build-and-test` | Checkout → validate Gradle wrapper → set up Temurin 17 → set up Gradle (cache) → `./gradlew build` (compile with warnings as errors, all tests including PBT and performance smoke tests) → Kover report → upload test and coverage reports | `contents: read` |
| `dependency-submission` | Checkout → set up Temurin 17 → Gradle dependency submission (feeds GitHub's dependency graph, Dependabot alerts and the SBOM export) | `contents: write` |

Triggers: every push and every pull request. Every action is pinned by commit SHA; the runner image is a fixed Ubuntu version.

## 5. How They Connect

```
ViewModel / widget / receiver
   -> JournalUseCases (one per process)
        -> writing use cases -> JournalWriteLock -> withStorage { port calls }
        -> read use cases    -> withStorage { port calls }
        -> SyncReminderWithToday -> best effort -> onBackgroundFailure(type only)

Developer push / pull request
   -> CI build-and-test: wrapper check -> verified downloads -> build -> tests -> reports
   -> CI dependency-submission -> GitHub dependency graph -> Dependabot alerts, SBOM export
```
