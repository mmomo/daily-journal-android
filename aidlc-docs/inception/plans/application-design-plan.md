# Application Design Plan: Daily Journal for Android

**Inputs**: `requirements.md`, `stories.md`, `personas.md` (all approved), `execution-plan.md` (approved), and the iOS application design (`demo/aidlc-docs/inception/application-design/`) as the starting point.

**Already decided** (not asked again):
- Clean Architecture in four Gradle modules: domain (pure Kotlin), data, app, widget (NFR-1.3)
- Kotlin, Jetpack Compose with Material 3, Glance for the widget (NFR-1.2)
- Everything on the device; no network (NFR-2)
- Storage library, reminder scheduling mechanism and test tooling are chosen later, in NFR Requirements

**iOS decisions for reference**: views called use cases directly (no view models); one use case per user action; domain defines repository interfaces and data implements them; constructor injection with one environment object; the widget read the shared store and computed with the domain itself; prompts in a bundled JSON file; Apple frameworks only.

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should screens get their data and call business logic? Android differs from iOS here: when the phone rotates, changes theme or the system reclaims memory, Android rebuilds screens, and state not kept in a ViewModel is lost unless saved separately.

A) ViewModels: each screen has a ViewModel that calls use cases and exposes screen state; the composable only displays that state. This is the Android standard and survives screen rebuilds

B) Same as iOS: composables call use cases directly, with no ViewModels. Less code, but typed text and loaded data must be saved by hand when Android rebuilds the screen

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
How fine-grained should the domain use cases be?

A) Same as iOS: one use case per user action (for example `SaveTodayEntryUseCase`, `BackfillYesterdayUseCase`, `RefreshPromptUseCase`), each with one `execute` function and the same names as iOS, so the two codebases are easy to compare

B) A few services grouped by capability (`PromptService`, `EntryService`, `StreakService`, …), each with several functions

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How should the domain access storage, the clock and the system?

A) Same as iOS: the domain defines interfaces ("ports") such as `EntryRepository`, `SettingsStore`, `TimeSource`, `ReminderScheduler`, `WidgetRefresher`; the data module implements them. Tests use fakes, and business rules never touch Android classes

B) The domain uses the storage library directly

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
How should dependencies be wired together?

A) Manual constructor injection with one `AppContainer` created when the app starts, like the iOS `AppEnvironment`. No extra library; the widget and reminder receivers get the same container

B) Hilt, Google's dependency-injection library. Less wiring code, but adds annotation processing and a learning curve

C) Koin, a lightweight dependency-injection library (third-party, not from Google)

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
How should screens learn that data changed (after a save, at midnight, at 12:00, after a time-zone change)?

A) Reactive: storage exposes Kotlin `Flow`s, so screens and the widget update automatically when entries or settings change; a small clock signal also emits at midnight and 12:00

B) Same as iOS: use cases are plain `suspend` functions, and a shared "refresh" signal tells screens to reload after saves and at day boundaries

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
How should the widget get its data? On Android the widget runs inside the app's own process and can read the app's storage directly; there is no App Group problem like on iOS.

A) Same as iOS: the widget reads the same store and uses the domain use cases to work out the prompt and streak itself, so it is always correct even if the app hasn't run today

B) The app writes a small snapshot (prompt, streak, done and at-risk states) for the widget to display; the widget never computes anything

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7
How should navigation work?

A) Navigation Compose with typed routes in a single activity (Today, History, Entry, Settings, Onboarding); the widget and reminder open Today by clearing the back stack (FR-5.4)

B) A simple hand-written screen state (a list of screens) without the Navigation library

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8
What is the policy for third-party libraries in shipped code?

A) Only official Kotlin and AndroidX/Google libraries (Compose, Glance, Navigation, lifecycle and ViewModel, the storage library chosen later, kotlinx-serialization, kotlinx-datetime); test-only libraries such as Kotest are allowed. Closest to iOS's "Apple frameworks only" and keeps the supply chain small (NFR-6.5)

B) Any well-maintained library that saves significant effort

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 9
How should the shared business rules be kept in line with iOS?

A) Port the iOS domain logic file by file to Kotlin (same types, policies and use case names), and reuse the iOS test scenarios, so behavior matches exactly, including the prompt selection hash

B) Write the domain fresh from the requirements and stories, only matching behavior, not structure

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Design Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `components.md`: domain (entities, policies, ports, use cases), data (implementations), app (screens, ViewModels or not per Q1, navigation per Q7, container per Q4), widget; with responsibilities and interfaces
- [x] 2. Write `component-methods.md`: function signatures and input and output types for use cases, ports and main components (business rules are detailed later in Functional Design)
- [x] 3. Write `services.md`: how use cases are orchestrated for each main flow (open Today, save, refresh, backfill, midnight and 12:00 boundaries, widget update, reminder scheduling and restart, deep links, export, onboarding)
- [x] 4. Write `component-dependency.md`: dependency matrix, module dependency rules, communication patterns and data flow diagrams (with text alternatives)
- [x] 5. Map every story to the components that implement it
- [x] 6. Security review of the design (NFR-6: deep links, logging, backup opt-out, error handling, supply chain) and PBT notes (which components carry invariants)
- [x] 7. Write `application-design.md` consolidating the four documents
- [x] 8. Validate completeness and consistency (every requirement and story covered, no module dependency pointing outward from domain)
