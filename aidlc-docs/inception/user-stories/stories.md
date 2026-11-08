# User Stories

**Source**: `aidlc-docs/inception/requirements/requirements.md` (approved)
**Plan**: `aidlc-docs/inception/plans/story-generation-plan.md` (approved)
**Base**: the approved iOS stories (`demo/aidlc-docs/inception/user-stories/stories.md`), adapted for Android (plan Q3:A)
**Personas**: `personas.md`. P1 Everyday Journaler, P2 Habit Builder, P3 Lapsed Journaler

## Conventions

- **Format**: "As a [persona], I want [capability], so that [benefit]", followed by Given / When / Then acceptance criteria (Q4:A).
- **Same ids as iOS** (Q2:A): story US-x.y here matches iOS US-x.y. Android-only stories are added inside the matching epic. Text that differs from iOS is marked **Android:**.
- **Clock**: unless a scenario says otherwise, times are local to the device in **America/Mexico_City** (UTC−6, no daylight saving), and "today" is **Thursday 2026-10-08**, the same as iOS, so both apps' tests share scenarios. In tests these run against an injectable fixed clock (NFR-4.2).
- **Grace cutoff**: 12:00 local time the day after a missed day (FR-2.4).
- **Back**: the Android system Back button or gesture (Q6:A).
- **Invariants** (Q5:A): rules that must hold for all inputs. They become property-based tests in Functional Design (PBT-01).
- **Security and misuse** scenarios sit inside the relevant stories (Q8:A).
- 26 stories in epics E1 to E7: the 25 iOS stories plus one Android-only story (US-6.4).

---

## E1: Daily Prompt (FR-1)

### US-1.1: See today's prompt
**As** P1 Everyday Journaler, **I want** a journaling prompt waiting for me when I open the app, **so that** I never start from a blank page.

**Acceptance criteria**
- **Scenario: Prompt on Today screen.** **Given** onboarding is complete, **When** I open the app on 2026-10-08, **Then** the Today screen shows one prompt from the bundled library, with its category name.
- **Scenario: Works offline.** **Given** Airplane Mode is on, **When** I open the app, **Then** today's prompt still appears. **Android:** the app has no internet permission at all (NFR-2.4).
- **Scenario: New day, new prompt.** **Given** the app was open showing the 2026-10-08 prompt, **When** the clock passes 00:00 on 2026-10-09 and I return to the app, **Then** the Today screen shows the 2026-10-09 prompt.
- **Scenario: Same library as iOS.** **Given** the bundled library, **When** the app starts, **Then** it contains the same 600 prompts (150 per category) with the same ids and texts as the iOS app, and the file passes validation (ids, categories, lengths, no duplicates) (FR-1.2, NFR-6.7).

### US-1.2: Same prompt everywhere, all day
**As** P1 Everyday Journaler, **I want** today's prompt to stay the same across launches and in the widget, **so that** I'm not confused by a prompt that changes.

**Acceptance criteria**
- **Scenario: Same prompt after relaunch.** **Given** the Today screen showed prompt X at 08:00 on 2026-10-08, **When** I swipe the app away from Recents and reopen it at 21:00, **Then** prompt X is still shown.
- **Scenario: App and widget agree.** **Given** today's prompt is X, **When** I look at the Home Screen widget, **Then** it shows prompt X.
- **Scenario: No recent repeats.** **Given** my selected categories contain at least 30 prompts, **When** daily prompts are picked for any 30 consecutive days, **Then** no prompt appears twice in that window.
- **Scenario: Same as iOS.** **Given** the same date, the same selected categories, no refreshes and no entries in the previous 30 days, **When** the prompt is picked on Android and on iOS, **Then** both pick the same prompt (FR-1.4).

**Invariants**
- For any date and any non-empty category selection, the pick is always the same prompt and always from the selected categories.
- Over any window of N consecutive days, where N is the number of prompts in the selected categories, no prompt is picked twice.

### US-1.3: Refresh today's prompt (limited)
**As** P3 Lapsed Journaler, **I want** to swap a prompt that doesn't speak to me, **so that** I'm more likely to write today.

