# Logical Components: Unit 3 App

| Component | Responsibility | Lifetime |
|---|---|---|
| `JournalApplication` | Error handler, StrictMode (debug), starts `AppStartup`; implements `JournalEnvironmentProvider` | Process |
| `AppStartup` | Creates the environment in the background; `StateFlow<StartState>`; `retry()`; `awaitEnvironment()` | Process |
| `AppContainer` | Holds `JournalEnvironment`; builds ViewModel factories; exposes `refreshSignals`, `ReminderNotifier`, `NotificationPermissionFlag` | Process (after Ready) |
| `MainActivity` | Single activity; observes startup state; hosts navigation; parses deep links (`onCreate`, `onNewIntent`) | Activity |
| `JournalNavHost` + typed routes | Onboarding, Today, History, Entry(dayKey), Settings; back stack rules | Activity |
| `DeepLinkRouter` | Pending link until Ready and onboarded; routes to Today with the right action | Activity |
| ViewModels | `OnboardingViewModel`, `TodayViewModel`, `EntryEditorViewModel`, `BackfillViewModel`, `HistoryViewModel`, `EntryDetailViewModel`, `SettingsViewModel` | Navigation entry |
| Screens | Composables per route plus shared components (streak card, prompt card, editor, primary button, card) | Composition |
| `JournalTheme` | Colors (iOS accent, light and dark), typography (system serif for prompts), shapes | App |
| `ExportSharer` | Writes the export to the app's cache, shares it through a `FileProvider` URI with temporary read permission | Call |
| `ReminderReceiver` | Posts today's reminder if not done, then syncs (registers the next) | Broadcast |
| `RescheduleReceiver` | Re-syncs reminders after boot, time or zone change, app update | Broadcast |
| Release build config | R8, resource shrinking, log stripping, debug signing (C1:B) | Build |

## Connections

```
Process start
  -> JournalApplication.onCreate -> AppStartup.begin (background)
       -> JournalEnvironmentFactory.create(context, widgetRefresher*, onBackgroundFailure, ReminderReceiver, MainActivity)
       -> AppContainer -> state Ready (or Failed -> "Try again")

MainActivity -> observes state -> NavHost (Onboarding | Today) -> screens -> ViewModels -> use cases
Widget tap / reminder tap -> MainActivity (explicit intent) -> DeepLinkRouter -> Today
Alarm -> ReminderReceiver -> awaitEnvironment -> notify -> sync
Boot / time change / update -> RescheduleReceiver -> awaitEnvironment -> sync

* No-op WidgetRefresher until the Widget unit replaces it with GlanceWidgetRefresher.
```
