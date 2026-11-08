# Tech Stack Decisions: Unit 3 App

Project policy: latest stable versions at the start of this unit's Code Generation, pinned; official Kotlin, AndroidX and Google libraries only in shipped code; checksums regenerated with `--refresh-dependencies --no-configuration-cache`; Java 21 builds, Java 17 bytecode.

| Area | Choice | Rationale |
|---|---|---|
| UI | Jetpack Compose through the Compose BOM; Material 3; `activity-compose` | NFR-1.2 |
| Compose compiler | Kotlin Compose compiler plugin (same version as Kotlin) | Required for Compose with Kotlin 2.x |
| ViewModels | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `SavedStateHandle` | Application design Q1, APP-REL-1 |
| Navigation | Navigation Compose with typed routes | Application design Q7 |
| Route types | kotlinx-serialization (official Kotlin plugin and core library) | Required by typed routes |
| Fonts | The phone's built-in serif (`FontFamily.Serif`) | Q2:A |
| Back | Classic Back (`BackHandler`), no predictive Back opt-in | Q3:B |
| Build types | `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, R8 rules that strip `Log.v/d/i`, `isDebuggable = false`, `signingConfig = debug` | APP-SEC-1, APP-SEC-2 (C1:B) |
| Splash / first frame | A simple Compose loading screen while the environment is created off the main thread | APP-PERF-1 |
| Tests | Kotest for pure ViewModel logic; JUnit 4 + Robolectric where Android classes are needed (same pattern as the Data unit); kotlinx-coroutines-test; the domain's test fixtures | Q5:A |

**Versions to resolve at Code Generation**: Compose BOM, Activity Compose, Lifecycle (ViewModel and runtime Compose), Navigation Compose, kotlinx-serialization (plugin = Kotlin version, core library).

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-01 (backup and transfer excluded), 03 (logs reviewed; low-level logs stripped in release), 05 (deep links and route arguments validated), 06 (only the notification permission, no internet), 09 (hardened release build installed; C1:B resolved the conflict), 10 (official pinned libraries), 11 (only the activity exported; receivers not exported), 15 (top-level handler, generic errors): compliant. Server-only rules N/A |
| Property-Based Testing | No business rules of its own (they're in the domain, already covered). PBT-06 applied to the editor ViewModel's state machine (APP-MAIN-2); other rules N/A for this unit |
