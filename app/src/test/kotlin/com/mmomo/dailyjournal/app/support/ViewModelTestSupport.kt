package com.mmomo.dailyjournal.app.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Runs [body] with `Dispatchers.Main` (and so `viewModelScope`) on the test's virtual-time scheduler. */
@OptIn(ExperimentalCoroutinesApi::class)
fun vmTest(body: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        body()
    } finally {
        advanceUntilIdle() // let pending autosaves finish while Main is still the test dispatcher
        Dispatchers.resetMain()
    }
}

/** A signal the test emits by hand (refresh signals, clock boundaries). */
fun signal() = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
