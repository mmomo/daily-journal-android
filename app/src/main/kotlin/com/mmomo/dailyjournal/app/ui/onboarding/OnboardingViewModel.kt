package com.mmomo.dailyjournal.app.ui.onboarding

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmomo.dailyjournal.app.ui.components.userMessage
import com.mmomo.dailyjournal.domain.entities.JournalError
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.entities.ReminderSettings
import com.mmomo.dailyjournal.domain.usecases.JournalUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Two-step first-launch setup (US-7.1). The step and choices survive rotation and process death. */
class OnboardingViewModel(
    private val useCases: JournalUseCases,
    private val authorization: suspend () -> ReminderAuthorization,
    private val markPermissionAsked: suspend () -> Unit,
    private val savedState: SavedStateHandle,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : ViewModel() {

    enum class Step { CATEGORIES, REMINDER }

    data class UiState(
        val step: Step = Step.CATEGORIES,
        val categories: Set<PromptCategory> = PromptCategory.entries.toSet(),
        val hour: Int = ReminderSettings.DEFAULT.hour,
        val minute: Int = ReminderSettings.DEFAULT.minute,
        val requestPermission: Boolean = false,
        val busy: Boolean = false,
        val finished: Boolean = false,
        val message: String? = null,
    ) {
        val canContinue: Boolean get() = categories.isNotEmpty()
    }

    private val mutableState = MutableStateFlow(
        UiState(
            step = savedState.get<String>(KEY_STEP)?.let(Step::valueOf) ?: Step.CATEGORIES,
            categories = savedState.get<List<String>>(KEY_CATEGORIES)?.mapNotNull(PromptCategory::fromRawValue)?.toSet()
                ?: PromptCategory.entries.toSet(),
            hour = savedState[KEY_HOUR] ?: ReminderSettings.DEFAULT.hour,
            minute = savedState[KEY_MINUTE] ?: ReminderSettings.DEFAULT.minute,
        ),
    )
    val state: StateFlow<UiState> = mutableState

    private fun set(transform: (UiState) -> UiState) {
        mutableState.update(transform)
        val s = mutableState.value
        savedState[KEY_STEP] = s.step.name
        savedState[KEY_CATEGORIES] = s.categories.map { it.rawValue }
        savedState[KEY_HOUR] = s.hour
        savedState[KEY_MINUTE] = s.minute
    }

    fun toggle(category: PromptCategory) = set {
        val updated = if (category in it.categories) it.categories - category else it.categories + category
        it.copy(categories = updated, message = null)
    }

    fun next() = set {
        if (it.canContinue) it.copy(step = Step.REMINDER, message = null) else it.copy(message = userMessage(JournalError.CategoriesEmpty()))
    }

    /** Back on the reminder step returns to categories; on the first step the activity handles Back (leaves the app). */
    fun back(): Boolean {
        if (mutableState.value.step == Step.CATEGORIES) return false
        set { it.copy(step = Step.CATEGORIES) }
        return true
    }

    fun setTime(hour: Int, minute: Int) = set { it.copy(hour = hour, minute = minute) }

    /** "Not now, start journaling": no permission prompt (US-6.1 "No early prompt"). */
    fun skipReminder() = finish(reminder = null)

    fun enableReminder() {
        viewModelScope.launch {
            if (sdkInt >= 33 && authorization() == ReminderAuthorization.NOT_DETERMINED) {
                set { it.copy(requestPermission = true) }
            } else {
                finish(ReminderSettings(isEnabled = true, hour = mutableState.value.hour, minute = mutableState.value.minute))
            }
        }
    }

    fun onPermissionResult() {
        viewModelScope.launch {
            markPermissionAsked()
            set { it.copy(requestPermission = false) }
            finish(ReminderSettings(isEnabled = true, hour = mutableState.value.hour, minute = mutableState.value.minute))
        }
    }

    private fun finish(reminder: ReminderSettings?) {
        if (mutableState.value.busy) return
        set { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                useCases.completeOnboarding.execute(mutableState.value.categories, reminder)
                set { it.copy(busy = false, finished = true) }
            } catch (_: JournalError.NotificationsDenied) {
                set { it.copy(busy = false, finished = true) } // onboarding still completes; Settings shows the state
            } catch (error: JournalError) {
                set { it.copy(busy = false, message = userMessage(error)) }
            }
        }
    }

    private companion object {
        const val KEY_STEP = "onboarding.step"
        const val KEY_CATEGORIES = "onboarding.categories"
        const val KEY_HOUR = "onboarding.hour"
        const val KEY_MINUTE = "onboarding.minute"
    }
}