**Acceptance criteria**
- **Scenario: Refresh shows a different prompt.** **Given** today's prompt is X and I have used 0 refreshes, **When** I tap "New prompt", **Then** a different prompt Y from my selected categories is shown and the button shows "2 left".
- **Scenario: Limit reached.** **Given** I have used 3 refreshes today, **When** I view the Today screen, **Then** the refresh button is disabled and shows "No refreshes left today".
- **Scenario: Limit resets at midnight.** **Given** I used 3 refreshes on 2026-10-08, **When** it is 00:00 on 2026-10-09, **Then** I have 3 refreshes available again.
- **Scenario: Refreshed prompt persists.** **Given** I refreshed to prompt Y, **When** I relaunch the app or view the widget, **Then** prompt Y is shown, not X.
- **Scenario: No refresh after saving.** **Given** I have saved today's entry, **When** I view the Today screen, **Then** the refresh button is hidden and the prompt saved with the entry is shown (FR-1.6).
- **Misuse scenario: Moving the clock to get more refreshes.** **Given** I used 3 refreshes on 2026-10-08, **When** I set the phone's date to 2026-10-09, refresh, and set it back to 2026-10-08, **Then** 2026-10-08 still shows 0 refreshes left, nothing crashes and no stored data is damaged (NFR-6.6). Refreshes gained on the other date apply only to that date.

**Invariants**
- For any sequence of refresh attempts on one day, at most 3 succeed and refreshes remaining never goes below 0.
- A refresh never returns a prompt already shown that day, while unshown prompts remain in the selected categories.

### US-1.4: Choose prompt categories
**As** P1 Everyday Journaler, **I want** to choose which kinds of prompts I get, **so that** the prompts match what I want to reflect on.

**Acceptance criteria**
- **Scenario: Select categories.** **Given** I'm in Settings, Prompt categories, **When** I turn off "Goals" and keep Gratitude, Reflection and Creativity, **Then** future daily prompts and refreshes come only from those three categories.
- **Scenario: At least one category.** **Given** only "Gratitude" is selected, **When** I try to turn it off, **Then** the app prevents it and explains that at least one category is required.
- **Scenario: Today's prompt is stable.** **Given** today's prompt X is already shown, **When** I change my categories, **Then** today's prompt stays X and the new selection applies from tomorrow's prompt and to any refresh today.

---

## E2: Quick Entry (FR-2, FR-8)

### US-2.1: Write and save today's entry
**As** P1 Everyday Journaler, **I want** to type a short answer to today's prompt and save it, **so that** journaling takes under a minute.

**Acceptance criteria**
- **Scenario: Save entry.** **Given** no entry exists for 2026-10-08, **When** I type "Grateful for a quiet morning." and tap Done at 21:00, **Then** an entry dated 2026-10-08 is stored with that text and today's prompt, and the Today screen shows it as done.
- **Scenario: Autosave.** **Given** I'm typing today's entry, **When** I pause typing, **Then** the text is saved within about a second and "Saved" is shown, without leaving the editor or hiding the keyboard.
- **Scenario: Empty entry rejected.** **Given** the entry field contains only spaces or newlines, **When** I look at the Done button, **Then** it is disabled.
- **Scenario: One entry per day.** **Given** an entry for 2026-10-08 exists, **When** I open the Today screen, **Then** I see that entry with an Edit option, not a new blank entry.
- **Scenario: Prompt frozen with entry.** **Given** I saved an entry while prompt Y was shown, **When** I view it later in History, **Then** prompt Y is shown with it.
- **Security scenario: Save fails.** **Given** saving to storage fails (for example the phone is out of space), **When** I tap Done, **Then** I see a short, generic error message, my typed text stays in the editor, and no partial or corrupt entry is stored (NFR-6.8).

**Invariants**
- After any sequence of saves on one day, exactly one entry exists for that day, and it holds the last saved text.
- An entry's calendar day never changes after it is saved, whatever the time zone (NFR-3.2).

### US-2.2: Edit today's entry until midnight
**As** P1 Everyday Journaler, **I want** to add to or fix today's entry later in the day, **so that** I can capture thoughts as they come.

**Acceptance criteria**
- **Scenario: Edit same day.** **Given** I saved an entry at 08:00 on 2026-10-08, **When** I tap Edit and change its text at 22:00 the same day, **Then** the updated text is stored and the entry is still dated 2026-10-08.
- **Scenario: Locked after midnight.** **Given** an entry for 2026-10-08 exists, **When** it is 00:00 or later on 2026-10-09, **Then** that entry is read-only, including during the grace window (FR-2.5).
- **Scenario: Typing across midnight.** **Given** I'm editing the 2026-10-08 entry at 23:59:30, **When** the clock passes 00:00, **Then** all text typed before midnight is kept in the 2026-10-08 entry, the editor becomes read-only, and a message explains the entry is now locked. The 2026-10-09 editor is not swapped in until I leave the locked entry (FR-8.1). **Android:** on iOS this behavior was a known gap; here it is required from the start.
- **Scenario: Back while editing.** **Given** I'm editing today's entry with unsaved changes, **When** I press Back, **Then** the text is saved before leaving, and nothing I typed is lost (Q6:A).

