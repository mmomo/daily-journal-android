# Requirements: Daily Journal for Android

## Intent Analysis Summary

| Item | Value |
|---|---|
| **User Request** | "Lets create a new repo for the android version" (then: "Start building the Android app") |
| **Request Type** | New Project (Greenfield): Android version of the existing Daily Journal iOS app |
| **Scope Estimate** | Multiple Components: Android app, Home Screen widget, shared business logic and data modules |
| **Complexity Estimate** | Moderate (business rules are already defined by the iOS app; Android platform work is new) |
| **Requirements Depth** | Standard |
| **Distribution** | Personal use: built and installed on the user's own phone (Q13:A) |
| **Baseline** | The approved iOS requirements (`demo/aidlc-docs/inception/requirements/requirements.md`) with the Android decisions below |

**Sources**: `requirement-verification-questions.md` (Q1–Q15 and the three extension questions) and `requirement-clarification-questions.md` (C1–C4, R1–R5). Each requirement lists the answers it comes from. "iOS FR-x" means the same rule as the iOS requirement with that ID.

---

## 1. Functional Requirements

The Android app has **full feature parity** with the iOS app and keeps its rules exactly (Q1:A, Q15:A). The only feature difference is that there is no cloud sync or backup (Q7:B, C2:A, C3:A).

### FR-1 Daily Prompt

| ID | Requirement | Source |
|---|---|---|
| FR-1.1 | The app shows one journaling prompt per calendar day from a prompt library bundled with the app. No AI or network generation. | iOS FR-1.1 |
| FR-1.2 | The library is the same 600 prompts as the iOS app (150 each in Gratitude, Reflection, Goals, Creativity), with the same ids and texts, so both apps offer identical prompts. The repository is public, so the prompts become public. | Q2:A |
| FR-1.3 | The user chooses prompt categories in Settings and during onboarding. At least one category must stay selected. | iOS FR-1.3 |
| FR-1.4 | The daily prompt is picked deterministically from the selected categories, so the same day gives the same prompt on every launch and in the widget, and no prompt repeats within 30 days. The selection algorithm matches iOS, so the same date and categories give the same prompt on both platforms. | iOS FR-1.4, Q15:A |
| FR-1.5 | The user can refresh today's prompt up to **3** times per day. The refreshed prompt becomes today's prompt everywhere, including the widget. | iOS FR-1.5 |
| FR-1.6 | Once today's entry is saved, the prompt it answered is stored with the entry and no longer changes. Today's prompt also stays the same if categories are changed during the day; new categories apply to refreshes today and to prompts from tomorrow. | iOS FR-1.6, iOS US-1.4 |

### FR-2 Quick Entry

| ID | Requirement | Source |
|---|---|---|
| FR-2.1 | The user writes a text-only entry in response to today's prompt. | iOS FR-2.1 |
| FR-2.2 | Only one entry is allowed per calendar day. | iOS FR-2.2 |
| FR-2.3 | Today's entry can be edited until local midnight, and is autosaved while typing. | iOS FR-2.3 |
| FR-2.4 | **Grace backfill**: if yesterday has no entry, the user can write one for yesterday until 12:00 local time today. | iOS FR-2.4 |
| FR-2.5 | An existing entry for yesterday is locked at midnight; the grace window does not allow editing it. | iOS FR-2.5 |
| FR-2.6 | Entries from earlier days are read-only. | iOS FR-2.6 |
| FR-2.7 | Empty or whitespace-only entries cannot be saved. | iOS FR-2.7 |
| FR-2.8 | Opening the app (or tapping the widget or reminder) shows the editor with today's prompt and the keyboard up, unless today's entry is already done. While typing, the editor and its Done button stay visible above the keyboard. | iOS FR-2.8, iOS fixes from 2026-10-28 |
| FR-2.9 | Entries cannot be deleted (same as iOS). | Q15:A |

### FR-3 Past Entries

| ID | Requirement | Source |
|---|---|---|
| FR-3.1 | Browse past entries newest first, each showing date, prompt and a text preview. | iOS FR-3.1 |
| FR-3.2 | Search entries by text, matching entry text and prompt text, ignoring case and accents. | iOS FR-3.2 |
| FR-3.3 | Export all entries through the Android share sheet as Markdown (`.md`) and plain text (`.txt`). Export is the **only** way to keep a copy of the journal outside the phone (see NFR-2). | iOS FR-3.3, C2:A |

