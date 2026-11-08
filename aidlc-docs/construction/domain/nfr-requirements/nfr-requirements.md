# NFR Requirements: Unit 1 Domain (and project setup)

**Sources**: `domain-nfr-requirements-plan.md` (Q1–Q9) and `domain-nfr-requirements-clarification-questions.md` (C1, C2); requirements NFR-3 to NFR-6; domain functional design.

This unit covers the pure-Kotlin domain **and** the project-wide build, test, CI and supply-chain setup.

---

## 1. Performance (Q7:A)

| ID | Requirement | Check |
|---|---|---|
| DOM-PERF-1 | `StreakCalculator.summary` with 3,650 entry days completes in **under 50 ms** | Performance smoke test in CI |
| DOM-PERF-2 | `PromptSelector.dailyPrompt` with all four categories (600 prompts) and 30 days of answered prompts completes in **under 10 ms** | Performance smoke test in CI |
| DOM-PERF-3 | `EntrySearchMatcher` over 3,650 entries completes in under 100 ms (supports NFR-5.2; the Data unit owns the end-to-end search target) | Performance smoke test in CI |

Timing is the median of several runs after a warm-up, so one slow CI moment doesn't fail the build. These are smoke tests for regressions, not device benchmarks; device speed (NFR-5.1) is checked in later units.

## 2. Reliability

| ID | Requirement | Source |
|---|---|---|
| DOM-REL-1 | Every use case fails only with the typed errors in `domain-entities.md` §6; port failures become `StorageFailure` | NFR-6.8 |
| DOM-REL-2 | No partial writes: a use case validates everything before writing, and writes one entry per call | NFR-6.8 |
| DOM-REL-3 | Clock and time-zone changes never cause an unexpected error, a moved entry or an inconsistent streak (BR-SEC1–SEC3) | NFR-6.6 |
| DOM-REL-4 | All rules are deterministic for the same inputs (no hidden state, no randomness, no system clock) | BR-T1, BR-P2 |
| DOM-REL-5 | `SyncReminderWithTodayUseCase` is best effort: its failures are caught and never block a save | BR-R |

## 3. Security (Security Baseline, enabled)

| ID | Requirement | Rule |
|---|---|---|
| DOM-SEC-1 | The domain doesn't log. Error values never contain journal text; `StorageFailure.reason` is technical only | SECURITY-03, SECURITY-15 |
| DOM-SEC-2 | `DeepLink.parse` and `CalendarDay.fromKey` accept only well-formed, known input; everything else is `null` | SECURITY-05 |
| DOM-SEC-3 | Entry text is bounded at 20,000 characters | SECURITY-05 |
| DOM-SEC-4 | Every dependency (build and test included) is pinned to an exact version in `gradle/libs.versions.toml`; no dynamic versions (`+`, ranges, `latest`) | SECURITY-10 |
| DOM-SEC-5 | Gradle dependency verification: SHA-256 checksums of every downloaded file recorded in `gradle/verification-metadata.xml` and checked on every build (Q5:A) | SECURITY-10, SECURITY-13 |
| DOM-SEC-6 | The Gradle wrapper pins the Gradle distribution and its SHA-256 checksum | SECURITY-10, SECURITY-13 |
| DOM-SEC-7 | Dependencies come only from Google Maven and Maven Central (repositories declared once, centrally) | SECURITY-10 |
| DOM-SEC-8 | CI uploads the resolved dependency list to GitHub (dependency submission), so Dependabot raises alerts for known vulnerabilities and pull requests get dependency review (Q3:A). Dependabot alerts are switched on once in the repository settings | SECURITY-10 |
| DOM-SEC-9 | An SBOM is exported on demand from GitHub's dependency graph (C1:B; NFR-6.5 amended) | SECURITY-10 |
| DOM-SEC-10 | CI uses pinned tool versions: GitHub Actions referenced by full commit SHA, a fixed runner image (not `ubuntu-latest`), and a fixed JDK version | SECURITY-10 |
| DOM-SEC-11 | The CI workflow runs with read-only repository permissions, plus write only for dependency submission | SECURITY-06 (applied to CI), SECURITY-13 |
| DOM-SEC-12 | No unused dependencies: the domain depends only on kotlinx-coroutines-core and kotlinx-datetime | SECURITY-10 |

## 4. Maintainability

| ID | Requirement | Source |
|---|---|---|
| DOM-MAIN-1 | Kotest for all tests: example-based and property-based tests in clearly separate files (`…Test.kt` and `property/…PropertyTest.kt`) | Q2:A, PBT-10 |
| DOM-MAIN-2 | Every scenario in the domain functional design's test mapping has an example-based test; every property in §10 of the business logic model has a property-based test | NFR-4.1, NFR-4.3, PBT-01 to PBT-07 |
| DOM-MAIN-3 | Property-based tests use a random seed per run; on failure the seed and the shrunk input are printed; CI keeps test reports as artifacts | Q7 of functional design, PBT-08 |
| DOM-MAIN-4 | Kotlin compiler warnings are treated as errors in every module | Q6:A |
| DOM-MAIN-5 | Kover produces a coverage report in every CI run (saved as an artifact), with no minimum enforced | Q8:B |
| DOM-MAIN-6 | iOS parity tests use hand-copied iOS values (hash, daily picks, export layout) | Domain functional design Q8:B |
| DOM-MAIN-7 | CI runs on every push and every pull request (Q9:B): build, all tests, coverage report, dependency submission | NFR-4.4, R3:A |

## 5. Not Applicable

| Category | Reason |
|---|---|
| Scalability | One user on one phone; about 3,650 entries in ten years, covered by DOM-PERF |
| Availability | No server; the domain runs on the phone. Data loss risk is accepted in requirements NFR-2.3 |
| Usability | The domain has no UI (App unit) |
| Monitoring and alerting | No server or log infrastructure (SECURITY-14 N/A) |
