# Unit of Work Story Map

Each story is assigned to every unit that implements part of it. "Rules" means business rules and their tests; "Storage" means ports implemented on Android; "UI" means screens and ViewModels; "Widget" means the Home Screen widget.

| Story | Domain | Data | App | Widget |
|---|---|---|---|---|
| US-1.1 See today's prompt | Rules: `PromptSelector`, `GetTodayStateUseCase`, `PromptLibraryValidator` | Storage: `BundledPromptLibrary`, prompt file check | UI: Today prompt card | |
| US-1.2 Same prompt everywhere | Rules: deterministic pick, `StableHash` matching iOS | Storage: prompt state store | | Widget shows the same prompt |
| US-1.3 Refresh prompt | Rules: `RefreshPromptUseCase`, limit, misuse scenario | Storage: prompt state | UI: refresh button | |
| US-1.4 Choose categories | Rules: `UpdatePromptCategoriesUseCase` | Storage: settings store | UI: Settings, Onboarding | |
| US-2.1 Write and save | Rules: `SaveTodayEntryUseCase`, valid text | Storage: entry database (one per day), save failure mapping | UI: editor, autosave, Done, error message | |
| US-2.2 Edit until midnight | Rules: `EntryEditPolicy` | Storage: `ClockEvents` midnight signal | UI: midnight lock, Back saves | |
| US-2.3 Backfill yesterday | Rules: `BackfillYesterdayUseCase`, deadline | | UI: backfill sheet, Back confirm | |
| US-2.4 Read-only past entries | Rules: `GetEntryUseCase`, editability | | UI: entry detail | |
| US-2.5 Open editor fast | Rules: `GetTodayStateUseCase` | | UI: focus, keyboard, scrolling, launch speed | |
| US-2.6 Journal stays on my phone | | Storage: private database and preferences | UI: manifest (backup opt-out, no internet), Settings note | |
| US-3.1 Browse | Rules: `ListEntriesUseCase` | Storage: list query | UI: History, Back rules | |
| US-3.2 Search | Rules: `EntrySearchMatcher`, `SearchEntriesUseCase` | Storage: search query, performance with 3,650 entries | UI: search, Back clears search | |
| US-3.3 Export | Rules: `JournalExporter`, `ExportJournalUseCase` | | UI: `ExportSharer`, share sheet | |
| US-4.1 Current streak | Rules: `StreakCalculator`, DST, time-zone misuse | Storage: entry days query | UI: streak card | |
| US-4.2 At risk | Rules: at-risk status | | UI: at-risk message | |
| US-4.3 Reset after grace | Rules: reset at 12:00, clock misuse | Storage: `ClockEvents` 12:00 signal | UI: reload at 12:00 | |
| US-4.4 Best streak | Rules: best streak, calculated not stored | | UI: Best pill | |
| US-5.1 Widget content | Rules: `TodayState` | Storage: `JournalEnvironmentProvider` | | Widget: content, done, at risk, setup |
| US-5.2 Tap widget | Rules: `DeepLink` | | UI: `MainActivity`, `DeepLinkRouter`, unknown links | Widget: explicit tap intent |
| US-5.3 Widget stays current | Rules: `DayMath` boundaries | | UI: passes refresher | Widget: `WidgetUpdateScheduler`, `GlanceWidgetRefresher` |
| US-6.1 Turn on reminder | Rules: `ConfigureReminderUseCase` | Storage: `AndroidReminderScheduler` permission state | UI: permission request, denied state on open | |
| US-6.2 Skip when done | Rules: `ReminderPlanner`, `SyncReminderWithTodayUseCase` | Storage: scheduling, `ReminderNotifier` | UI: `ReminderReceiver` re-check | |
| US-6.3 Tap reminder | Rules: `DeepLink` | Storage: notification tap intent | UI: `MainActivity` routing | |
| US-6.4 Reminder keeps working | Rules: `SyncReminderWithTodayUseCase` | Storage: scheduling survives update | UI: `RescheduleReceiver` | |
| US-7.1 Onboarding | Rules: `CompleteOnboardingUseCase` | Storage: settings | UI: Onboarding | Widget: setup invitation until done |
| US-7.2 Settings | Rules: `GetSettingsUseCase` | Storage: settings persistence | UI: Settings, Back | |

All 26 stories are assigned. Every story touches Domain except US-2.6 (privacy is a platform and storage concern).

## Technical-only Requirements

| Requirement | Unit |
|---|---|
| NFR-1.1 to NFR-1.4 Platform, Kotlin, Compose, modules, English | Domain (project setup), App |
| NFR-1.5 Visual design | App, Widget |
| NFR-2.2, NFR-2.4 Backup opt-out, no internet, no analytics | App (manifest) |
| NFR-2.5 Encryption at rest | Data (private storage only) |
| NFR-3.x Date and time correctness | Domain |
| NFR-4.1 to NFR-4.3 Unit and property-based tests | Domain, Data (and App for ViewModels) |
| NFR-4.4 CI | Domain (project setup) |
| NFR-5.1 Launch speed | App |
| NFR-5.2 Search speed | Data |
| NFR-6.2 Logging without journal text | App, Data |
| NFR-6.3 Deep links | Domain (`DeepLink`), App (`DeepLinkRouter`), Widget (intent) |
| NFR-6.4 Release hardening | App |
| NFR-6.5 Supply chain (pinned versions, scanning, SBOM) | Domain (project setup and CI) |
| NFR-6.6 Clock misuse | Domain |
| NFR-6.7 Prompt validation | Domain (validator), Data (loader and check test) |
| NFR-6.8 Error handling | Data (error mapping), App (messages, top-level handler) |
| NFR-7.2 Non-destructive migrations, tagged releases | Data; tagging in Build and Test |
| NFR-7.3 GitHub Issues and regression tests | All units (process) |

Every functional and non-functional requirement is assigned to at least one unit.
