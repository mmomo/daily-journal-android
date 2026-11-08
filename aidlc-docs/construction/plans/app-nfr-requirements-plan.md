# NFR Requirements Plan: App Unit

**Unit**: App (`:app`, Android application): screens, ViewModels, navigation, deep links, reminder receivers, design system, manifest. See `unit-of-work.md`.
**Inputs**: requirements (NFR-1.5 iOS look, NFR-2 privacy, NFR-5.1 launch speed, NFR-6 security), stories (Back behavior, midnight lock, permission state), application design, the Data unit's `JournalEnvironmentFactory`, project-wide policies (latest stable pinned versions, official libraries only in shipped code, checksums, Kotest, Java 21 builds / Java 17 bytecode).

**Already decided** (not asked again): Jetpack Compose with Material 3; ViewModels; Navigation Compose with typed routes (this needs the official kotlinx-serialization plugin); iOS look with the orange accent, no wallpaper colors; phones in portrait, English only; no internet permission; backup and device-transfer opt-out; only the notification permission; tests run on the JVM with Robolectric where Android is needed.

**Categories considered**:
- Performance: Question 1
- Tech stack and usability (fonts, back gesture): Questions 2–3
- Security and release hardening: Question 4
- Maintainability and testing: Question 5
- Release process (NFR-7.2 tagged releases): Question 6
- **Scalability** and **availability**: N/A (one user, on-device app)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should the app make sure the editor is ready to type in about 1 second after launch (NFR-5.1)?

A) Keep startup light (open storage and load prompts off the main thread while a simple first frame shows) and measure on your phone during Build and Test; no extra tooling

B) Also add a Baseline Profile (an AndroidX tool that precompiles the startup path for faster launch). Needs an extra benchmark module and a device or emulator to generate the profile

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
Which serif font should the prompt text use (iOS uses a serif for prompts)?

A) The phone's built-in serif font (no extra files; looks slightly different between phone makers)

B) Bundle an open-licensed serif font with the app (for example Noto Serif, under the SIL Open Font License), so it looks the same on every phone; adds about 300–600 KB

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
Should the app support Android's predictive Back animation (Android 14+ shows a preview of the previous screen while you swipe back)?

A) Yes: opt in, and make the editor's "save before leaving" and the backfill "discard?" prompt work with it

B) No: use classic Back handling only

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 4
How should the build you install on your phone be produced (personal use, Q13:A)?

A) A release build: code shrinking and obfuscation (R8), unused resources removed, debug and verbose logs stripped, not debuggable; signed with a local keystore kept outside the repository (its passwords in a git-ignored file)

B) A debug build: simplest, but debuggable, larger, slower, and keeps all logs

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 5
What should the App unit's automated tests cover (requirements Q12:A asked for unit tests of business rules, which the domain already has)?

A) ViewModel tests on the JVM (with fakes): autosave and Done, the midnight lock message, Back rules, refresh, backfill, the permission state on opening Settings, deep-link routing; no UI tests

B) A plus a few Compose UI tests run with Robolectric for the main flows (open Today and type, open History and an entry and go Back, Settings)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
How should app versions be numbered and released (NFR-7.2: every release is tagged; rollback = install the previous tagged build)?

A) `versionName` like `1.0.0` set by hand in the build file, `versionCode` increased by one for each release, and a Git tag `v1.0.0` on the commit you install

B) Version name and code derived automatically from the latest Git tag at build time

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/app/nfr-requirements/nfr-requirements.md`: performance, usability (iOS look, Back, keyboard, edge-to-edge), security (manifest, intents, deep links, logs, release hardening, startup errors), reliability (state across screen rebuilds and process death, errors), maintainability (tests, test tags), release process, N/A categories
- [x] 2. Write `aidlc-docs/construction/app/nfr-requirements/tech-stack-decisions.md`: Compose BOM and libraries, Navigation and serialization, fonts, build types and signing, test tooling, version policy
- [x] 3. Security and PBT compliance review (PBT: App has no business rules of its own; ViewModel state machines evaluated for stateful properties)
- [x] 4. Check consistency with requirements, stories, application design and the Data unit
