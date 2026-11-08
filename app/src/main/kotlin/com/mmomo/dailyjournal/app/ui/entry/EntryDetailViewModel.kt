package com.mmomo.dailyjournal.app.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.domain.entities.EntryDetail
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/** One entry, read-only unless it's today's (US-2.4). [dayKey] comes from the route and is validated. */
class EntryDetailViewModel(private val useCases: JournalUseCases, dayKey: String, refreshSignals: Flow<Unit>) : ViewModel() {
    sealed interface UiState {
        data object Loading : UiState
        data object Missing : UiState
        data class Loaded(val detail: EntryDetail) : UiState
    }

    val day: CalendarDay? = CalendarDay.fromKey(dayKey) // invalid route arguments are rejected (NFR-6.3)
    private val mutableState = MutableStateFlow<UiState>(if (day == null) UiState.Missing else UiState.Loading)
    val state: StateFlow<UiState> = mutableState

    init {
        if (day != null) {
            viewModelScope.launch {
                refreshSignals.onStart { emit(Unit) }.collect {
                    mutableState.value = try {
                        useCases.getEntry.execute(day)?.let { UiState.Loaded(it) } ?: UiState.Missing
                    } catch (_: JournalError) {
                        UiState.Missing
                    }
                }
            }
        }
    }
}