### US-2.3: Backfill a missed yesterday until noon
**As** P3 Lapsed Journaler, **I want** to write yesterday's missing entry the next morning, **so that** one late night doesn't cost me my streak.

**Acceptance criteria**
- **Scenario: Backfill offered.** **Given** no entry exists for 2026-10-08, **When** I open the app at 09:00 on 2026-10-09, **Then** the Today screen offers "Write yesterday's entry" with "Available until 12:00", in addition to today's prompt.
- **Scenario: Backfill saved.** **Given** the backfill option is shown, **When** I write and save an entry at 11:30 on 2026-10-09, **Then** it is stored with date 2026-10-08 and yesterday's prompt.
- **Scenario: Grace window closes.** **Given** no entry exists for 2026-10-08, **When** it is 12:00 or later on 2026-10-09, **Then** the backfill option is gone and 2026-10-08 can't be written.
- **Scenario: No backfill if yesterday exists.** **Given** an entry exists for 2026-10-08, **When** I open the app at 09:00 on 2026-10-09, **Then** no backfill option is offered.
- **Scenario: Only yesterday.** **Given** no entries exist for 2026-10-07 or 2026-10-08, **When** I open the app at 09:00 on 2026-10-09, **Then** only 2026-10-08 can be backfilled.
- **Scenario: Back from backfill.** **Given** the backfill editor is open with text typed, **When** I press Back, **Then** I'm asked whether to discard the text or keep writing; nothing is discarded without confirmation. **Android:** iOS has a Cancel button instead.

**Invariants**
- Only the day immediately before today can be backfilled, only when it has no entry, and only before 12:00 local time.

### US-2.4: Read-only past entries
**As** P1 Everyday Journaler, **I want** past entries to be read-only, **so that** my journal is an honest record.

**Acceptance criteria**
- **Scenario: Past entry locked.** **Given** an entry dated 2026-10-05, **When** I open it from History on 2026-10-08, **Then** it is shown read-only with no edit control.
- **Scenario: Today's entry editable from History.** **Given** today's entry exists, **When** I open it from History, **Then** I can edit it (same rules as US-2.2).

**Invariants**
- For any entry day and any current time, the entry is editable only if its day is today.

### US-2.5: Open the editor fast
**As** P1 Everyday Journaler, **I want** to start typing almost immediately, **so that** a quick entry really is quick.

**Acceptance criteria**
- **Scenario: Ready to type.** **Given** the app is not running and today's entry isn't written, **When** I launch it (or tap the widget or reminder), **Then** within about 1 second on a mid-range phone the editor shows today's prompt with the keyboard up and the cursor in the entry field (NFR-5.1).
- **Scenario: Done-today state.** **Given** today's entry is already saved, **When** I open the app, **Then** the keyboard is not raised automatically, and the saved entry is shown with an Edit option.
- **Scenario: Long entries stay visible.** **Given** I'm typing a long entry, **When** the text reaches the keyboard, **Then** the screen scrolls so the line I'm typing and the Done button stay visible above the keyboard (FR-2.8).

### US-2.6: My journal stays on my phone
**Android:** replaces the iOS sync story (no sync or backup on Android, NFR-2).

**As** P1 Everyday Journaler, **I want** my journal to stay only on my phone, **so that** nobody else, including Google, can read it.

**Acceptance criteria**
- **Scenario: Works fully offline.** **Given** the phone has no connection, **When** I use any feature, **Then** everything works, and the app never needs the internet.
- **Scenario: No cloud backup.** **Given** Android's automatic backup is turned on for the phone, **When** the phone backs up, **Then** the journal, settings and prompt state are not included, and a phone set up from that backup starts with an empty journal (NFR-2.2).
- **Scenario: Private by design.** **Given** the app is installed, **When** I look at its permissions, **Then** it has no internet access, and no analytics or crash reporting is included (NFR-2.4).
- **Scenario: Encrypted at rest.** **Given** the phone has a screen lock, **When** the journal is stored, **Then** it is protected by Android's built-in storage encryption (NFR-2.5).
- **Scenario: Accepted risk is visible.** **Given** I'm on the Export section in Settings, **When** I read it, **Then** a short note explains that export is the only copy of my journal outside this phone (NFR-2.3).

---

## E3: Past Entries (FR-3)

