# NFR Requirements: Unit 2 Data

**Sources**: `data-nfr-requirements-plan.md` (Q1 A, Q2 A, Q3 B, Q4 A, Q5 A, Q6 B, Q7 A, Q8 B); data functional design; project-wide policies from the Domain unit's tech stack decisions.

---

## 1. Performance (Q6:B)

| ID | Requirement | Check |
|---|---|---|
| DATA-PERF-1 | No data-layer speed targets are enforced by tests. Search over about 3,650 entries (NFR-5.2) and opening the editor quickly (NFR-5.1) are measured on a device during Build and Test | Build and Test manual check |
| DATA-PERF-2 | All storage work runs off the main thread (main-safe ports), so slow storage can never freeze the screen | Code review; repository tests run on a test dispatcher |

The domain's own search smoke test (DOM-PERF-3, 3,650 entries under 100 ms) still runs in CI.

## 2. Reliability

| ID | Requirement | Source |
|---|---|---|
| DATA-REL-1 | Corrupt or missing settings fall back to defaults; corrupt prompt state to "no state"; nothing throws to the user | DATA-S2, S3 |
| DATA-REL-2 | Unreadable entry rows are skipped and never deleted | DATA-E5 |
| DATA-REL-3 | A save that would put a second entry on a day fails; it never overwrites | DATA-E1 |
| DATA-REL-4 | Database schema changes use explicit migrations; destructive fallback is never enabled; Room's exported schema files are committed so migrations can be tested | NFR-7.2, DATA-E9 |
| DATA-REL-5 | Prompt state updates are atomic (DataStore `edit`) | DATA-S5 |
| DATA-REL-6 | Reminders are inexact and may arrive a few minutes late (Q4:A); only the next reminder is registered and it is re-registered whenever reminders are recomputed or one fires (Q5:A); after a restart, time change or app update the App unit recomputes them (FR-6.5) | DATA-R2, R3 |

## 3. Security (Security Baseline)

| ID | Requirement | Rule |
|---|---|---|
| DATA-SEC-1 | Database and DataStore files live in the app's private storage; no world-readable files; no external storage | SECURITY-01, NFR-2.5 |
| DATA-SEC-2 | Logging uses Android's logger directly (Q8:B). Log messages contain only ids, counts and error types, never entry text, prompt text or settings values; checked in code review for every change to this unit | SECURITY-03, NFR-6.2 |
| DATA-SEC-3 | Notification tap intents are explicit (target the app's main activity) and immutable | SECURITY-11, NFR-6.3 |
| DATA-SEC-4 | Reminder alarms use explicit, immutable intents to the app's own receiver; the receiver is not exported | SECURITY-11 |
| DATA-SEC-5 | No exact-alarm or other special permission is requested (Q4:A); the only new permission is the notification permission, requested by the App unit | SECURITY-06 (least privilege, applied to Android permissions) |
| DATA-SEC-6 | JSON from storage and the prompt file is parsed defensively: wrong types, missing fields and unknown fields never crash; the prompt file is validated | SECURITY-13, SECURITY-15, NFR-6.7 |
| DATA-SEC-7 | Database queries use Room's bound parameters only (no string-built SQL) | SECURITY-05 |
| DATA-SEC-8 | New libraries follow the project policy: official AndroidX/Google libraries only in shipped code, exact versions in the catalog, checksums recorded in `verification-metadata.xml` | SECURITY-10 |

## 4. Maintainability and Testing (Q7:A)

| ID | Requirement | Source |
|---|---|---|
| DATA-MAIN-1 | Data tests run on the JVM with Robolectric, inside the normal `./gradlew build`, locally and in CI; no emulator needed | Q7:A |
| DATA-MAIN-2 | Example-based tests for every row of the data story mapping (`business-rules.md`), including the iOS JSON samples and the 600-prompt file check | Data functional design |
| DATA-MAIN-3 | Property-based tests (Kotest) for every property in the data business logic model §8, using the domain's generators shared through a test-fixtures source set | PBT-01 to PBT-07 |
| DATA-MAIN-4 | Domain use cases run end to end against the real Room database (in-memory) and DataStore in tests | Unit of work exit criteria |
| DATA-MAIN-5 | Kover covers `:data` in the aggregated report (report only) | Domain Q8:B |

## 5. Not Applicable

| Category | Reason |
|---|---|
| Scalability | One user, about 3,650 entries in ten years |
| Availability | On-device only; data-loss risk accepted (NFR-2.3) |
| Usability | No UI in this unit |
