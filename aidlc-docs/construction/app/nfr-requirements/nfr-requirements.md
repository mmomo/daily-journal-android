# NFR Requirements: Unit 3 App

**Sources**: `app-nfr-requirements-plan.md` (Q1 A, Q2 A, Q3 B, Q5 A, Q6 A) and `app-nfr-requirements-clarification-questions.md` (C1 B); requirements NFR-1.5, NFR-2, NFR-5.1, NFR-6, NFR-7.2; stories.

## 1. Performance

| ID | Requirement | Check |
|---|---|---|
| APP-PERF-1 | Startup stays light (Q1:A): the first frame is a simple loading screen; the environment (database, preferences, prompt file) is created off the main thread; Today appears as soon as it's ready | Code review |
| APP-PERF-2 | From launch or a widget/reminder tap, the editor is ready to type in about 1 second on your phone (NFR-5.1), measured during Build and Test with a release build | Manual, Build and Test |
| APP-PERF-3 | No storage work on the main thread (ViewModels call main-safe use cases from `viewModelScope`) | Code review; Android StrictMode in debug builds |

## 2. Usability

| ID | Requirement | Source |
|---|---|---|
| APP-USE-1 | iOS look: orange accent (light and dark variants as iOS), rounded cards on a grouped background, the phone's built-in **serif** font for prompts (Q2:A), large full-width primary button, light and dark mode; no wallpaper colors | NFR-1.5 |
| APP-USE-2 | **Classic Back** handling (Q3:B): the app doesn't opt in to predictive Back; Back behaves as in the stories (save before leaving the editor, confirm discarding a backfill, clear search first, return to Today) | Stories, Q3:B |
| APP-USE-3 | Keyboard: the editor resizes with the keyboard so the typed line and the Done button stay visible (US-2.5); edge-to-edge layout with proper insets (required at target SDK 35+) | US-2.5, platform |
| APP-USE-4 | Portrait phones only; English text | NFR-1.1, NFR-1.4 |
| APP-USE-5 | Interactive elements carry stable test tags (`{screen}-{element}`), matching the iOS accessibility identifiers where they exist | Code generation rules |

## 3. Security (Security Baseline)

| ID | Requirement | Rule |
|---|---|---|
| APP-SEC-1 | The installed build is a **hardened release build** (C1:B): R8 shrinking and obfuscation, unused resources removed, `Log.v`/`Log.d`/`Log.i` stripped, not debuggable | SECURITY-09, NFR-6.2, NFR-6.4 |
| APP-SEC-2 | The release build is signed with the **debug signing key** Android Studio created on this Mac (C1:B); no keystore in the repository. Accepted consequence: if that key is lost, installing a new version needs an uninstall first, which deletes the journal on the phone (export first) | C1:B |
| APP-SEC-3 | Manifest: `allowBackup="false"` plus data-extraction rules excluding everything from cloud backup and device transfer; no internet permission; only `POST_NOTIFICATIONS` (from the Data unit) | NFR-2.2, NFR-2.4, SECURITY-06 |
| APP-SEC-4 | Only `MainActivity` is exported (launcher and the `dailyjournal://` link); the reminder and restart receivers are not exported (the restart receiver receives only protected system broadcasts) | SECURITY-11 |
| APP-SEC-5 | Incoming links are parsed with `DeepLink.parse`; unknown links just show Today; route arguments are validated with `CalendarDay.fromKey` | NFR-6.3 |
| APP-SEC-6 | A top-level uncaught-exception handler logs only the exception class, then defers to the system handler; user-facing errors are generic | NFR-6.8, SECURITY-15 |
| APP-SEC-7 | No journal text, prompt text or settings values in logs | NFR-6.2 |
| APP-SEC-8 | New libraries follow the project policy (official AndroidX/Google/Kotlin only, pinned, checksums) | SECURITY-10 |

## 4. Reliability

| ID | Requirement | Source |
|---|---|---|
| APP-REL-1 | Typed text survives rotation, theme changes and process death (ViewModel plus `SavedStateHandle`) | US-2.1 |
| APP-REL-2 | Saves are serialized by the domain lock; the editor keeps text on a failed save and shows a generic message | US-2.1 |
| APP-REL-3 | Midnight while editing: pending text is flushed with its edit time, the editor locks with a message, and Today doesn't swap in the new day until the user leaves (FR-8.1) | FR-8.1, BR-E4 |
| APP-REL-4 | Startup failure (for example an invalid prompt file) shows an error screen with "Try again" | DATA-P2 |
| APP-REL-5 | Reminders are recomputed on app start, after boot, time or time-zone change and app update (restart receiver), and when a reminder fires | FR-6.5 |

## 5. Maintainability and Testing

| ID | Requirement | Source |
|---|---|---|
| APP-MAIN-1 | ViewModel tests on the JVM with fakes (Q5:A): autosave and Done, Back saving, the midnight lock, refresh, backfill confirm, permission state on opening Settings, deep-link routing, History search and Back; no UI tests | Q5:A |
| APP-MAIN-2 | ViewModels are evaluated for stateful properties (PBT-06): the editor's sequence of edits, saves and clock boundaries is checked against a simple model | PBT-06 |
| APP-MAIN-3 | Kover includes `:app` in the report | Domain Q8:B |

## 6. Release Process (Q6:A)

| ID | Requirement |
|---|---|
| APP-REL-PROC-1 | `versionName` (for example `1.0.0`) set by hand in `app/build.gradle.kts`; `versionCode` increased by one for every release |
| APP-REL-PROC-2 | The commit you install is tagged `v<versionName>` (for example `v1.0.0`); rollback = check out the previous tag, build and install (NFR-7.2) |

## 7. Not Applicable

| Category | Reason |
|---|---|
| Scalability | One user on one phone |
| Availability | On-device app, no server |
