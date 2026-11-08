package com.mmomo.dailyjournal.app

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.di.AppStartup
import com.mmomo.dailyjournal.app.di.StartState
import com.mmomo.dailyjournal.app.di.StartupFailedException
import com.mmomo.dailyjournal.data.composition.JournalEnvironmentFactory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.support.FakeReminderScheduler
import com.mmomo.dailyjournal.domain.support.FakeWidgetRefresher
import com.mmomo.dailyjournal.domain.support.FixedTimeSource
import com.mmomo.dailyjournal.domain.support.instant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Start states (App NFR design §2): ready, failed with a generic reason, retry. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class) // don't start the real app in tests
class AppStartupTest {
    private val directory: File = Files.createTempDirectory("prefs").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reminders = FakeReminderScheduler()

    @After fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    private fun container(): AppContainer {
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(directory, "p.preferences_pb") })
        val time = FixedTimeSource(instant("2026-10-08 09:00"))
        val env = JournalEnvironmentFactory.createInMemory(
            ApplicationProvider.getApplicationContext(), store, time, reminders, FakeWidgetRefresher(), io = Dispatchers.IO,
        )
        return AppContainer(env.useCases, time, env.refreshSignals, emptyFlow(), { ReminderAuthorization.ALLOWED }, {}, env)
    }

    private suspend fun AppStartup.settled() = withTimeout(10_000) { state.first { it !is StartState.Loading } }

    @Test
    fun `ready after creating the container and syncing reminders`() = runBlocking<Unit> {
        val startup = AppStartup({ container() })
        startup.begin()
        startup.settled().shouldBeInstanceOf<StartState.Ready>()
        startup.awaitContainer().useCases.getSettings.execute().onboardingCompleted shouldBe false
        reminders.replaceCalls shouldBe 1
    }

    @Test
    fun `a failure shows the error state, waiters get an exception, and retry recovers`() = runBlocking<Unit> {
        var attempts = 0
        val startup = AppStartup({ if (attempts++ == 0) throw IOException("disk with private path /data/x") else container() })
        startup.begin()
        val failed = startup.settled() as StartState.Failed
        failed.reason shouldBe "IOException" // class name only, no message text (APP-SEC-7)
        shouldThrow<StartupFailedException> { startup.awaitContainer() }
        startup.retry()
        startup.settled().shouldBeInstanceOf<StartState.Ready>()
    }
}
