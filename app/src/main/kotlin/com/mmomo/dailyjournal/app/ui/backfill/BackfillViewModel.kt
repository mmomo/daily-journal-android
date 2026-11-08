package com.mmomo.dailyjournal.app.ui.backfill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel
import com.mmomo.dailyjournal.domain.entities.BackfillOffer
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Yesterday's missing entry before 12:00 (S5, US-2.3). Back with text asks before discarding. */
class BackfillViewModel(private val useCases: JournalUseCases, val offer: BackfillOffer) : ViewModel() {

    data class UiState(
        val text: String = "",
        val confirmDiscard: Boolean = false,
        val closed: Boolean = false,
        val windowClosed: Boolean = false,
        val message: String? = null,
    ) {
        val canSave: Boolean get() = !windowClosed && JournalEntry.isValidText(text)
    }

    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState

    fun onTextChange(text: String): Boolean {
        if (!JournalEntry.isWithinLimit(text)) {
            mutableState.update { it.copy(message = EntryEditorViewModel.MESSAGE_LIMIT) }
            return false
        }
        mutableState.update { it.copy(text = text, message = null) }
        return true
    }

    fun save() {
        viewModelScope.launch {
            try {
                useCases.backfillYesterday.execute(mutableState.value.text, offer.day)
                mutableState.update { it.copy(closed = true) }
            } catch (error: JournalError.BackfillNotAvailable) {
                mutableState.update { it.copy(windowClosed = true, message = userMessage(error)) }
            } catch (error: JournalError) {
                mutableState.update { it.copy(message = userMessage(error)) }
            }
        }
    }

    /** Back: close if nothing typed, otherwise ask (US-2.3 Back scenario). */
    fun onBack() {
        mutableState.update { if (JournalEntry.isValidText(it.text)) it.copy(confirmDiscard = true) else it.copy(closed = true) }
    }

    fun keepWriting() = mutableState.update { it.copy(confirmDiscard = false) }
    fun discard() = mutableState.update { it.copy(confirmDiscard = false, closed = true) }
}
