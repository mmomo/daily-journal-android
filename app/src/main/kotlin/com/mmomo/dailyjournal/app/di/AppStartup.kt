package com.mmomo.dailyjournal.app.di

import android.content.Context
import android.util.Log
import com.mmomo.dailyjournal.app.JournalApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the app can show while starting (App NFR design §2). */
sealed interface StartState {
    data object Loading : StartState
    data class Ready(val container: AppContainer) : StartState
    data class Failed(val reason: String) : StartState
}

/** Thrown to receivers and the widget when the environment couldn't be created. */
class StartupFailedException(reason: String) : Exception("App startup failed: $reason")

/**
 * Creates the environment on a background thread as soon as the process starts; everything that needs it
 * waits (Q1:A). `create` is replaceable for tests.
 */
class AppStartup(
    private val create: suspend () -> AppContainer,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    constructor(context: Context) : this({ AppContainer.create(context.applicationContext) })

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow<StartState>(StartState.Loading)
    val state: StateFlow<StartState> = mutableState

    fun begin() {
        mutableState.value = StartState.Loading
        scope.launch {
            mutableState.value = try {
                val container = create()
                container.useCases.syncReminderWithToday.execute() // reminders recomputed on app start (APP-REL-5)
                StartState.Ready(container)
            } catch (error: Exception) {
                Log.e(JournalApplication.TAG, "Startup failed: ${error::class.java.name}")
                StartState.Failed(error::class.java.simpleName)
            }
        }
    }

    fun retry() {
        if (mutableState.value is StartState.Failed) begin()
    }

    suspend fun awaitContainer(): AppContainer =
        when (val result = state.first { it !is StartState.Loading }) {
            is StartState.Ready -> result.container
            is StartState.Failed -> throw StartupFailedException(result.reason)
            StartState.Loading -> error("unreachable")
        }
}