### US-3.1: Browse past entries
**As** P1 Everyday Journaler, **I want** to scroll back through my journal, **so that** I can reflect on how I've changed.

**Acceptance criteria**
- **Scenario: Reverse chronological list.** **Given** entries exist for 2026-10-01, 2026-10-05 and 2026-10-08, **When** I open History from the Today screen, **Then** they are listed 2026-10-08, 2026-10-05, 2026-10-01, each showing date, prompt and a text preview.
- **Scenario: Empty state.** **Given** no entries exist, **When** I open History, **Then** a friendly empty state invites me to write today's entry.
- **Scenario: Entry stays on its date after travel.** **Given** I wrote an entry at 22:00 on 2026-10-08 in America/Mexico_City, **When** I later change the phone's time zone to Asia/Tokyo, **Then** History still lists that entry under 2026-10-08 (NFR-3.2).
- **Scenario: Back navigation.** **Given** I opened an entry from History, **When** I press Back, **Then** I return to History once, and pressing Back again returns to Today. Opening an entry never adds a second History screen (Q6:A; iOS navigation fix from 2026-10-28).

### US-3.2: Search entries
**As** P1 Everyday Journaler, **I want** to search my journal, **so that** I can find the time I wrote about something.

**Acceptance criteria**
- **Scenario: Match entry text.** **Given** one entry contains "beach", **When** I search "beach", **Then** that entry is listed.
- **Scenario: Match prompt text.** **Given** one entry answered a prompt containing "grateful", **When** I search "grateful", **Then** that entry is listed.
- **Scenario: Case and accent insensitive.** **Given** an entry contains "Café", **When** I search "cafe", **Then** that entry is listed.
- **Scenario: No results.** **Given** nothing matches "zebra", **When** I search "zebra", **Then** a "No entries match" message is shown.
- **Scenario: Large journal.** **Given** about 3,650 entries exist, **When** I type a search term, **Then** results update without noticeable lag (NFR-5.2).
- **Scenario: Back clears search first.** **Given** I typed a search term, **When** I press Back, **Then** the search is cleared first; pressing Back again returns to Today.

**Invariants**
- Search results are always a subset of all entries, newest first, and an empty search returns all entries.
- An entry matches a query if and only if its text or prompt contains the query, ignoring case and accents.

### US-3.3: Export my journal
**As** P1 Everyday Journaler, **I want** to export my entries, **so that** I own a copy outside the app.

**Acceptance criteria**
- **Scenario: Markdown export.** **Given** entries exist, **When** I choose "Export as Markdown" in Settings, **Then** the Android share sheet offers a `.md` file with one section per entry (date heading, prompt, text), oldest first.
- **Scenario: Plain-text export.** **Given** entries exist, **When** I choose "Export as plain text", **Then** the share sheet offers a `.txt` file with the same content without Markdown syntax.
- **Scenario: Nothing to export.** **Given** no entries exist, **When** I open Export, **Then** the export actions are disabled with an explanation.
- **Scenario: Same format as iOS.** **Given** the same entries, **When** I export on Android and on iOS, **Then** the files have the same structure, so a journal can be compared or merged by hand.
- **Security scenario: Export stays private.** **Given** I export, **When** the share sheet opens, **Then** the file is only readable by the app I choose to share it with; the app doesn't keep extra copies in shared storage.

**Invariants**
- Every entry appears exactly once in an export, in date order, with its full text unchanged.

---

## E4: Streak (FR-4)

### US-4.1: See my current streak
**As** P2 Habit Builder, **I want** to see how many days in a row I've journaled, **so that** I stay motivated.

**Acceptance criteria**
- **Scenario: Streak including today.** **Given** entries exist for 2026-10-06, 10-07 and 10-08, **When** I view the Today screen on 2026-10-08, **Then** the streak shows 3.
- **Scenario: Today not written yet.** **Given** entries exist for 2026-10-06 and 10-07 but not 10-08, **When** I view the Today screen at 09:00 on 2026-10-08, **Then** the streak shows 2 (ending yesterday) and is not marked at risk.
- **Scenario: No entries.** **Given** no entries exist, **When** I view the Today screen, **Then** the streak shows 0 with "Start your streak today".
- **Scenario: Daylight-saving change.** **Given** the phone is in America/New_York and entries exist for 2026-10-31, 2026-11-01 (the 25-hour day DST ends) and 2026-11-02, **When** I view the streak on 2026-11-02, **Then** it shows 3.
- **Misuse scenario: Changing time zone.** **Given** a 3-day streak ending today in America/Mexico_City, **When** I change the phone's time zone to Pacific/Kiritimati (UTC+14), **Then** entries keep their dates, the streak is recalculated for the new local date without crashing, and no entry is lost or moved (NFR-6.6).

