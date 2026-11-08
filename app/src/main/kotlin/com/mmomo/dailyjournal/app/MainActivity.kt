package com.mmomo.dailyjournal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.di.StartState
import com.mmomo.dailyjournal.app.navigation.DeepLinkRouter
import com.mmomo.dailyjournal.app.navigation.JournalNavHost
import com.mmomo.dailyjournal.app.ui.components.PrimaryButton
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.entities.DeepLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** The single activity: start states, navigation, and deep links from the widget and reminders (App NFR design §2, §7). */
class MainActivity : ComponentActivity() {
    private val router = DeepLinkRouter()
    private val showToday = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity (rotation) already handled its launch link.
        if (savedInstanceState == null) handle(intent)
        val startup = (application as JournalApplication).startup
        setContent {
            JournalTheme {
                val state by startup.state.collectAsStateWithLifecycle()
                when (val s = state) {
                    StartState.Loading -> Box(Modifier.fillMaxSize().background(JournalTheme.colors.groupedBackground))
                    is StartState.Failed -> StartupError(onRetry = startup::retry)
                    is StartState.Ready -> Ready(s.container)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (router.onLink(DeepLink.parse(intent?.dataString))) showToday.trySend(Unit)
    }

    @Composable
    private fun Ready(container: AppContainer) {
        val onboardedAtStart by produceState<Boolean?>(null, container) {
            value = container.useCases.getSettings.execute().onboardingCompleted
        }
        val startOnboarded = onboardedAtStart ?: return // first frame stays the plain background
        var onboarded by remember { mutableStateOf(startOnboarded) }
        LaunchedEffect(onboarded) { router.setReady(onboarded) }
        JournalNavHost(
            container,
            startOnboarded = startOnboarded,
            onboarded = onboarded,
            router = router,
            showToday = remember { showToday.receiveAsFlow() },
            onOnboardingFinished = { onboarded = true },
        )
    }
}

/** Startup failed (storage couldn't open or the prompt file is broken); generic text only (APP-SEC-7). */
@Composable
private fun StartupError(onRetry: () -> Unit) {
    val colors = JournalTheme.colors
    Box(Modifier.fillMaxSize().background(colors.groupedBackground).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Daily Journal couldn't start", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            Text("Your journal is safe. Try again, or restart your phone if this keeps happening.", color = colors.secondaryText, textAlign = TextAlign.Center)
            PrimaryButton("Try again", onClick = onRetry, modifier = Modifier.testTag("startup-retry"))
        }
    }
}
