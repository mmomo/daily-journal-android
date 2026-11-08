package com.mmomo.dailyjournal.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.time.CalendarDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * "The day may have changed" signal for screens (DATA-C2, DATA-C3, NFR design §4): emits at the next
 * midnight or 12:00, on system time, date or time-zone changes, and when the app returns to the
 * foreground. Listens only while collected.
 */
class ClockEvents(private val context: Context, private val time: TimeSource) {

    val boundaries: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val foreground = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                trySend(Unit)
            }
        }
        // Lifecycle observers must be added and removed on the main thread.
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val mainThread = Handler(Looper.getMainLooper())
        mainThread.post { lifecycle.addObserver(foreground) }

        val timer = launch {
            while (isActive) {
                val now = time.now
                delay(nextBoundary(now, time.timeZone) - now)
                trySend(Unit)
            }
        }

        awaitClose {
            timer.cancel()
            context.unregisterReceiver(receiver)
            mainThread.post { lifecycle.removeObserver(foreground) }
        }
    }.conflate()

    companion object {
        /** The next local midnight or 12:00 strictly after [now] in [timeZone]. */
        fun nextBoundary(now: Instant, timeZone: TimeZone): Instant {
            val today = CalendarDay.containing(now, timeZone)
            val noon = today.noonIn(timeZone)
            return if (now < noon) noon else today.adding(1).startIn(timeZone)
        }
    }
}