**Invariants**
- The current streak is never greater than the number of days with entries, and never negative.
- Adding an entry for any day never lowers the current or best streak.

### US-4.2: Streak at risk during grace
**As** P3 Lapsed Journaler, **I want** to be warned my streak is at risk instead of seeing it reset immediately, **so that** I can save it with a backfill.

**Acceptance criteria**
- **Scenario: At-risk shown.** **Given** entries exist for 2026-10-05, 10-06 and 10-07 and none for 10-08, **When** I view the Today screen at 09:00 on 2026-10-09, **Then** the streak shows 3 with the message "Write yesterday's entry by 12:00 to keep your 3-day streak".
- **Scenario: Backfill restores.** **Given** the at-risk state above, **When** I backfill 2026-10-08 at 11:00 on 2026-10-09, **Then** the streak shows 4 and is no longer at risk.
- **Scenario: Backfill then today.** **Given** I backfilled 2026-10-08 as above, **When** I also save today's entry for 2026-10-09, **Then** the streak shows 5.
- **Scenario: Today written, yesterday missing, before noon.** **Given** entries exist for 10-05 to 10-07, none for 10-08, and I saved today's 10-09 entry at 08:00, **When** I view the streak at 09:00 on 2026-10-09, **Then** it shows 1 with "Write yesterday's entry by 12:00 to restore your 3-day streak". If I then backfill 10-08, it shows 5.

### US-4.3: Streak resets after grace
**As** P2 Habit Builder, **I want** the streak to follow clear rules, **so that** I trust it.

