package com.mmomo.dailyjournal.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** History with search (S6; US-3.1, US-3.2). Back clears the search first. */
@OptIn(FlowPreview::class)
class HistoryViewModel(private val useCases: JournalUseCases, refreshSignals: Flow<Unit>) : ViewModel() {

    data class UiState(val query: String = "", val entries: List<JournalEntry> = emptyList(), val loaded: Boolean = false, val message: String? = null) {
        val isEmptyJournal: Boolean get() = loaded && entries.isEmpty() && query.isBlank()
        val noMatches: Boolean get() = loaded && entries.isEmpty() && query.isNotBlank()
    }

    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(query.debounce(250), refreshSignals.onStart { emit(Unit) }) { q, _ -> q }.collect { load(it) }
        }
    }

    fun onQueryChange(text: String) {
        mutableState.update { it.copy(query = text) }
        query.value = text
    }

    /** Returns true if Back was handled here (a search was cleared). */
    fun onBack(): Boolean {
        if (mutableState.value.query.isEmpty()) return false
        onQueryChange("")
        return true
    }

    private suspend fun load(q: String) {
        try {
            val entries = if (q.isBlank()) useCases.listEntries.execute() else useCases.searchEntries.execute(q)
            mutableState.update { it.copy(entries = entries, loaded = true, message = null) }
        } catch (error: JournalError) {
            mutableState.update { it.copy(loaded = true, message = userMessage(error)) }
        }
    }
}