### FR-4 Streak

| ID | Requirement | Source |
|---|---|---|
| FR-4.1 | A day counts toward the streak if it has a saved entry, using the device's current time zone. | iOS FR-4.1 |
| FR-4.2 | The current streak is the run of consecutive days with entries ending today, or ending yesterday if today has no entry yet. | iOS FR-4.2 |
| FR-4.3 | Missing a whole day resets the streak to 0 once the 12:00 grace window passes without a backfill. | iOS FR-4.3 |
| FR-4.4 | Before 12:00, a streak that ended the day before yesterday is shown as **at risk**, not reset; a backfill restores it. | iOS FR-4.4 |
| FR-4.5 | The best (longest) streak is shown. | iOS FR-4.5 |
| FR-4.6 | The streak is always calculated from stored entries, never kept as a counter. | iOS FR-4.6 |

### FR-5 Widget

| ID | Requirement | Source |
|---|---|---|
| FR-5.1 | One small Home Screen widget. | Q6:A |
| FR-5.2 | It shows today's prompt (truncated if needed) and a streak badge, with a checkmark when today is done and an at-risk state before 12:00. | iOS FR-5.2, FR-5.6 |
| FR-5.3 | It shows the same prompt as the app, including after a refresh. | iOS FR-5.3 |
| FR-5.4 | Tapping it opens the app on the Today screen with the editor ready (or the saved entry if today is done), returning to Today even if another screen was open. | iOS FR-5.4; iOS compliance gap 2 |
| FR-5.5 | It updates at local midnight, at the 12:00 grace cutoff, and right after an entry is saved, the prompt is refreshed or settings change. | iOS FR-5.5 |
| FR-5.6 | Before onboarding is finished, it shows an invitation to open the app. | iOS US-5.1 |

### FR-6 Reminder Notifications

| ID | Requirement | Source |
|---|---|---|
| FR-6.1 | Optional daily reminder, off by default, at a time the user sets (default 8:00 PM). | Q8:A |
| FR-6.2 | No reminder fires on a day whose entry is already saved. | Q8:A |
| FR-6.3 | On Android 13 and later, notification permission is requested only when the user turns the reminder on. If permission is denied, Settings shows the reminder as unavailable with a button to open the system notification settings, **including when Settings is opened later**. | Q8:A; iOS compliance gap 1 |
| FR-6.4 | Tapping the reminder opens the editor, the same way as the widget (FR-5.4). | iOS FR-6.4 |
| FR-6.5 | Reminders survive a phone restart and changes of time zone or clock. | Android platform need |

### FR-7 Onboarding and Settings

| ID | Requirement | Source |
|---|---|---|
| FR-7.1 | A one-time, two-step onboarding: choose categories (all selected by default), then an optional reminder. It ends on the Today screen. | iOS US-7.1 |
| FR-7.2 | Settings contains prompt categories, the reminder switch and time, and export. Changes persist across restarts. | iOS FR-7.1 |

### FR-8 Edge-case behavior carried over from the iOS review

| ID | Requirement | Source |
|---|---|---|
| FR-8.1 | If the clock passes midnight while today's entry is being edited, text typed before midnight is kept, the editor becomes read-only and a message explains the entry is locked (iOS US-2.2). | iOS compliance gap 3 |

---

## 2. Non-Functional Requirements

### NFR-1 Platform and Technology

| ID | Requirement | Source |
|---|---|---|
| NFR-1.1 | Minimum Android 10 (API 29). Phones only, portrait. | Q3:B, Q9:A |
| NFR-1.2 | Kotlin, Jetpack Compose with Material 3, Gradle with the Kotlin DSL and a version catalog. Widget built with a Compose-based widget library (Jetpack Glance). Storage and reminder technologies are chosen during design. | Q4:A, Q6:A |
| NFR-1.3 | Clean Architecture in separate Gradle modules mirroring iOS: **domain** (pure Kotlin business rules and use cases, no Android dependencies), **data** (storage, prompt library, notifications), **app** (screens) and **widget**. | Q11:A |
| NFR-1.4 | English only. | Q10:A |
| NFR-1.5 | Visual design matches the iOS app: warm orange accent, rounded cards on a grouped background, serif prompt text, large full-width primary button, light and dark mode. | Q5:A |

