package com.mmomo.dailyjournal.app.navigation

import kotlinx.serialization.Serializable

/** Typed navigation routes. [Entry.dayKey] is validated by the screen (`CalendarDay.fromKey`). */
@Serializable data object OnboardingRoute
@Serializable data object TodayRoute
@Serializable data object HistoryRoute
@Serializable data class EntryRoute(val dayKey: String)
@Serializable data object SettingsRoute
