# Code Summary: Unit 3 App

**Plan**: `aidlc-docs/construction/plans/app-code-generation-plan.md`

## Versions Added (Step 1, latest stable at 2026-11-07)

| Library | Version | Scope |
|---|---|---|
| Compose BOM (ui, foundation, material3) | 2026.09.00 | `:app` |
| Activity Compose | 1.13.0 | `:app` |
| Lifecycle ViewModel Compose, Runtime Compose | 2.11.0 | `:app` |
| Navigation Compose (typed routes) | 2.10.2 | `:app` |
| kotlinx-serialization core | 1.12.0 | `:app` (route classes only) |
| Kotlin Compose compiler and serialization plugins | 2.4.21 (= Kotlin) | Build |
| DataStore Preferences | 1.3.0 (already in `:data`) | `:app` tests only (temporary file for startup tests) |

## Files Created or Changed

**Build**: `gradle/libs.versions.toml`, root `build.gradle.kts` (Kover includes `:app`), `app/build.gradle.kts` (Compose, serialization, release build type: minified, resources shrunk, not debuggable, signed with this Mac's debug key per C1:B; `versionName 1.0.0`, `versionCode 1`), `app/proguard-rules.pro` (strips `Log.v`, `Log.d`, `Log.i`), `gradle/verification-metadata.xml` (539 components).

**Data change (Step 3)**: `JournalEnvironmentProvider` now has `suspend fun awaitEnvironment()`, so receivers and the widget wait for the environment that's created in the background at startup.

**App main code** (`app/src/main/kotlin/com/mmomo/dailyjournal/app/`, 26 files):
- `JournalApplication` (error handler logging the class only, StrictMode in debug, environment provider), `MainActivity` (start states, edge to edge, deep links in `onCreate` and `onNewIntent`)
- `di/`: `AppStartup` (Loading, Ready, Failed, retry, await), `AppContainer` (no-op widget refresher until the Widget unit)
- `navigation/`: `Routes` (typed), `JournalNavHost`, `DeepLinkRouter` (holds a link until ready and onboarded), `ViewModels` (factories)
- `ui/theme/Theme`, `ui/components/` (`JournalCard`, `PrimaryButton`, `SmallPillButton`, `StreakCard`, `PromptCard`, user-facing error messages)
- `ui/onboarding/`, `ui/today/`, `ui/editor/`, `ui/backfill/`, `ui/history/`, `ui/entry/`, `ui/settings/`: a ViewModel and a screen each
- `export/ExportSharer` (cache file shared through a `FileProvider` with temporary read permission)
- `reminders/Receivers` (`ReminderReceiver`, `RescheduleReceiver`: `goAsync`, 9-second limit)

**Resources**: manifest (single activity, `dailyjournal://today` and `dailyjournal://backfill`, two non-exported receivers, file provider, `allowBackup=false`, backup and data-extraction rules excluding everything, no internet permission, `RECEIVE_BOOT_COMPLETED`), 10 vector icons, adaptive launcher icon, light and dark colors and first-frame theme.

**App tests** (`app/src/test/kotlin/…`, 6 files): `support/ViewModelTestSupport`, `EntryEditorViewModelTest`, `ScreenViewModelsTest` (Today, Backfill, History, Entry), `SetupViewModelsTest` (Settings, Onboarding, DeepLinkRouter), `AppStartupTest` (Robolectric), `property/EditorStatefulPropertyTest`.

## Results

| Module | Tests | Result |
|---|---|---|
| Domain | 113 | All pass |
| Data | 43 | All pass |
| App | 32 (31 example-based, 1 stateful property test with 200 random sequences) | All pass |
| **Total** | **188** | **All pass** |

- `./gradlew build :app:assembleRelease` with warnings as errors and Android Lint: passes; strict dependency verification passes.
- Coverage (Kover, report only): domain lines 98.1%, data 86.8%, app 30.3% (ViewModels are tested; the screen composables aren't, as chosen in Q5:A); whole project lines 68.1%.
- Release APK: 3.0 MB, not debuggable, permissions `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` only, signed with the Android debug certificate.

## Log Line Review (APP-SEC-6)

| File | Message |
|---|---|
| `JournalApplication.kt` | "Uncaught exception: {exception class}" |
| `di/AppStartup.kt` | "Startup failed: {exception class}" |
| `di/AppContainer.kt` | "Background update failed: {exception class}" |
| `reminders/Receivers.kt` | "{Reminder or Reschedule} failed: {exception class}" |

None can contain journal text, prompts or settings. The startup error screen shows generic text only; a test checks that the failure reason is the class name, not the exception message.

## Emulator Walkthrough (Step 18)

Release build on the existing `Medium_Phone_API_37.0` emulator (Android 17, API 37). A physical phone was also connected over USB; nothing was installed on it. Screenshots are in `docs/screenshots/`.

| Check | Result |
|---|---|
| Fresh install shows onboarding; categories, then reminder | Pass |
| Turning the reminder on shows the Android notification dialog; Allow lands on Today | Pass |
| Editor focused with the keyboard up; Done visible above the keyboard | Pass |
| Autosave after typing shows "✓ Saved" and **the keyboard stays open** | Pass after fix (see below) |
| Done closes the editor and shows the saved entry with Edit | Pass |
| Backfill: Back with text asks to discard; Keep writing; Save; streak becomes 2 | Pass |
| History lists both days; search "saturday" filters; Back clears the search first | Pass |
| A past entry is read-only; Back returns to History | Pass |
| Settings shows the reminder at 20:00; Markdown export opens the share sheet with `daily-journal-2026-11-08.md` | Pass |
| Next alarm registered for 2026-11-09 20:00 (today already written, so skipped) | Pass |
| Notification permission revoked: Settings shows "Notifications are off" and "Open settings"; the pending alarm was cancelled at startup | Pass |
| `dailyjournal://today` while open routes in place; an unknown link isn't accepted by the system | Pass |
| Cold start to first screen | 648 ms (target about 1 s) |

Not checked on the emulator: a reminder alarm actually firing (its receiver isn't exported, so the shell can't trigger it; covered by design and the data unit's alarm tests), rotation, and dark mode screenshots.

## Fixes and Refinements During Generation

| Change | Why |
|---|---|
| **Autosave no longer cancels its own job** (`EntryEditorViewModel.saveNow`) | Found on the emulator: the autosave job called `saveNow()`, which cancelled the autosave job, that is, itself. The database write finished, but the status stayed on "Saving…" and Today wasn't told. Done, Back and the midnight flush now cancel the pending autosave before saving. A regression test with storage that really suspends fails before the fix and passes after; the earlier tests missed it because the fakes never suspend |
| `TodayViewModel` first load moved into the collector (`onStart`) | The first load request could be emitted before its collector subscribed; it only worked on a device because of immediate dispatch |
| `RECEIVE_BOOT_COMPLETED` in the manifest | Needed for `RescheduleReceiver` to re-register the reminder after a restart (FR-6.5); install-time, no prompt |
| Deep-link `<data>` tags split, `ic_lock` removed, no default `SavedStateHandle()` in production ViewModels | Lint warnings |
| `LocalActivity` instead of casting `LocalContext` | Lint error |

Remaining Lint warnings, accepted: the portrait lock (approved; Android 16+ ignores it on large screens only), and two `InlinedApi` notes on `POST_NOTIFICATIONS` (the dialog is only requested on Android 13+).

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01 (private storage, backup and transfer excluded), 03 (four log lines, class names only), 05 (deep links parsed to a fixed set; route keys validated), 06 (only notifications at runtime, no internet), 09 (release build: not debuggable, minified, logs stripped, generic errors), 11 (non-exported receivers, explicit immutable intents, `FileProvider` with temporary grants), 15 (startup failure state, errors mapped to generic messages): compliant. Server-only rules N/A |
| Property-Based Testing | PBT-06 (stateful editor test against a model: typing, pauses, Done, Back, midnight), 07 (domain generators and fixtures reused), 08 (seed printed on failure), 09 (Kotest), 10 (property test in its own file): compliant. PBT-02, 03 and 05 N/A for this unit (no new pure business rules; covered in Domain and Data) |

## Stories Completed (app parts)

US-1.1 to 1.4, US-2.1 to 2.6, US-3.1 to 3.3, US-4.1 to 4.3, US-5.2 (deep links; the widget itself is the Widget unit), US-6.1 to 6.4, US-7.1, US-7.2, FR-8.1 (midnight lock while editing).
