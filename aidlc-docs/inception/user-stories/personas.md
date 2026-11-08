# Personas

Following plan decision Q1:A, the Android app reuses the three iOS personas unchanged, described as Android phone users. As on iOS, there is one kind of user (someone journaling privately on their own phone), modeled as **one primary persona** plus **two behavioral variants** that stress different requirements.

Android differences from the iOS personas are marked **Android:**.

---

## P1: Sam, the Everyday Journaler (Primary)

| Attribute | Description |
|---|---|
| **Context** | Adult with a busy routine who wants a few minutes of reflection a day. Uses an Android phone (Android 10 or later). **Android:** one phone; there is no sync to other devices (NFR-2.1) |
| **Goal** | Write a short, honest entry most days without having to think of what to write |
| **Motivations** | Less mental clutter, noticing patterns over time, a calm daily ritual |
| **Frustrations** | Staring at a blank page; apps with accounts, ads or social features; slow, multi-tap flows |
| **Journaling habit** | 2–5 sentences, usually in the evening, sometimes from the Home Screen widget |
| **Tech comfort** | Comfortable with Android basics (widgets, notifications, share sheet, Back gesture). Doesn't want to configure much |
| **Key requirements** | FR-1 (prompt), FR-2 (quick entry), FR-3 (history and export), FR-5 (widget), NFR-2 (privacy, on-device only), NFR-5 (speed) |

---

## P2: Habit Builder (Variant of P1)

| Attribute | Description |
|---|---|
| **Context** | Same as Sam, but driven mostly by keeping the streak going |
| **Goal** | Never break the streak, and beat their longest streak |
| **Motivations** | Visible progress; the satisfaction of a "done today" checkmark |
| **Frustrations** | A streak that resets unfairly: travel across time zones or daylight-saving changes. **Android:** no synced entries to worry about, but losing the phone loses the streak's history unless exported |
| **Behavior** | Checks the streak badge on the widget daily. Writes early in the day to "lock in" the streak |
| **Key requirements** | FR-4 (streak, longest streak), FR-5.2 (widget badge, done and at-risk states), NFR-3 (date and time correctness) |

---

## P3: Lapsed Journaler (Variant of P1)

| Attribute | Description |
|---|---|
| **Context** | Same as Sam, but with an irregular schedule; often forgets or falls asleep before writing |
| **Goal** | Get back into the habit without feeling punished for missing a day |
| **Motivations** | Gentle nudges; a second chance the next morning |
| **Frustrations** | Losing a long streak over one late night; reminders that fire after they've already written. **Android:** a reminder that silently stops after a phone restart |
| **Behavior** | Relies on the daily reminder. Often writes yesterday's entry the next morning |
| **Key requirements** | FR-2.4/FR-2.5 (grace backfill until 12:00), FR-4.3/FR-4.4 (at-risk state and reset), FR-6 (reminder, skipped once done, survives restarts), FR-1.5 (refresh when a prompt doesn't fit) |

---

## Persona Summary

| Persona | Primary concern | Most-used touchpoints |
|---|---|---|
| P1 Everyday Journaler | Easy, private, quick writing | Today screen, widget, history |
| P2 Habit Builder | Streak correctness and visibility | Widget badge, Today screen streak |
| P3 Lapsed Journaler | Forgiveness and reminders | Notification, grace backfill, Today screen at-risk message |

Persona-to-story mapping is in `stories.md`.
