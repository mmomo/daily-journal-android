package com.mmomo.dailyjournal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.ui.entry.EntryDetailScreen
import com.mmomo.dailyjournal.app.ui.history.HistoryScreen
import com.mmomo.dailyjournal.app.ui.onboarding.OnboardingScreen
import com.mmomo.dailyjournal.app.ui.settings.SettingsScreen
import com.mmomo.dailyjournal.app.ui.today.TodayScreen
import kotlinx.coroutines.flow.Flow

/** Screens and their routes. [showToday] emits when a deep link arrives, to return to Today from anywhere. */
@Composable
fun JournalNavHost(
    container: AppContainer,
    startOnboarded: Boolean,
    onboarded: Boolean,
    router: DeepLinkRouter,
    showToday: Flow<Unit>,
    onOnboardingFinished: () -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    val isOnboarded by rememberUpdatedState(onboarded)
    LaunchedEffect(nav) {
        showToday.collect {
            if (isOnboarded && nav.currentDestination?.hasRoute<TodayRoute>() != true) {
                nav.navigate(TodayRoute) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
            }
        }
    }
    NavHost(nav, startDestination = if (startOnboarded) TodayRoute else OnboardingRoute) {
        composable<OnboardingRoute> {
            OnboardingScreen(container) {
                onOnboardingFinished()
                nav.navigate(TodayRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
            }
        }
        composable<TodayRoute> {
            TodayScreen(
                container,
                linkRequests = router.requests,
                onOpenHistory = { nav.navigate(HistoryRoute) { launchSingleTop = true } },
                onOpenSettings = { nav.navigate(SettingsRoute) { launchSingleTop = true } },
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(container, onOpenEntry = { nav.navigate(EntryRoute(it.key)) }, onBack = { nav.popBackStack() })
        }
        composable<EntryRoute> { entry ->
            EntryDetailScreen(container, entry.toRoute<EntryRoute>().dayKey, onBack = { nav.popBackStack() })
        }
        composable<SettingsRoute> { SettingsScreen(container, onBack = { nav.popBackStack() }) }
    }
}
