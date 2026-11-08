package com.mmomo.dailyjournal.app

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.mmomo.dailyjournal.app.di.AppStartup
import com.mmomo.dailyjournal.data.composition.JournalEnvironment
import com.mmomo.dailyjournal.data.composition.JournalEnvironmentProvider

/** Starts the app: error handler, StrictMode in debug, and background creation of the environment (App NFR design §1). */
class JournalApplication : Application(), JournalEnvironmentProvider {
    lateinit var startup: AppStartup
        private set

    override fun onCreate() {
        super.onCreate()
        installErrorHandler()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().build())
        }
        startup = AppStartup(this)
        startup.begin()
    }

    override suspend fun awaitEnvironment(): JournalEnvironment = startup.awaitContainer().environment

    /** Logs only the exception class (never journal text), then lets the system handle the crash (APP-SEC-6). */
    private fun installErrorHandler() {
        val system = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Log.e(TAG, "Uncaught exception: ${error::class.java.name}")
            system?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val TAG = "DailyJournal"
    }
}
