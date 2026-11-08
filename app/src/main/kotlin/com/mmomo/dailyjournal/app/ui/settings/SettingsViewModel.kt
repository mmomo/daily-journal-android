package com.mmomo.dailyjournal.app.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.ExportDocument
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Settings (S7; US-1.4, US-6.1, US-3.3, US-7.2). The permission state is re-read every time Settings is shown (FR-6.3). */
class SettingsViewModel(
    private val useCases: JournalUseCases,
    private val authorization: suspend () -> ReminderAuthorization,
    private val markPermissionAsked: suspend () -> Unit,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val authorization: ReminderAuthorization = ReminderAuthorization.ALLOWED,
        val categoryMessage: String? = null,
        val exportMessage: String? = null,
        val requestPermission: Boolean = false,
        val share: ExportDocument? = null,
    ) {
        val reminderUnavailable: Boolean get() = authorization == ReminderAuthorization.DENIED
    }

    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState

    init {
        viewModelScope.launch {
            mutableState.update { it.copy(settings = useCases.getSettings.execute()) }
            refreshPermission()
        }
    }

    /** Called on every resume (returning from system settings updates the state). */
    fun refreshPermission() {
        viewModelScope.launch { mutableState.update { it.copy(authorization = authorization()) } }
    }

    fun toggleCategory(category: PromptCategory) {
        viewModelScope.launch {
            val current = mutableState.value.settings.categories
            val updated = if (category in current) current - category else current + category
            try {
                useCases.updatePromptCategories.execute(updated)
                mutableState.update { it.copy(settings = it.settings.copy(categories = updated), categoryMessage = null) }
            } catch (error: JournalError.CategoriesEmpty) {
                mutableState.update { it.copy(categoryMessage = userMessage(error)) }
            }
        }
    }

    fun setReminderEnabled(on: Boolean) {
        viewModelScope.launch {
            if (on && sdkInt >= 33 && authorization() == ReminderAuthorization.NOT_DETERMINED) {
                mutableState.update { it.copy(requestPermission = true) }
            } else {
                configure(mutableState.value.settings.reminder.copy(isEnabled = on))
            }
        }
    }

    /** The system permission dialog closed (Android 13+). */
    fun onPermissionResult() {
        viewModelScope.launch {
            markPermissionAsked()
            mutableState.update { it.copy(requestPermission = false) }
            configure(mutableState.value.settings.reminder.copy(isEnabled = true))
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { configure(mutableState.value.settings.reminder.copy(hour = hour, minute = minute)) }
    }

    private suspend fun configure(reminder: ReminderSettings) {
        try {
            useCases.configureReminder.execute(reminder)
        } catch (_: JournalError.NotificationsDenied) {
            // Saved as off by the use case; the screen shows the unavailable state.
        }
        mutableState.update { it.copy(settings = useCases.getSettings.execute(), authorization = authorization()) }
    }

    fun export(format: ExportFormat) {
        viewModelScope.launch {
            try {
                mutableState.update { it.copy(share = useCases.exportJournal.execute(format), exportMessage = null) }
            } catch (error: JournalError) {
                mutableState.update { it.copy(exportMessage = userMessage(error)) }
            }
        }
    }

    fun onShared() = mutableState.update { it.copy(share = null) }
}
