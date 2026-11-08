package com.mmomo.dailyjournal.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.TodayState
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Today screen (S1, S4, S5; US-1.x, US-2.x, US-4.x). */
class TodayViewModel(private val useCases: JournalUseCases, refreshSignals: Flow<Unit>) : ViewModel() {

    data class UiState(
        val today: TodayState? = null,
        /** The day of the entry being edited; kept after midnight so the locked editor stays until the user leaves (FR-8.1). */
        val editingDay: CalendarDay? = null,
        val message: String? = null,
        val showBackfill: Boolean = false,
        val focusRequests: Int = 0,
    ) {
        /** The editor is shown while editing, or while today isn't written yet. */
        val editorDay: CalendarDay?
            get() = editingDay ?: today?.takeIf { !it.isTodayDone && it.entryEditability.isEditable }?.day
    }

    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState
    private val reloads = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        // The first load is part of the collection, so it can't be lost before the collector subscribes.
        viewModelScope.launch { reloads.onStart { emit(Unit) }.collect { load() } }
        viewModelScope.launch { refreshSignals.collect { reloads.tryEmit(Unit) } }
    }

    fun reload() {
        reloads.tryEmit(Unit)
    }

    private suspend fun load() {
        try {
            val today = useCases.getTodayState.execute()
            mutableState.update { it.copy(today = today, message = null) }
        } catch (error: JournalError) {
            mutableState.update { it.copy(message = userMessage(error)) }
        }
    }

    fun refreshPrompt() {
        viewModelScope.launch {
            try {
                useCases.refreshPrompt.execute()
                load()
            } catch (error: JournalError) {
                mutableState.update { it.copy(message = userMessage(error)) }
            }
        }
    }

    /** After any save the editor stays on screen until Done (iOS keyboard fix). */
    fun onEntrySaved(day: CalendarDay) {
        mutableState.update { it.copy(editingDay = day) }
        reload()
    }

    fun startEditing() {
        mutableState.update { state -> state.copy(editingDay = state.today?.day, focusRequests = state.focusRequests + 1) }
    }

    fun finishEditing() {
        mutableState.update { it.copy(editingDay = null) }
        reload()
    }

    /** Widget or reminder tap: focus the editor if today isn't done (US-5.2). */
    fun requestFocus() {
        mutableState.update { state ->
            if (state.today?.isTodayDone == true) state else state.copy(focusRequests = state.focusRequests + 1)
        }
    }

    fun openBackfill() {
        mutableState.update { it.copy(showBackfill = it.today?.backfillOffer != null) }
    }

    fun closeBackfill() {
        mutableState.update { it.copy(showBackfill = false) }
        reload()
    }
}
