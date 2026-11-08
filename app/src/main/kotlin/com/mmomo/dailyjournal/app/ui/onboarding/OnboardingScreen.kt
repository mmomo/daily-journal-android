package com.mmomo.dailyjournal.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.navigation.ViewModels
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.components.PrimaryButton
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.entities.PromptCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(container: AppContainer, onFinished: () -> Unit) {
    val vm: OnboardingViewModel = viewModel(factory = ViewModels.onboarding(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors

    BackHandler(enabled = state.step == OnboardingViewModel.Step.REMINDER) { vm.back() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onPermissionResult() }
    LaunchedEffect(state.requestPermission) { if (state.requestPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    Column(
        Modifier.fillMaxSize().background(colors.groupedBackground).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        when (state.step) {
            OnboardingViewModel.Step.CATEGORIES -> {
                Text("Welcome to Daily Journal", fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("One prompt a day. Pick the kinds of prompts you'd like.", color = colors.secondaryText, fontSize = 17.sp)
                JournalCard {
                    PromptCategory.entries.forEach { category ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = category in state.categories,
                                onCheckedChange = { vm.toggle(category) },
                                modifier = Modifier.testTag("onboarding-category-${category.rawValue}"),
                            )
                            Text(category.rawValue.replaceFirstChar { it.uppercase() }, fontSize = 17.sp)
                        }
                    }
                }
                state.message?.let { Text(it, color = colors.warning, fontSize = 13.sp) }
                PrimaryButton("Continue", onClick = vm::next, enabled = state.canContinue, modifier = Modifier.testTag("onboarding-continue"))
            }
            OnboardingViewModel.Step.REMINDER -> {
                Text("A daily reminder?", fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("We'll remind you once a day, and skip days you've already written.", color = colors.secondaryText, fontSize = 17.sp)
                val picker = rememberTimePickerState(state.hour, state.minute, is24Hour = true)
                LaunchedEffect(picker.hour, picker.minute) { vm.setTime(picker.hour, picker.minute) }
                JournalCard { TimeInput(picker) }
                state.message?.let { Text(it, color = colors.warning, fontSize = 13.sp) }
                PrimaryButton("Turn on reminder", onClick = vm::enableReminder, enabled = !state.busy, modifier = Modifier.testTag("onboarding-enable-reminder"))
                TextButton(onClick = vm::skipReminder, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("onboarding-skip-reminder")) {
                    Text("Not now, start journaling", color = colors.accent, fontSize = 17.sp)
                }
            }
        }
    }
}
