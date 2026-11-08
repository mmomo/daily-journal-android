# Component Methods

Kotlin signatures for the main components. Business rules behind each function are detailed later in Functional Design. Types `Instant`, `TimeZone`, `LocalTime` are from kotlinx-datetime (official Kotlin library, plan Q8:A); `Uuid` is a stable entry id. Names follow the iOS code (plan Q9:A).

---

## 1. Domain

### 1.1 Entities (data classes; fields)

```kotlin
data class CalendarDay(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDay> {
    val key: String                       // "2026-10-08"
    val dayNumber: Long                   // days since 1970-01-01, for arithmetic
    fun adding(days: Int): CalendarDay
    fun startIn(timeZone: TimeZone): Instant
    fun noonIn(timeZone: TimeZone): Instant
    companion object {
        fun containing(instant: Instant, timeZone: TimeZone): CalendarDay
        fun fromKey(key: String): CalendarDay?   // null for invalid keys
    }
}

data class JournalEntry(
    val id: String, val day: CalendarDay, val text: String,
    val promptId: String, val promptText: String,
    val createdAt: Instant, val updatedAt: Instant,
) { companion object { fun isValidText(text: String): Boolean } }

enum class PromptCategory { GRATITUDE, REFLECTION, GOALS, CREATIVITY }
data class Prompt(val id: String, val category: PromptCategory, val text: String)

data class DailyPromptState(
    val day: CalendarDay, val promptId: String, val refreshesUsed: Int,
    val shownPromptIds: List<String>, val categoriesKey: String,
)

sealed interface StreakStatus {
    data object Active : StreakStatus
    data class AtRisk(val restorableCount: Int, val until: Instant) : StreakStatus
    data object None : StreakStatus
}
data class StreakSummary(val current: Int, val best: Int, val status: StreakStatus, val isTodayDone: Boolean)

sealed interface EntryEditability {
    data class Editable(val until: Instant) : EntryEditability
    data class Backfillable(val until: Instant) : EntryEditability
    data object Locked : EntryEditability
}

data class BackfillOffer(val day: CalendarDay, val prompt: Prompt, val until: Instant)
data class TodayState(
    val day: CalendarDay, val prompt: Prompt, val entry: JournalEntry?,
    val entryEditability: EntryEditability, val refreshesRemaining: Int,
    val backfillOffer: BackfillOffer?, val streak: StreakSummary, val onboardingCompleted: Boolean,
)
data class EntryDetail(val entry: JournalEntry, val editability: EntryEditability)

data class ReminderSettings(val isEnabled: Boolean, val timeOfDay: LocalTime)   // default off, 20:00
data class AppSettings(
    val categories: Set<PromptCategory>, val reminder: ReminderSettings, val onboardingCompleted: Boolean,
)

enum class ReminderAuthorization { NOT_DETERMINED, ALLOWED, DENIED }
data class ScheduledReminder(val fireAt: Instant, val day: CalendarDay, val promptText: String, val deepLink: DeepLink)

enum class ExportFormat { MARKDOWN, PLAIN_TEXT }
data class ExportDocument(val fileName: String, val mimeType: String, val contents: String)

sealed interface DeepLink {
    data object TodayEditor : DeepLink
    data object BackfillEditor : DeepLink
    fun toUri(): String                                   // "dailyjournal://today", "dailyjournal://backfill"
    companion object { fun parse(uri: String?): DeepLink? }   // null for anything unknown (NFR-6.3)
}

sealed class JournalError : Exception() {
    data object EntryLocked : JournalError()
    data object BackfillNotAvailable : JournalError()
    data object NoRefreshesLeft : JournalError()
    data object EmptyText : JournalError()
    data object CategoriesEmpty : JournalError()
    data class StorageFailure(val reason: String) : JournalError()   // reason is never shown to users
}
```

### 1.2 Policies