### NFR-2 Data and Privacy

| ID | Requirement | Source |
|---|---|---|
| NFR-2.1 | Entries, settings and prompt state are stored only on the device. No accounts, no server, no sync between devices. | Q7:B, C3:A |
| NFR-2.2 | No automatic backup: the app opts out of Android's automatic backup and device-to-device transfer, so journal data never goes to Google's servers. The manual export (FR-3.3) is the only recovery path. | C2:A |
| NFR-2.3 | **Accepted risk**: if the phone is lost, broken or reset, entries written since the user's last manual export are lost. Recovery target: hours to days, from the last export (R1:A). | C2:A, R1:A |
| NFR-2.4 | No analytics, crash reporting or third-party SDKs that send data off the device; the app requests no internet permission. | iOS NFR-2.5, C1:B |
| NFR-2.5 | Data is encrypted at rest by Android's built-in file-based encryption (hardware-backed keys tied to the screen lock; required on devices launching with Android 10+). No additional database encryption. | C4:A |

### NFR-3 Date and Time Correctness

| ID | Requirement | Source |
|---|---|---|
| NFR-3.1 | All day boundaries use the device's current time zone and calendar. | iOS NFR-3.1 |
| NFR-3.2 | Each entry stores its calendar day (year, month, day) separately from its creation time, so changing time zones doesn't move it. | iOS NFR-3.2 |
| NFR-3.3 | Streak and prompt logic are correct across daylight-saving changes, midnight, the 12:00 cutoff and time zone changes. | iOS NFR-3.3 |

### NFR-4 Quality and Testing

| ID | Requirement | Source |
|---|---|---|
| NFR-4.1 | Unit tests cover the business rules: streak (including grace and at-risk), daily prompt selection, refresh limit, and editing and backfill rules. The iOS acceptance scenarios are reused as test cases. | Q12:A |
| NFR-4.2 | Business logic depends on an injectable clock and time zone so tests are deterministic. | iOS NFR-4.2 |
| NFR-4.3 | **Property-based testing is enforced** (all PBT rules, blocking) using Kotest property testing, alongside example-based tests. | PBT extension: A |
| NFR-4.4 | GitHub Actions builds the app and runs all tests, including property-based tests and dependency vulnerability scanning, on every push. | R3:A |
| NFR-4.5 | Accessibility and localization are not formally in scope; standard Compose components give basic support. | iOS NFR-4.4, Q10:A |

### NFR-5 Performance

| ID | Requirement | Source |
|---|---|---|
| NFR-5.1 | From launch or a widget tap, the editor is ready to type in about 1 second on a mid-range phone. | iOS NFR-5.1 |
| NFR-5.2 | Search stays responsive with about 3,650 entries (10 years). | iOS NFR-5.2 |

### NFR-6 Security (Security Baseline extension, enabled)

| ID | Requirement | Source |
|---|---|---|
| NFR-6.1 | Stored data is encrypted at rest (NFR-2.5). No data leaves the device, so encryption in transit does not apply. | SECURITY-01, C4:A |
| NFR-6.2 | Logging uses the platform logger only. Journal text, prompts and settings are never logged, and release builds strip debug and verbose logs. Central log collection is N/A (no server; NFR-2.4). | SECURITY-03, C1:B |
| NFR-6.3 | Deep links (widget, reminder) only accept the app's own known routes; anything else opens the Today screen and does nothing else. The widget and reminder use explicit, immutable intents. | SECURITY-05, SECURITY-11 |
| NFR-6.4 | Release builds are minified, contain no debug features or sample code, and user-facing errors are generic (no stack traces or internal details). | SECURITY-09, SECURITY-15 |
| NFR-6.5 | Every dependency is pinned to an exact version in the version catalog, Gradle dependency verification or locking is enabled, unused dependencies are removed, dependencies come only from Google Maven and Maven Central, CI scans dependencies for known vulnerabilities, and an SBOM can be exported on demand from GitHub's dependency graph (which CI keeps up to date). *(Amended during Domain NFR Requirements, clarification C1:B; originally "CI produces a Software Bill of Materials".)* | SECURITY-10, R3:A |
| NFR-6.6 | The design documents at least one misuse case: changing the phone's clock or time zone to gain refreshes or fake a streak. Clock changes are accepted as the user's own choice (personal journal), but must not corrupt data or crash. | SECURITY-11 |
| NFR-6.7 | The bundled prompt file is validated when loaded (ids, categories, lengths, duplicates), as on iOS. | SECURITY-13 |
| NFR-6.8 | All storage and file operations handle errors explicitly; a top-level handler prevents crashes from leaking details, and failures never save partial or corrupt entries. | SECURITY-15 |

