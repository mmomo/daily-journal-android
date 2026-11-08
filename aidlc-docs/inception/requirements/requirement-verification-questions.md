# Requirements Verification Questions: Daily Journal for Android

The iOS version's approved requirements and user stories (`/Users/mmomo/Code/AIDLC/demo/aidlc-docs/inception/`) are the starting point. These questions cover what is different or still open for Android.

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want after the tag.

**Recap of the iOS app** (for reference while answering):

- One prompt a day from a bundled library (now 600 prompts in Gratitude, Reflection, Goals and Creativity), with up to 3 refreshes a day
- One text entry per day, editable until midnight, with autosave. A missed yesterday can be written until 12:00 the next day
- Streak counted from saved entries, with an "at risk" state until 12:00, plus the best streak
- Small Home Screen widget with today's prompt and streak; tapping it opens the editor
- Optional daily reminder, skipped once today's entry is written
- History with search, and export as Markdown or plain text
- Short onboarding (categories, optional reminder) and a Settings screen
- iCloud sync between Apple devices; no accounts, no analytics

---

## Question 1
How closely should the Android app match the iOS app's features?

A) Full feature parity: everything in the recap above, with the same rules (refresh limit, editing until midnight, 12:00 grace window, streak rules)

B) Core first: prompt, entry, streak and widget now; history search, export, reminders and onboarding later

C) Full parity plus Android-only extras (describe the extras after the [Answer]: tag)

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Where should the prompts come from? Note that this repository is public, so anything bundled in it becomes public.

A) Reuse the same 600 prompts from the iOS app, so both apps offer identical prompts

B) Write a new prompt library for Android

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
What is the minimum Android version to support?

A) Android 8.0 (API 26): covers almost all active devices

B) Android 10 (API 29): good balance, simpler code

C) Android 12 (API 31): newest widget features and Material You dynamic colors everywhere, fewer devices

D) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4
Which UI toolkit should be used?

A) Jetpack Compose with Material 3 (current Android standard, closest to SwiftUI)

B) Classic XML Views

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
How should the app look?

A) Match the iOS design: warm orange accent, rounded cards, serif prompt text, light and dark mode

B) Standard Material 3 with dynamic colors taken from the user's wallpaper (Material You)

C) iOS design by default, with an option in Settings to use dynamic colors

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
What should the Home Screen widget offer?

A) Same as iOS: one small widget showing today's prompt and a streak badge, tapping opens the editor

B) Small widget plus a resizable larger size that shows more of the prompt

C) No widget for now

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
How should entries be stored and protected against losing the phone? Android has no direct equivalent of iCloud sync, and syncing with the iOS app would need a server, which the iOS app deliberately avoids.

A) Stored on the device only, included in Android's automatic Google backup (restored when setting up a new phone); no live sync

B) Stored on the device only, with no cloud backup at all (most private)

C) Live sync between Android devices through the user's Google account (more work; may need a Google Cloud project)

D) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 8
Should the Android app support daily reminder notifications? On Android 13 and later this requires asking for notification permission.

A) Yes, same as iOS: off by default, permission asked only when the user turns it on, default time 8:00 PM, skipped once today's entry is written

B) No reminders

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Which devices should be supported?

A) Phones only, portrait

B) Phones and tablets (adaptive layout)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 10
Which languages should the app support?

A) English only (same as iOS)

B) English and Spanish

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 11
How should the code be organized?

A) Clean Architecture in separate Gradle modules, mirroring iOS: domain (business rules, pure Kotlin), data (storage, notifications), app (screens) and widget

B) A single app module with packages for each layer (simpler build, less separation)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 12
What level of automated testing is expected?

A) Unit tests for the business rules (streak, prompt selection, refresh limit, editing and backfill rules), like iOS

B) Unit tests plus data-layer tests and a few Compose UI tests for the main flows

C) Minimal tests

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 13
How will the app be distributed?

A) Personal use: build and install on my own phone (debug or locally signed build)

B) Google Play internal or closed testing (needs a Play Console account, one-time $25)

C) Public release on Google Play

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 14
This Mac has Java 21 and adb, but no Android SDK or Android Studio. How should the build tools be set up?

A) I'll install Android Studio myself (it includes the SDK and emulator)

B) Install the Android command-line tools with Homebrew, so builds, tests and an emulator can run from the terminal (I can install Android Studio later)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 15
Should anything else work differently from iOS? For example: allowing more than one entry per day, editing past entries, deleting entries, a different refresh limit, or a different grace-window time.

A) No, keep the iOS rules exactly

B) Yes (describe the differences after the [Answer]: tag)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question: Security Extensions
Should security extension rules be enforced for this project?

A) Yes — enforce all SECURITY rules as blocking constraints (recommended for production-grade applications)

B) No — skip all SECURITY rules (suitable for PoCs, prototypes, and experimental projects)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question: Resiliency Extensions
Should the resiliency baseline be applied to this project?

**What this extension is.** Enabling it applies a set of **directional, design-time best practices** for building resilient systems, derived from the **AWS Well-Architected Framework (Reliability Pillar)** and resilience-review guidance. It steers requirements, design, and code toward fault tolerance, high availability, observability, and recoverability — covering 15 practice areas across business goals, change management, observability, high availability, disaster recovery, and continuous improvement.

**What this extension is NOT.** Enabling it does **not** make your workload production-ready, nor does it certify or guarantee any availability, RTO, or RPO target. It is a **starting point** that scaffolds good resiliency decisions early — it is not a substitute for a formal **AWS Well-Architected Review** of the built system.

Treat the output as a well-grounded **first draft of your resiliency posture** to build on and validate — not a finished, production-certified result.

A) Yes — apply the resiliency baseline as directional best practices and design-time guidance (recommended for business-critical workloads, as an informed starting point that you can validate and harden before go-live)

B) No — skip the resiliency baseline (suitable for PoCs, prototypes, and experimental projects where rapid iteration matters more than reliability)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?

A) Yes — enforce all PBT rules as blocking constraints (recommended for projects with business logic, data transformations, serialization, or stateful components)

B) Partial — enforce PBT rules only for pure functions and serialization round-trips (suitable for projects with limited algorithmic complexity)

C) No — skip all PBT rules (suitable for simple CRUD applications, UI-only projects, or thin integration layers with no significant business logic)

X) Other (please describe after [Answer]: tag below)

[Answer]: A
