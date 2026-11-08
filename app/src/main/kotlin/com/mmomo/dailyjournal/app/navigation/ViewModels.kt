package com.mmomo.dailyjournal.app.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.ui.backfill.BackfillViewModel
import com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel
import com.mmomo.dailyjournal.app.ui.entry.EntryDetailViewModel
import com.mmomo.dailyjournal.app.ui.history.HistoryViewModel
import com.mmomo.dailyjournal.app.ui.onboarding.OnboardingViewModel
import com.mmomo.dailyjournal.app.ui.settings.SettingsViewModel
import com.mmomo.dailyjournal.app.ui.today.TodayViewModel
import com.mmomo.dailyjournal.domain.entities.BackfillOffer
import com.mmomo.dailyjournal.domain.time.CalendarDay

/** ViewModel factories over the [AppContainer] (manual injection, application design Q4). */
object ViewModels {
    fun today(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { TodayViewModel(c.useCases, c.refreshSignals) }
    }

    fun editor(c: AppContainer, day: CalendarDay, initialText: String): ViewModelProvider.Factory = viewModelFactory {
        initializer { EntryEditorViewModel(c.useCases, c.time, c.clockBoundaries, day, initialText, createSavedStateHandle()) }
    }

    fun backfill(c: AppContainer, offer: BackfillOffer): ViewModelProvider.Factory = viewModelFactory {
        initializer { BackfillViewModel(c.useCases, offer) }
    }

    fun history(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { HistoryViewModel(c.useCases, c.refreshSignals) }
    }

    fun entry(c: AppContainer, dayKey: String): ViewModelProvider.Factory = viewModelFactory {
        initializer { EntryDetailViewModel(c.useCases, dayKey, c.refreshSignals) }
    }

    fun settings(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(c.useCases, c.authorization, c.markPermissionAsked) }
    }

    fun onboarding(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { OnboardingViewModel(c.useCases, c.authorization, c.markPermissionAsked, createSavedStateHandle()) }
    }
}
