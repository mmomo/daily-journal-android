# Story Generation Plan: Daily Journal for Android

**Inputs**: `aidlc-docs/inception/requirements/requirements.md` (approved) and the approved iOS stories and personas (`demo/aidlc-docs/inception/user-stories/`).

## Approach options

| Approach | How stories are organized | Fit for this project |
|---|---|---|
| Epic-based by feature (used on iOS) | Epics E1 Daily Prompt … E7 Settings, each with stories | Keeps the same ids and structure as iOS, so parity is easy to check side by side |
| User journey-based | First launch → daily writing → missed day → reviewing the past | Reads naturally, but harder to compare with iOS |
| Persona-based | Stories grouped per persona | Personas share most features, so lots of duplication |
| Domain-based | Prompt, Entry, Streak, Notification domains | Close to the code modules, less user-centered |

The questions below decide which approach to use and how much of the iOS material to reuse.

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
Which personas should the Android stories use?

A) Reuse the three iOS personas unchanged (P1 Everyday Journaler, P2 Habit Builder, P3 Lapsed Journaler), described as Android phone users

B) Reuse the three iOS personas and add one for a person who uses both the iOS and Android apps

C) Write new personas for Android

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
How should the stories be organized?

A) Same feature epics and story ids as iOS (E1–E7, US-1.1 to US-7.2), with Android-only stories added inside the matching epic, so each Android story lines up with its iOS counterpart

B) User journey-based (first launch, daily writing, missed day, reviewing the past)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How much of the iOS story text should be reused?

A) Adapt the iOS stories: same narrative and Given/When/Then scenarios, reworded for Android (share sheet, system back, Android widget), with each Android difference marked "Android:" so it's easy to spot

B) Write every story fresh from the Android requirements

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
How should acceptance criteria be written?

A) Same as iOS: Given/When/Then scenarios with concrete dates and times on a fixed clock (America/Mexico_City, "today" = a fixed date), so each scenario can become a unit test

B) Short checklist-style criteria without fixed dates

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
Should acceptance criteria also state the invariants that property-based tests will check (for example "the streak never exceeds the number of saved days", "the same date and categories always give the same prompt")?

A) Yes, add an "Invariants" line to stories with business rules (prompt, entry rules, streak), so they carry straight into Functional Design (PBT-01)

B) No, keep stories to scenarios only; invariants are identified later in Functional Design

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
Android has a system Back button or gesture. How should stories cover it?

A) Add Back scenarios to the navigation-related stories: Back from an entry returns to History, Back from History or Settings returns to Today, Back on Today leaves the app, and Back never discards typed text

B) No special scenarios; standard Android behavior is assumed

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7
With no automatic backup, export is the only copy of the journal (accepted risk NFR-2.3). Should the stories include anything to help users remember to export?

A) No, export works the same as iOS (this keeps the approved requirements unchanged)

B) Show the date of the last export in Settings, so users can see how old their copy is (small addition; requirements would be updated)

C) B plus a gentle in-app reminder when the last export is older than 30 days (larger addition; requirements would be updated)

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8
How should the security misuse case (NFR-6.6: changing the phone's clock or time zone) and other security requirements appear?

A) As acceptance scenarios inside the relevant stories (for example in the streak and refresh stories: "Given the user moves the clock back…")

B) As a separate "Security and misuse" epic

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Generation checklist

These steps run after the questions are answered, any follow-up questions are resolved and the plan is approved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/inception/user-stories/personas.md` according to Question 1
- [x] 2. Write `aidlc-docs/inception/user-stories/stories.md` header: sources, conventions (format, clock and dates per Question 4), and the epic list per Question 2
- [x] 3. E1 Daily Prompt stories (FR-1)
- [x] 4. E2 Quick Entry stories (FR-2, FR-8.1), including Back behavior per Question 6
- [x] 5. E3 Past Entries stories (FR-3), including export per Question 7
- [x] 6. E4 Streak stories (FR-4)
- [x] 7. E5 Widget stories (FR-5), including FR-5.4 returning to Today
- [x] 8. E6 Reminder stories (FR-6), including Android 13+ permission and FR-6.3 denied state, and FR-6.5 restart behavior
- [x] 9. E7 Onboarding and Settings stories (FR-7)
- [x] 10. Security and misuse coverage per Question 8 (NFR-6.3, NFR-6.6, NFR-6.8)
- [x] 11. Invariants per Question 5
- [x] 12. Persona-to-story map
- [x] 13. Requirements traceability table (every FR and NFR mapped to stories or marked "technical only")
- [x] 14. Check every story against INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable) and that every story has acceptance criteria
- [x] 15. List open points that go beyond the approved requirements, for review
