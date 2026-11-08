package com.mmomo.dailyjournal.app.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.ports.TimeSource
import com.mmomo.dailyjournal.domain.time.CalendarDay
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Today's entry editor (US-2.1, US-2.2, US-2.5; App NFR design §4). Typed text and its edit time live in
 * [SavedStateHandle], so they survive rotation and process death.
 */
class EntryEditorViewModel(
    private val useCases: JournalUseCases,
    private val time: TimeSource,
    clockBoundaries: Flow<Unit>,
    val day: CalendarDay,
    initialText: String,
    private val savedState: SavedStateHandle,
    private val autosaveDelay: Duration = 800.milliseconds,
) : ViewModel() {

    enum class Status { IDLE, SAVING, SAVED, ERROR }

    data class UiState(
        val text: String,
        val status: Status = Status.IDLE,
        val isLocked: Boolean = false,
        val message: String? = null,
    ) {
        val canFinish: Boolean get() = !isLocked && JournalEntry.isValidText(text)
        val wordCount: Int get() = text.split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    private val mutableState = MutableStateFlow(UiState(text = savedState[KEY_TEXT] ?: initialText))
    val state: StateFlow<UiState> = mutableState

    private var lastSaved: String = savedState[KEY_SAVED] ?: initialText
    private var editedAt: Instant = savedState.get<Long>(KEY_EDITED_AT)?.let { Instant.fromEpochMilliseconds(it) } ?: time.now
    private var autosaveJob: Job? = null

    /** Called after every successful save (Today keeps the editor on screen; iOS fix). */
    var onSaved: () -> Unit = {}

    init {
        viewModelScope.launch { clockBoundaries.collect { onClockBoundary() } }
        if (CalendarDay.containing(time.now, time.timeZone) != day) lock()
    }

    val isDirty: Boolean get() = mutableState.value.text != lastSaved

    /** Returns false if the change was rejected (locked, or over the 20,000-character limit; C3:B, C4:B). */
    fun onTextChange(text: String): Boolean {
        if (mutableState.value.isLocked) return false
        if (!JournalEntry.isWithinLimit(text)) {
            mutableState.update { it.copy(message = MESSAGE_LIMIT) }
            return false
        }
        editedAt = time.now
        savedState[KEY_TEXT] = text
        savedState[KEY_EDITED_AT] = editedAt.toEpochMilliseconds()
        mutableState.update { it.copy(text = text, message = null, status = if (it.status == Status.SAVED) Status.IDLE else it.status) }
        scheduleAutosave()
        return true
    }

    /** Done: save now; [onFinished] runs only if the save worked. */
    fun done(onFinished: () -> Unit) {
        autosaveJob?.cancel()
        viewModelScope.launch { if (saveNow()) onFinished() }
    }

    /** Back: save unsaved text first, then leave; stay if the save fails (Back never discards text). */
    fun leave(onLeave: () -> Unit) {
        autosaveJob?.cancel()
        viewModelScope.launch {
            val ok = if (isDirty && JournalEntry.isValidText(mutableState.value.text)) saveNow() else true
            if (ok) onLeave()
        }
    }

    /** Midnight or another boundary: flush pending text with its edit time, then lock if the day is over (FR-8.1). */
    suspend fun onClockBoundary() {
        autosaveJob?.cancel()
        if (isDirty && JournalEntry.isValidText(mutableState.value.text)) saveNow()
        if (CalendarDay.containing(time.now, time.timeZone) != day) lock()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        if (!JournalEntry.isValidText(mutableState.value.text)) return
        autosaveJob = viewModelScope.launch {
            delay(autosaveDelay)
            saveNow()
        }
    }

    /**
     * Saves the current text. It never cancels the autosave job itself: the autosave runs inside that job,
     * and cancelling it here would stop its own save at the first suspension (callers cancel it instead).
     */
    private suspend fun saveNow(): Boolean {
        val snapshot = mutableState.value.text
        if (mutableState.value.isLocked || !JournalEntry.isValidText(snapshot)) return false
        mutableState.update { it.copy(status = Status.SAVING) }
        return try {
            useCases.saveTodayEntry.execute(snapshot, day, editedAt)
            lastSaved = snapshot
            savedState[KEY_SAVED] = snapshot
            mutableState.update { it.copy(status = Status.SAVED, message = null) }
            onSaved()
            true
        } catch (error: JournalError.EntryLocked) {
            lock()
            false
        } catch (error: JournalError) {
            mutableState.update { it.copy(status = Status.ERROR, message = userMessage(error)) }
            false
        }
    }

    private fun lock() {
        autosaveJob?.cancel()
        mutableState.update { it.copy(isLocked = true, message = MESSAGE_LOCKED) }
    }

    companion object {
        const val MESSAGE_LIMIT = "You've reached the maximum length for one entry."
        const val MESSAGE_LOCKED = "This entry is now locked."
        private const val KEY_TEXT = "editor.text"
        private const val KEY_SAVED = "editor.saved"
        private const val KEY_EDITED_AT = "editor.editedAt"
    }
}