**Acceptance criteria**
- **Scenario: Reset at cutoff.** **Given** entries exist for 10-05 to 10-07, none for 10-08, and no backfill, **When** it is 12:00 on 2026-10-09 and I haven't written today, **Then** the streak shows 0.
- **Scenario: Restart.** **Given** the streak was reset as above, **When** I save an entry for 2026-10-09 at 20:00, **Then** the streak shows 1.
- **Scenario: Two missed days.** **Given** the last entry is for 2026-10-06, **When** I open the app at 09:00 on 2026-10-09, **Then** the streak shows 0 (grace covers only yesterday).
- **Misuse scenario: Clock moved back.** **Given** the streak was reset at 12:00 on 2026-10-09, **When** I set the clock back to 11:00, **Then** the at-risk state and backfill offer come back for that hour (the rules follow the phone's clock), and nothing crashes or is damaged (NFR-6.6).

### US-4.4: See my longest streak
**As** P2 Habit Builder, **I want** to see my best streak ever, **so that** I have something to beat.

**Acceptance criteria**
- **Scenario: Longest shown.** **Given** past entries contain a 12-day run and the current streak is 4, **When** I view the streak card, **Then** it shows the current streak 4 and "Best 12".
- **Scenario: New record.** **Given** the best streak is 12 and the current streak is 12, **When** I save today's entry making the current streak 13, **Then** the best streak shows 13.
- **Scenario: Calculated, not stored.** **Given** the stored entries, **When** the streak is shown, **Then** both current and best streak are calculated from the entries, never read from a saved counter (FR-4.6).

**Invariants**
- The best streak is always at least the current streak, and equals the longest run of consecutive entry days.

---

## E5: Widget (FR-5)

### US-5.1: Prompt and streak on my Home Screen
**As** P1 Everyday Journaler, **I want** a small Home Screen widget showing today's prompt and my streak, **so that** I'm reminded to write without opening the app.

**Acceptance criteria**
- **Scenario: Widget content.** **Given** today's prompt is X and the streak is 5, **When** I add the "Daily Prompt" widget (Android: long-press the Home Screen, Widgets), **Then** it shows prompt X (truncated with an ellipsis if too long) and a corner badge showing 5.
- **Scenario: Done mark.** **Given** today's entry is saved, **When** I view the widget, **Then** the streak badge includes a checkmark.
- **Scenario: At risk.** **Given** the streak is at risk (US-4.2), **When** I view the widget before 12:00, **Then** the badge shows the at-risk state.
- **Scenario: Before onboarding.** **Given** the app has been installed but onboarding isn't finished, **When** I add the widget, **Then** it invites me to open the app (FR-5.6).
- **Scenario: Matches the app's look.** **Given** light or dark mode, **When** I view the widget, **Then** it uses the app's colors and serif prompt text (NFR-1.5).

### US-5.2: Tap widget to write
**As** P1 Everyday Journaler, **I want** tapping the widget to open the editor directly, **so that** I can start writing in one tap.

**Acceptance criteria**
- **Scenario: Deep link to editor.** **Given** today's entry isn't written, **When** I tap the widget, **Then** the app opens on the Today screen with the editor focused, today's prompt loaded and the keyboard up.
- **Scenario: Already done.** **Given** today's entry is saved, **When** I tap the widget, **Then** the app opens showing today's saved entry with an Edit option.
- **Scenario: Another screen was open.** **Given** the app was left on History, Settings or an entry, **When** I tap the widget, **Then** the app returns to the Today screen with the editor ready (FR-5.4). **Android:** on iOS this behavior was a known gap; here it is required from the start.
- **Security scenario: Unknown links.** **Given** another app sends the journal app a link it doesn't recognize, **When** the app receives it, **Then** it simply opens the Today screen and does nothing else (NFR-6.3).

### US-5.3: Widget stays current
**As** P2 Habit Builder, **I want** the widget to update by itself, **so that** it never shows a stale prompt or streak.

**Acceptance criteria**
- **Scenario: Midnight rollover.** **Given** the widget shows the 2026-10-08 prompt, **When** it is 00:00 on 2026-10-09, **Then** the widget shows the 2026-10-09 prompt and recalculated streak, without the app being opened.
- **Scenario: Grace cutoff.** **Given** the widget shows the at-risk state, **When** it is 12:00 and no backfill happened, **Then** the widget shows the streak as 0.
- **Scenario: After save, refresh or settings change.** **Given** the widget is on the Home Screen, **When** I save an entry, refresh the prompt or change categories in the app, **Then** the widget updates to match when I return to the Home Screen.

---

## E6: Reminder Notifications (FR-6)

### US-6.1: Turn on a daily reminder
**As** P3 Lapsed Journaler, **I want** a daily reminder at a time I choose, **so that** I don't forget to write.

**Acceptance criteria**
- **Scenario: Enable with permission.** **Given** reminders are off and notification permission hasn't been asked, **When** I turn on the reminder (in onboarding or Settings), **Then** on Android 13 or later the system asks for notification permission, and if I allow it, a daily reminder is scheduled for 20:00 by default. **Android:** on Android 10–12 no permission dialog is needed.
- **Scenario: Change time.** **Given** the reminder is on at 20:00, **When** I change it to 07:30, **Then** future reminders fire at 07:30.
- **Scenario: Permission denied.** **Given** I denied notification permission, **When** I open Settings at any later time, **Then** the reminder is shown as unavailable with a button that opens the system notification settings for the app (FR-6.3). **Android:** on iOS this state only appeared after toggling; here it is required on opening Settings.
- **Scenario: Permission granted later.** **Given** the reminder showed as unavailable, **When** I allow notifications in system settings and return to the app, **Then** Settings lets me turn the reminder on.
- **Scenario: No early prompt.** **Given** a fresh install, **When** I go through onboarding without turning on the reminder, **Then** I'm never asked for notification permission.
- **Scenario: Turn off.** **Given** the reminder is on, **When** I turn it off, **Then** no further reminders fire.

### US-6.2: No reminder once I've written
**As** P3 Lapsed Journaler, **I want** no reminder if I've already written today, **so that** notifications don't nag me.

**Acceptance criteria**
- **Scenario: Skip when done.** **Given** the reminder is set for 20:00, **When** I save today's entry at 08:00, **Then** no reminder fires today, and tomorrow's reminder is still scheduled.
- **Scenario: Fires when not done.** **Given** the reminder is set for 20:00 and no entry exists for today, **When** it is 20:00, **Then** the reminder fires showing today's prompt.
- **Scenario: Backfill doesn't silence today.** **Given** the reminder is set for 20:00, **When** I backfill yesterday but don't write today, **Then** today's reminder still fires.

**Invariants**
- On any day, a reminder fires at most once, and never after that day's entry is saved.

### US-6.3: Tap reminder to write
**As** P3 Lapsed Journaler, **I want** tapping the reminder to open the editor, **so that** I can write straight away.

**Acceptance criteria**
- **Scenario: Open editor.** **Given** the reminder notification is showing, **When** I tap it, **Then** the app opens the Today screen with the editor and today's prompt, as in US-5.2, even if another screen was open.

### US-6.4: Reminder keeps working (Android only)
**Android:** new story. Android cancels scheduled alarms when the phone restarts, and the iOS app has no equivalent concern.

**As** P3 Lapsed Journaler, **I want** my reminder to keep working after a restart or a time change, **so that** I can rely on it.

**Acceptance criteria**
- **Scenario: Phone restart.** **Given** the reminder is on at 20:00, **When** I restart the phone at 15:00, **Then** the reminder still fires at 20:00 without opening the app first.
- **Scenario: Time zone change.** **Given** the reminder is on at 20:00 in America/Mexico_City, **When** I travel and the phone switches to America/New_York, **Then** the reminder fires at 20:00 New York time.
- **Scenario: App updated.** **Given** the reminder is on, **When** I install a newer version of the app, **Then** the reminder stays scheduled.

---

## E7: Onboarding and Settings (FR-7)

### US-7.1: First-launch onboarding
**As** P1 Everyday Journaler, **I want** a short setup the first time I open the app, **so that** prompts and reminders fit me from day one.

**Acceptance criteria**
- **Scenario: Shown once.** **Given** a fresh install, **When** I open the app, **Then** a two-step onboarding appears. After finishing it, it never appears again.
- **Scenario: Choose categories.** **Given** I'm on the categories step, **When** I continue, **Then** at least one category must be selected (all four are selected by default).
- **Scenario: Optional reminder.** **Given** I'm on the reminder step, **When** I choose "Not now, start journaling", **Then** onboarding completes with the reminder off and no permission prompt. **When** I turn it on and pick a time, **Then** US-6.1 applies.
- **Scenario: Lands on Today.** **Given** I finish onboarding, **When** it closes, **Then** the Today screen appears with today's prompt and the editor ready.
- **Scenario: Back during onboarding.** **Given** I'm on the reminder step, **When** I press Back, **Then** I return to the categories step; on the categories step, Back leaves the app without completing onboarding.

### US-7.2: Settings screen
**As** P1 Everyday Journaler, **I want** one place to change my preferences, **so that** I can adjust the app as my habits change.

**Acceptance criteria**
- **Scenario: Settings contents.** **Given** I'm on the Today screen, **When** I open Settings, **Then** I see Prompt categories (US-1.4), Daily reminder on/off and time (US-6.1), and Export (US-3.3) with the note from US-2.6.
- **Scenario: Changes persist.** **Given** I change any setting, **When** I restart the app or the phone, **Then** the change is kept.
- **Scenario: Back.** **Given** I'm in Settings, **When** I press Back, **Then** I return to Today, and any change I made is already saved.

---

## Persona-to-Story Map

| Story | P1 Everyday | P2 Habit Builder | P3 Lapsed |
|---|---|---|---|
| US-1.1 See today's prompt | Primary | | |
| US-1.2 Same prompt everywhere | Primary | Yes | |
| US-1.3 Refresh prompt | Yes | | Primary |
| US-1.4 Choose categories | Primary | | |
| US-2.1 Write and save | Primary | Yes | Yes |
| US-2.2 Edit until midnight | Primary | | |
| US-2.3 Backfill yesterday | | Yes | Primary |
| US-2.4 Read-only past entries | Primary | | |
| US-2.5 Open editor fast | Primary | | Yes |
| US-2.6 Journal stays on my phone (Android) | Primary | Yes | |
| US-3.1 Browse | Primary | | |
| US-3.2 Search | Primary | | |
| US-3.3 Export | Primary | Yes | |
| US-4.1 Current streak | Yes | Primary | |
| US-4.2 At risk during grace | | Yes | Primary |
| US-4.3 Reset after grace | | Primary | Yes |
| US-4.4 Longest streak | | Primary | |
| US-5.1 Widget content | Primary | Yes | |
| US-5.2 Tap widget to write | Primary | | Yes |
| US-5.3 Widget stays current | | Primary | |
| US-6.1 Turn on reminder | | | Primary |
| US-6.2 Skip when done | | | Primary |
| US-6.3 Tap reminder to write | | | Primary |
| US-6.4 Reminder keeps working (Android) | | | Primary |
| US-7.1 Onboarding | Primary | | Yes |
| US-7.2 Settings | Primary | | |

---

## Requirements Traceability

| Requirement | Stories | Notes |
|---|---|---|
| FR-1.1 | US-1.1 | |
| FR-1.2 | US-1.1 | Same 600 prompts as iOS |
| FR-1.3 | US-1.4, US-7.1 | |
| FR-1.4 | US-1.2 | Includes cross-platform equality |
| FR-1.5 | US-1.3 | |
| FR-1.6 | US-1.3, US-1.4, US-2.1 | |
| FR-2.1, FR-2.2, FR-2.7 | US-2.1 | |
| FR-2.3 | US-2.1 (autosave), US-2.2 | |
| FR-2.4 | US-2.3 | |
| FR-2.5 | US-2.2, US-2.3 | |
| FR-2.6 | US-2.4 | |
| FR-2.8 | US-2.5 | |
| FR-2.9 | None (no deletion feature) | Out of scope by design |
| FR-3.1 | US-3.1 | |
| FR-3.2 | US-3.2 | |
| FR-3.3 | US-3.3, US-2.6 | |
| FR-4.1, FR-4.2 | US-4.1 | |
| FR-4.3 | US-4.3 | |
| FR-4.4 | US-4.2 | |
| FR-4.5 | US-4.4 | |
| FR-4.6 | US-4.4 | |
| FR-5.1, FR-5.2, FR-5.6 | US-5.1 | |
| FR-5.3, FR-5.5 | US-5.3, US-1.2 | |
| FR-5.4 | US-5.2 | iOS gap, required here |
| FR-6.1, FR-6.3 | US-6.1 | FR-6.3 iOS gap, required here |
| FR-6.2 | US-6.2 | |
| FR-6.4 | US-6.3 | |
| FR-6.5 | US-6.4 | Android only |
| FR-7.1 | US-7.1 | |
| FR-7.2 | US-7.2 | |
| FR-8.1 | US-2.2 | iOS gap, required here |
| NFR-1.1 to NFR-1.4 | Technical only | Platform choices |
| NFR-1.5 | US-5.1 | Visual design; also applies to all screens |
| NFR-2.1, NFR-2.2, NFR-2.4, NFR-2.5 | US-2.6 | |
| NFR-2.3 | US-2.6, US-7.2 | Accepted risk shown in Settings |
| NFR-3.1, NFR-3.3 | US-1.1, US-2.2, US-2.3, US-4.1, US-4.2, US-4.3, US-5.3, US-6.4 | Midnight, 12:00 cutoff, DST, time zone |
| NFR-3.2 | US-2.1, US-3.1 | |
| NFR-4.1 to NFR-4.3 | All E1, E2, E4 stories | Scenarios become example-based tests; Invariants become property-based tests |
| NFR-4.4, NFR-4.5 | Technical only | CI and quality scope |
| NFR-5.1 | US-2.5 | |
| NFR-5.2 | US-3.2 | |
| NFR-6.1 | US-2.6 | |
| NFR-6.2, NFR-6.4, NFR-6.5 | Technical only | Logging, hardening, supply chain |
| NFR-6.3 | US-5.2 | Unknown links |
| NFR-6.6 | US-1.3, US-4.1, US-4.3 | Clock and time zone misuse |
| NFR-6.7 | US-1.1 | Prompt file validation |
| NFR-6.8 | US-2.1 | Save failure |
| NFR-7.1 to NFR-7.3 | Technical only | Process |

---

## INVEST Check

| Criterion | Result |
|---|---|
| Independent | Each story can be built and tested on its own once the shared domain rules exist; widget and reminder stories depend only on Today state, not on each other |
| Negotiable | Scenarios describe behavior, not implementation; layout and wording can change |
| Valuable | Every story maps to a persona goal (map above) |
| Estimable | Every story has concrete scenarios with dates and times |
| Small | Largest stories (US-2.2, US-6.1) each cover one feature with 4–6 scenarios |
| Testable | Every story has Given/When/Then scenarios; business-rule stories also list invariants |

All 26 stories have acceptance criteria.

---

## Open Points for Review

These go slightly beyond the approved requirements, to make the stories testable on Android. Please accept or change them.

1. **Back from yesterday's entry** (US-2.3): pressing Back with text typed asks whether to discard it, instead of iOS's Cancel button. Today's entry is autosaved instead (US-2.2).
2. **Settings note about export** (US-2.6, US-7.2): a one-line note in the Export section says export is the only copy outside the phone. This only adds text; there's no new feature (plan Q7:A).
3. **Reminder timing** (US-6.2, US-6.4): Android may deliver a reminder a few minutes late to save battery, unless the app uses exact alarms (which needs an extra permission on Android 12+). The stories say "fires at 20:00"; the tolerance will be decided in design.
4. **Same prompt as iOS** (US-1.2): matching iOS exactly means porting the iOS selection algorithm, including its hash function. It holds only when neither app has entries in the previous 30 days, because the no-repeat rule depends on each phone's own entries.
5. **Clock tampering** (US-1.3, US-4.3): the app follows the phone's clock and doesn't try to detect cheating; it only guarantees no crash and no data damage, which suits a personal journal.