```kotlin
class PromptSelector(library: List<Prompt>) {
    fun pool(categories: Set<PromptCategory>): List<Prompt>
    fun dailyPrompt(day: CalendarDay, categories: Set<PromptCategory>, answeredRecently: Set<String> = emptySet()): Prompt
    fun refreshedPrompt(day: CalendarDay, categories: Set<PromptCategory>, refreshNumber: Int,
                        shownToday: List<String>, answeredRecently: Set<String> = emptySet()): Prompt  // throws NoRefreshesLeft
    companion object { const val MAX_REFRESHES_PER_DAY = 3; const val NO_REPEAT_WINDOW_DAYS = 30 }
}

object StableHash { fun fnv1a64(text: String): ULong }

object EntryEditPolicy {
    fun editability(day: CalendarDay, hasEntry: Boolean, now: Instant, timeZone: TimeZone): EntryEditability
    fun backfillDeadline(today: CalendarDay, timeZone: TimeZone): Instant
}

object StreakCalculator {
    fun summary(entryDays: Set<CalendarDay>, now: Instant, timeZone: TimeZone): StreakSummary
}

object JournalExporter { fun export(entries: List<JournalEntry>, format: ExportFormat, today: CalendarDay): ExportDocument }

object EntrySearchMatcher {
    fun normalizedQuery(query: String): String
    fun matches(entry: JournalEntry, query: String): Boolean
}

object ReminderPlanner {
    fun plan(settings: ReminderSettings, today: CalendarDay, isTodayDone: Boolean, todayPrompt: String,
             now: Instant, timeZone: TimeZone, days: Int = 7): List<ScheduledReminder>
}

object PromptLibraryValidator {
    fun validate(prompts: List<Prompt>, minimumPerCategory: Int = 90, minimumTotal: Int = 365)   // throws PromptLibraryError
}
```

### 1.3 Ports

```kotlin
interface TimeSource {
    val now: Instant
    val timeZone: TimeZone
}

interface EntryRepository {
    val changes: Flow<Unit>                                          // emits after any write
    suspend fun entry(day: CalendarDay): JournalEntry?
    suspend fun entries(from: CalendarDay, through: CalendarDay): List<JournalEntry>
    suspend fun entriesAfter(day: CalendarDay): List<JournalEntry>   // future-dated after westward travel
    suspend fun allEntries(): List<JournalEntry>                     // newest first
    suspend fun entryDays(): Set<CalendarDay>
    suspend fun search(query: String): List<JournalEntry>            // newest first
    suspend fun save(entry: JournalEntry)                            // insert or update; one entry per day
}

interface PromptLibrary {
    fun allPrompts(): List<Prompt>
    fun prompt(id: String): Prompt?
}

interface PromptStateStore {
    val changes: Flow<Unit>
    suspend fun state(day: CalendarDay): DailyPromptState?
    suspend fun save(state: DailyPromptState, keepingFrom: CalendarDay)
}

interface SettingsStore {
    val changes: Flow<Unit>
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

interface ReminderScheduler {
    fun authorization(): ReminderAuthorization
    suspend fun replacePending(reminders: List<ScheduledReminder>)
}

interface WidgetRefresher { suspend fun reloadAll() }
```

All storage functions throw `JournalError.StorageFailure` on I/O errors (NFR-6.8).

### 1.4 Use cases

Each is a class with its port dependencies in the constructor and one `execute`:

| Use case | Signature |
|---|---|
| `GetTodayStateUseCase` | `suspend fun execute(): TodayState` |
| `RefreshPromptUseCase` | `suspend fun execute(): Prompt` (throws `EntryLocked`, `NoRefreshesLeft`) |
| `SaveTodayEntryUseCase` | `suspend fun execute(text: String, day: CalendarDay): JournalEntry` (throws `EntryLocked`, `EmptyText`) |
| `BackfillYesterdayUseCase` | `suspend fun execute(text: String, day: CalendarDay): JournalEntry` (throws `BackfillNotAvailable`, `EmptyText`) |
| `GetEntryUseCase` | `suspend fun execute(day: CalendarDay): EntryDetail?` |
| `ListEntriesUseCase` | `suspend fun execute(): List<JournalEntry>` |
| `SearchEntriesUseCase` | `suspend fun execute(query: String): List<JournalEntry>` |
| `ExportJournalUseCase` | `suspend fun execute(format: ExportFormat): ExportDocument` (throws when there are no entries) |
| `GetStreakUseCase` | `suspend fun execute(): StreakSummary` |
| `GetSettingsUseCase` | `suspend fun execute(): AppSettings` |
| `UpdatePromptCategoriesUseCase` | `suspend fun execute(categories: Set<PromptCategory>)` (throws `CategoriesEmpty`) |
| `ConfigureReminderUseCase` | `suspend fun execute(reminder: ReminderSettings): ReminderAuthorization` (saves settings; schedules only when allowed) |
| `SyncReminderWithTodayUseCase` | `suspend fun execute()` |
| `CompleteOnboardingUseCase` | `suspend fun execute(categories: Set<PromptCategory>, reminder: ReminderSettings?)` |

```kotlin
class JournalUseCases(time: TimeSource, entries: EntryRepository, prompts: PromptLibrary,
                      promptState: PromptStateStore, settings: SettingsStore,
                      reminders: ReminderScheduler, widgets: WidgetRefresher) {
    val getTodayState: GetTodayStateUseCase
    // … one property per use case above
}
```

