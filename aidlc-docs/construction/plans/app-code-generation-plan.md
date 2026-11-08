# Code Generation Plan: Unit 3 App

**This plan is the single source of truth for the App unit's Code Generation.** Steps run in order; each box is ticked as soon as its step is done.

## Unit Context

| Item | Value |
|---|---|
| Workspace root | `/Users/mmomo/Code/AIDLC/daily-journal-android` |
| Module | `:app`, Android application `com.mmomo.dailyjournal`, package `com.mmomo.dailyjournal.app` |
| Depends on | `:domain`, `:data`, `:widget` (skeleton; the widget refresher stays a no-op until Unit 4) |
| Design inputs | Application design (components, services S1–S12), `construction/app/nfr-requirements/`, `nfr-design/`, stories (Back, midnight lock, permission, export) |
| Change to an approved unit | Data: `JournalEnvironmentProvider` becomes `suspend fun awaitEnvironment()` (App NFR design §1) |

## Stories Covered (UI parts)

- [ ] US-1.1, US-1.2, US-1.3, US-1.4 (Today prompt card, refresh, categories)
- [ ] US-2.1, US-2.2, US-2.3, US-2.4, US-2.5 (editor, midnight lock, backfill sheet, entry detail, focus and keyboard)
- [ ] US-2.6 (manifest: backup exclusion, no internet; Settings note)
- [ ] US-3.1, US-3.2, US-3.3 (History, search, export through the share sheet)
- [ ] US-4.1, US-4.2, US-4.3, US-4.4 (streak card)
- [ ] US-5.2 (widget and link routing to Today; the widget itself is Unit 4)
- [ ] US-6.1, US-6.2, US-6.3, US-6.4 (permission flow, reminder and restart receivers)
- [ ] US-7.1, US-7.2 (onboarding, Settings)

---

## Steps

### Part A: Build and data change

- [x] **Step 1. Resolve versions.** Latest stable Compose BOM, Activity Compose, Lifecycle ViewModel and Runtime Compose, Navigation Compose, kotlinx-serialization core (plugin = Kotlin version), Kotlin Compose compiler plugin (= Kotlin version). Add them to the catalog.
- [x] **Step 2. `app/build.gradle.kts`.** Compose and serialization plugins, dependencies, `release` build type (minify, shrink resources, not debuggable, debug signing per C1:B), `proguard-rules.pro` with log stripping, `versionName "1.0.0"` / `versionCode 1`, Robolectric test settings like `:data`, Kover.
- [x] **Step 3. Data change.** `JournalEnvironmentProvider.awaitEnvironment()` (suspend); data tests still pass.

### Part B: App code (`app/src/main/kotlin/com/mmomo/dailyjournal/app/`)

- [x] **Step 4. Startup.** `JournalApplication` (error handler, StrictMode in debug, provider), `di/AppStartup` (start state, background creation, retry, await), `di/AppContainer` (environment, ViewModel factories, no-op widget refresher).
- [x] **Step 5. Design system.** `ui/theme/` (iOS colors light and dark, typography with system serif, shapes), shared components (`PrimaryButton`, `Card`, `StreakCard`, `PromptCard`).
- [x] **Step 6. Activity and navigation.** `MainActivity` (start state screens, edge-to-edge, link parsing in `onCreate`/`onNewIntent`), `navigation/Routes` (typed), `navigation/JournalNavHost`, `navigation/DeepLinkRouter` (pending link until ready and onboarded).
- [x] **Step 7. Onboarding.** `ui/onboarding/` ViewModel and screen (two steps, categories, optional reminder with the permission request, Back to step 1).
- [x] **Step 8. Today.** `ui/today/` ViewModel and screen (streak card, backfill card, prompt card with refresh, editor or saved entry with Edit; toolbar to History and Settings; refresh signals).
- [x] **Step 9. Editor.** `ui/editor/` ViewModel and composable (text and edit time in saved state, 0.8 s autosave, Done, Back saves, midnight flush and lock message, 20,000-character limit with paste rejection, status line and word count, keyboard and scrolling).
- [x] **Step 10. Backfill.** `ui/backfill/` ViewModel and sheet (yesterday's prompt, Save, Back asks to discard, window closing at 12:00).
- [x] **Step 11. History and entry.** `ui/history/` (list, debounced search, empty and no-match states, Back clears search) and `ui/entry/` (read-only or editable detail).
- [x] **Step 12. Settings and export.** `ui/settings/` (categories with "at least one", reminder switch and time, permission state re-read on resume, "Open settings", export note and actions), `export/ExportSharer` with a `FileProvider`.
- [x] **Step 13. Receivers.** `reminders/ReminderReceiver`, `reminders/RescheduleReceiver` (`goAsync`, 9-second limit, error class logging).
- [x] **Step 14. Manifest and resources.** Manifest (activity, receivers, file provider, `allowBackup=false`, data-extraction and full-backup rules excluding everything, no internet), `res/xml` rules and file paths, strings, a simple app icon (vector, adaptive), theme for the first frame.

### Part C: Tests and verification

- [x] **Step 15. ViewModel tests.** Over the domain `TestEnvironment` with a test dispatcher: Today (load, refresh, signals), editor (autosave, Done, Back save, failure keeps text, limit, midnight flush and lock), backfill (save, discard confirm, closed window), History (search, Back clears), Settings (categories, permission states, denied on open), onboarding, deep-link routing (pending link, unknown link), startup (ready, failed, retry).
- [x] **Step 16. Stateful property test.** Editor ViewModel: random edit, pause, Done, Back and midnight events against a model (PBT-06).
- [x] **Step 17. Full build.** `./gradlew build` (warnings as errors, Lint) and `:app:assembleRelease`; review every log line in `:app`; record test counts and coverage.
- [x] **Step 18. Run on an emulator.** Set up an Android emulator if none exists (system image and virtual device through the SDK tools installed with Android Studio), install the release build, and walk through onboarding, Today (type, autosave, Done), refresh, History and an entry with Back, Settings (reminder permission, export); take screenshots.

### Part D: Supply chain, docs, CI

- [x] **Step 19. Dependency verification.** Regenerate with `--refresh-dependencies --no-configuration-cache`; check platform-specific artifacts; strict build passes.
- [x] **Step 20. Documentation.** `aidlc-docs/construction/app/code/code-summary.md` (files, versions, data change, tests, coverage, log review, emulator walkthrough with screenshots, compliance); README (install instructions for your phone, release tagging).
- [ ] **Step 21. Commit and push, then check CI.** Commit to `main`, push, confirm `Build and test` passes.

## Not Applicable Steps

| Template step | Reason |
|---|---|
| API layer | No network |
| Repository layer | Data unit |
| Database migrations | Data unit (schema v1) |

## Expected Scope

About 35 main source files, 10 test files, manifest and resources in `:app`; one small change in `:data`.
