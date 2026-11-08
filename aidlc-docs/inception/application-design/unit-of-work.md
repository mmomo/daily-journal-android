# Units of Work

**Plan**: `aidlc-docs/inception/plans/unit-of-work-plan.md` (approved; answers Q1–Q6 all A)

Four units, one per Gradle module, built in order: **Domain → Data → App → Widget**. Each unit is fully designed, coded, tested and approved before the next starts. All four ship together in one app.

| # | Unit | Module | Construction stages |
|---|---|---|---|
| 1 | Domain | `:domain` (pure Kotlin) + project setup | Functional Design, NFR Requirements, NFR Design, Code Generation |
| 2 | Data | `:data` (Android library) | Functional Design, NFR Requirements, NFR Design, Code Generation |
| 3 | App | `:app` (Android application) | NFR Requirements, NFR Design, Code Generation |
| 4 | Widget | `:widget` (Android library) | Code Generation |

Functional Design runs for Domain and Data only (Q6:A). NFR stages run where the unit brings new technology or NFR decisions: Domain (project-wide stack, PBT framework, CI and supply chain), Data (storage, reminders, backup, migrations), App (UI stack, permissions, logging, hardening, performance). Widget skips them because its decisions are already made by the units before it: the widget library and versions in Domain's stack decisions, and update scheduling in Data. If the Widget unit turns out to need its own NFR decisions, the stages can be added then.

**Prerequisite**: Android Studio (with the Android SDK) installed before Domain's Code Generation, because project setup creates all four Android modules and Gradle needs the SDK to configure them.

---

## Unit 1: Domain

**Purpose**: Set up the project and implement every business rule as pure Kotlin, ported file by file from the iOS `JournalDomain` (application design Q9:A).

**Responsibilities**
- **Project setup** (first steps, Q1:A): Gradle wrapper, `settings.gradle.kts`, root `build.gradle.kts`, version catalog `gradle/libs.versions.toml` with exact versions, skeletons for all four modules, GitHub Actions workflow (build, tests including PBT, dependency vulnerability scanning, SBOM) (NFR-4.4, NFR-6.5)
- Entities and value types, policies, ports and the 14 use cases (`components.md` §1)
- Example-based tests reusing the iOS scenarios, and property-based tests for the invariants in the stories (NFR-4.1, NFR-4.3)

**Exit criteria**: `./gradlew :domain:test` passes (example and property-based tests); CI runs on GitHub and passes; the domain has no Android imports.

## Unit 2: Data

**Purpose**: Implement the domain ports on Android and package the prompt library.

**Responsibilities**
- Entry database (one entry per day, day stored separately from timestamps, non-destructive migrations) and repository with change signals
- Preferences stores for settings and per-day prompt state
- `BundledPromptLibrary` with the copied `prompts.json` (Q5:A)
- `SystemTimeSource`, `ClockEvents`
- `AndroidReminderScheduler`, `ReminderNotifier`
- `JournalEnvironmentFactory` (takes a `WidgetRefresher` from the caller) and `JournalEnvironmentProvider`
- Tests: repository and stores round-trips, migration safety, prompt file check (600 prompts, ids match iOS), factory wiring

**Exit criteria**: data tests pass locally and in CI; the domain use cases run end to end against the real storage in tests.

## Unit 3: App

**Purpose**: Everything the user sees except the widget.

**Responsibilities**
- `JournalApplication` (implements `JournalEnvironmentProvider`), `AppContainer`, top-level error handler
- `MainActivity`, `JournalNavGraph`, `DeepLinkRouter`
- ViewModels and screens: Onboarding, Today, Editor (autosave, midnight lock), Backfill, History, Entry detail, Settings (permission state on open, export note)
- `ExportSharer`, `ReminderReceiver`, `RescheduleReceiver`
- Design system matching iOS (NFR-1.5), test tags on interactive elements
- Manifest: backup and device-transfer opt-out, no internet permission, notification permission, receivers
- Until the Widget unit exists, the app passes a no-op `WidgetRefresher` to the factory
- Tests: ViewModel tests with fakes (save, autosave, midnight lock, Back rules, permission state)

