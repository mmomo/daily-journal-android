package com.mmomo.dailyjournal.data.support

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.mmomo.dailyjournal.data.database.DatabaseEntryRepository
import com.mmomo.dailyjournal.data.database.JournalDatabase
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.support.instant
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.nio.file.Files

fun appContext(): Context = ApplicationProvider.getApplicationContext()

/** A DataStore on a fresh temporary file; call [close] when done. */
class TempPreferences : AutoCloseable {
    private val directory: File = Files.createTempDirectory("prefs").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = File(directory, "journal_preferences.preferences_pb")
    val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    override fun close() {
        scope.cancel()
        directory.deleteRecursively()
    }
}

/** An in-memory Room database plus its repository. */
class TempDatabase : AutoCloseable {
    val database: JournalDatabase = JournalDatabase.inMemory(appContext())
    val repository = DatabaseEntryRepository(database, Dispatchers.IO)
    override fun close() = database.close()
}

fun entry(
    dayKey: String,
    text: String = "Some thoughts",
    id: String = "id-$dayKey",
    promptId: String = "gratitude-001",
): JournalEntry {
    val at = instant("$dayKey 21:00")
    return JournalEntry(id, CalendarDay.fromKey(dayKey)!!, text, promptId, "What small thing made today better?", at, at)
}

/** Stand-ins for the App unit's receiver and activity (the data unit only needs their classes). */
class TestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

class TestMainActivity : Activity()

/** Values produced by the iOS code (run once, read-only), for format parity. */
object IosDataFixtures {
    const val SETTINGS_JSON =
        """{"categories":["goals","gratitude"],"onboardingCompleted":true,"reminder":{"hour":7,"isEnabled":true,"minute":30}}"""
    const val PROMPT_STATE_JSON =
        """{"2026-10-08":{"categoriesKey":"creativity,goals,gratitude,reflection","day":{"day":8,"month":10,"year":2026},"promptID":"reflection-098","refreshesUsed":1,"shownPromptIDs":["reflection-098","reflection-125"]}}"""

    /** SHA-256 of the iOS `prompts.json` this copy was taken from. */
    const val PROMPTS_SHA256 = "660442137f6bdd49b3d02def33f02645bc469e19479f524a11c0e0bd668d3395"
}