---

## 2. Data

```kotlin
object JournalEnvironmentFactory {
    fun create(context: Context, widgets: WidgetRefresher): JournalEnvironment   // production; app passes GlanceWidgetRefresher
    fun createInMemory(time: TimeSource, …fakes): JournalEnvironment     // tests and previews
}
/** Implemented by the Application, so the widget and receivers reach the shared environment without depending on :app. */
interface JournalEnvironmentProvider { val environment: JournalEnvironment }

class JournalEnvironment(val useCases: JournalUseCases, val clockEvents: ClockEvents,
                         val entryChanges: Flow<Unit>, val settingsChanges: Flow<Unit>, val promptStateChanges: Flow<Unit>)

class ClockEvents(context: Context, time: TimeSource) {
    val boundaries: Flow<Unit>          // next midnight, next 12:00, system time/date/zone changes
}

class BundledPromptLibrary(context: Context) : PromptLibrary      // loads assets/prompts.json, validates

class ReminderNotifier(context: Context) {
    fun ensureChannel()
    fun post(reminder: ScheduledReminder)
}
```

`DatabaseEntryRepository`, `PreferencesSettingsStore`, `PreferencesPromptStateStore`, `AndroidReminderScheduler` and `SystemTimeSource` implement the ports above with no extra public functions.

---

## 3. App

### 3.1 Container and entry points

```kotlin
class JournalApplication : Application(), JournalEnvironmentProvider {
    lateinit var container: AppContainer
    override val environment: JournalEnvironment   // the one built for the container
}
class AppContainer(environment: JournalEnvironment) {
    val useCases: JournalUseCases
    val refreshSignals: Flow<Unit>   // merge of entry, settings, prompt-state changes and clock boundaries
}

class MainActivity : ComponentActivity()          // onCreate / onNewIntent → DeepLinkRouter
object DeepLinkRouter { fun route(intent: Intent?, navController: NavController) }

class ReminderReceiver : BroadcastReceiver()      // onReceive: check done, notify, sync next reminders
class RescheduleReceiver : BroadcastReceiver()    // boot, time set, time zone changed, package replaced
```

### 3.2 ViewModels (state exposed as `StateFlow`, events as functions)

| ViewModel | State | Events |
|---|---|---|
| `TodayViewModel` | `TodayUiState(today: TodayState?, isEditing: Boolean, error: String?)` | `refreshPrompt()`, `startEditing()`, `finishEditing()`, `openBackfill()` |
| `EntryEditorViewModel` | `EditorUiState(text, status: Saved/Saving/Error, isLocked, lockedMessage, canFinish, wordCount)` | `onTextChange(text)`, `done()`, `onBack(): Boolean` |
| `BackfillViewModel` | `BackfillUiState(prompt, text, until, canSave, confirmDiscard)` | `onTextChange`, `save()`, `onBack()`, `discard()` |
| `HistoryViewModel` | `HistoryUiState(query, entries, isEmpty, noMatches)` | `onQueryChange(query)`, `onBack(): Boolean` |
| `EntryDetailViewModel` | `EntryDetailUiState(detail: EntryDetail?, missing: Boolean)` | (editing delegates to `EntryEditorViewModel`) |
| `SettingsViewModel` | `SettingsUiState(settings, categoryMessage, reminderAuthorization, exportAvailable)` | `toggleCategory(c)`, `setReminderEnabled(b)`, `setReminderTime(t)`, `onPermissionResult(granted)`, `refreshPermission()`, `export(format)` |
| `OnboardingViewModel` | `OnboardingUiState(step, categories, reminderOn, reminder, isFinishing, message)` | `toggleCategory`, `next()`, `back()`, `setReminder…`, `finish()` |

### 3.3 Navigation routes

```kotlin
@Serializable sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Today : Route
    @Serializable data object History : Route
    @Serializable data class Entry(val dayKey: String) : Route
    @Serializable data object Settings : Route
}
```

---

## 4. Widget

```kotlin
class PromptStreakWidget : GlanceAppWidget() { override suspend fun provideGlance(context: Context, id: GlanceId) }
class PromptStreakWidgetReceiver : GlanceAppWidgetReceiver()
object WidgetStateLoader { suspend fun load(context: Context): WidgetState }   // via JournalEnvironmentProvider; Ready(TodayState) or NeedsSetup
class GlanceWidgetRefresher(context: Context) : WidgetRefresher   // updates PromptStreakWidget
object WidgetUpdateScheduler { fun scheduleNextBoundaries(context: Context, now: Instant, timeZone: TimeZone) }
```