**Exit criteria**: the app builds, installs and runs on an emulator or phone with every screen working; ViewModel tests pass.

## Unit 4: Widget

**Purpose**: The Home Screen widget.

**Responsibilities**
- `PromptStreakWidget` (Glance), `PromptStreakWidgetReceiver`, `WidgetStateLoader`, `WidgetUpdateScheduler`, `GlanceWidgetRefresher`
- The app switches from the no-op refresher to `GlanceWidgetRefresher` and registers the widget receiver
- Tests: widget state mapping (ready, needs setup, done, at risk), update scheduling times

**Exit criteria**: the widget shows the same prompt and streak as the app, updates after saves and at midnight and 12:00, and opens Today when tapped.

---

## Code Organization (greenfield)

Standard Gradle multi-module layout at the repository root (Q3:A). Application id and base package `com.mmomo.dailyjournal` (Q4:A).

```
daily-journal-android/
+-- settings.gradle.kts              (includes :domain, :data, :app, :widget)
+-- build.gradle.kts                 (root: plugins, common settings)
+-- gradle.properties
+-- gradle/
|   +-- libs.versions.toml           (all dependency versions, pinned)
|   +-- wrapper/                     (Gradle wrapper)
+-- gradlew, gradlew.bat
+-- .github/
|   +-- workflows/
|       +-- ci.yml                   (build, tests, scan, SBOM)
+-- domain/
|   +-- build.gradle.kts             (Kotlin JVM library)
|   +-- src/main/kotlin/com/mmomo/dailyjournal/domain/
|   |   +-- entities/  policies/  ports/  usecases/  time/
|   +-- src/test/kotlin/com/mmomo/dailyjournal/domain/
|       +-- (example-based tests)  property/  (property-based tests)  support/  (generators, fakes)
+-- data/
|   +-- build.gradle.kts             (Android library)
|   +-- src/main/kotlin/com/mmomo/dailyjournal/data/
|   |   +-- database/  stores/  prompts/  reminders/  system/  composition/
|   +-- src/main/assets/prompts.json (copy of the iOS file)
|   +-- src/test/kotlin/com/mmomo/dailyjournal/data/
+-- app/
|   +-- build.gradle.kts             (Android application, applicationId com.mmomo.dailyjournal)
|   +-- src/main/AndroidManifest.xml
|   +-- src/main/kotlin/com/mmomo/dailyjournal/app/
|   |   +-- di/  navigation/  ui/today/  ui/editor/  ui/history/  ui/settings/  ui/onboarding/  ui/theme/  reminders/
|   +-- src/main/res/                (backup rules, icons, strings)
|   +-- src/test/kotlin/com/mmomo/dailyjournal/app/
+-- widget/
|   +-- build.gradle.kts             (Android library)
|   +-- src/main/kotlin/com/mmomo/dailyjournal/widget/
|   +-- src/main/res/                (widget provider info, layouts for previews)
|   +-- src/test/kotlin/com/mmomo/dailyjournal/widget/
+-- aidlc-docs/                      (documentation only, never code)
```

Text alternative: four module folders (`domain/`, `data/`, `app/`, `widget/`) at the root, each with `src/main/kotlin/com/mmomo/dailyjournal/<module>/` and `src/test/kotlin/...`; build files and the Gradle wrapper at the root; CI in `.github/workflows/ci.yml`; prompts in `data/src/main/assets/prompts.json`.

**Prompts** (Q5:A): `data/src/main/assets/prompts.json` is a copy of the iOS `JournalCore/Sources/JournalData/Resources/prompts.json` (version 1, 600 prompts). A Data test checks the count, validity and ids. Future prompt changes are made in both repositories.