### NFR-7 Process (user preferences; the Resiliency extension is off)

| ID | Requirement | Source |
|---|---|---|
| NFR-7.1 | No formal change management process: personal project, documented exemption. | R2:A |
| NFR-7.2 | Every release is tagged in Git. Rolling back means installing the previous tagged build, so database changes between versions must keep older data readable (no destructive migrations). | R4:A |
| NFR-7.3 | Problems are tracked as GitHub Issues; each fix adds a regression test. | R5:B |

---

## 3. Out of Scope

- Sync between devices, cloud or automatic backup, accounts, servers (Q7:B, C2:A, C3:A)
- Sync with the iOS app
- Tablets, landscape, foldables (Q9:A)
- Languages other than English (Q10:A)
- Google Play distribution (Q13:A)
- Entry deletion, more than one entry per day, editing past entries (Q15:A)
- Database-level encryption such as SQLCipher (C4:A)
- AI-generated prompts, mood tracking, photos or attachments, app lock

---

## 4. Extension Configuration

| Extension | Enabled | Scope |
|---|---|---|
| Security Baseline | Yes | Rules that apply to an on-device app are enforced (NFR-6). Server-only rules are N/A (C1:B). |
| Resiliency Baseline | No | Turned off (C1:B): its rules target cloud availability. Process preferences from R1–R5 are kept as NFR-2.3 and NFR-7. |
| Property-Based Testing | Yes | Full enforcement (all rules blocking). |

### Security rule applicability

| Rule | Status | Reason |
|---|---|---|
| SECURITY-01 Encryption at rest and in transit | Applies | NFR-2.5, NFR-6.1. Data never leaves the device, so in-transit encryption does not apply |
| SECURITY-02 Access logging on network intermediaries | N/A | No load balancers, gateways or CDNs |
| SECURITY-03 Application-level logging | Applies, partly | NFR-6.2. Central log service N/A: no server, and journal data must not leave the device |
| SECURITY-04 HTTP security headers | N/A | No web content served |
| SECURITY-05 Input validation on API parameters | Applies, partly | No APIs; deep links validated (NFR-6.3) |
| SECURITY-06 Least-privilege access policies | N/A | No cloud IAM. Android permissions kept minimal: notifications only, no internet |
| SECURITY-07 Restrictive network configuration | N/A | No network |
| SECURITY-08 Application-level access control | N/A | Single user, no accounts or endpoints |
| SECURITY-09 Hardening | Applies | NFR-6.4 |
| SECURITY-10 Supply chain | Applies | NFR-6.5 |
| SECURITY-11 Secure design | Applies | NFR-6.3, NFR-6.6 |
| SECURITY-12 Authentication and credentials | N/A | No authentication (no app lock, no accounts) |
| SECURITY-13 Data integrity | Applies, partly | NFR-6.7. Change auditing N/A for a single-user personal journal |
| SECURITY-14 Alerting and monitoring | N/A | No server or log infrastructure; app sends no data |
| SECURITY-15 Exception handling | Applies | NFR-6.8 |

---

## 5. Key Requirements Summary

- Same features and rules as the iOS app: one prompt a day from the same 600 prompts, up to 3 refreshes, one entry per day editable until midnight with autosave, a 12:00 grace window to backfill yesterday, streak with at-risk state and best streak, history with search, Markdown and text export, reminders, onboarding and settings
- Small Home Screen widget with prompt and streak that opens the editor
- The three gaps found in the iOS review are requirements here from the start (FR-5.4, FR-6.3, FR-8.1)
- Everything stays on the phone: no sync, no automatic backup, no analytics; manual export is the only copy, an accepted risk
- Android 10+, phones in portrait, Kotlin, Jetpack Compose and Material 3, Clean Architecture modules, English only, iOS look and feel
- Unit tests plus property-based tests (Kotest), GitHub Actions CI with dependency scanning, SBOM exportable from GitHub, personal installation
