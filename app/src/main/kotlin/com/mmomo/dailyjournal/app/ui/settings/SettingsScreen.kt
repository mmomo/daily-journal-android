package com.mmomo.dailyjournal.app.ui.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.export.ExportSharer
import com.mmomo.dailyjournal.app.navigation.ViewModels
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.PromptCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ViewModels.settings(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors
    val context = LocalContext.current
    var pickingTime by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermission() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onPermissionResult() }
    LaunchedEffect(state.requestPermission) { if (state.requestPermission) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(state.share) { state.share?.let { ExportSharer.share(context, it); vm.onShared() } }

    Scaffold(
        containerColor = colors.groupedBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.groupedBackground),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Prompt categories")
            JournalCard {
                PromptCategory.entries.forEach { category ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.rawValue.replaceFirstChar { it.uppercase() }, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = category in state.settings.categories,
                            onCheckedChange = { vm.toggleCategory(category) },
                            modifier = Modifier.testTag("settings-category-toggle-${category.rawValue}"),
                        )
                    }
                }
            }
            Footer(state.categoryMessage ?: "Changes apply to refreshes today and to prompts from tomorrow.", warning = state.categoryMessage != null)

            SectionTitle("Reminder")
            JournalCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily reminder", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.settings.reminder.isEnabled,
                        onCheckedChange = vm::setReminderEnabled,
                        enabled = !state.reminderUnavailable || state.settings.reminder.isEnabled,
                        modifier = Modifier.testTag("settings-reminder-toggle"),
                    )
                }
                if (state.settings.reminder.isEnabled) {
                    TextButton(onClick = { pickingTime = true }, modifier = Modifier.testTag("settings-reminder-time")) {
                        Text("Time  %02d:%02d".format(state.settings.reminder.hour, state.settings.reminder.minute), color = colors.accent)
                    }
                }
            }
            if (state.reminderUnavailable) {
                Footer("Notifications are off for this app.", warning = true)
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    modifier = Modifier.testTag("settings-open-notification-settings"),
                ) { Text("Open settings", color = colors.accent) }
            } else {
                Footer("Skipped automatically on days you've already written.")
            }

            SectionTitle("Export")
            JournalCard {
                ExportRow("Export as Markdown", "settings-export-markdown") { vm.export(ExportFormat.MARKDOWN) }
                ExportRow("Export as plain text", "settings-export-text") { vm.export(ExportFormat.PLAIN_TEXT) }
            }
            Footer(state.exportMessage ?: "Export is the only copy of your journal outside this phone.", warning = state.exportMessage != null)
        }
    }

    if (pickingTime) {
        val picker = rememberTimePickerState(state.settings.reminder.hour, state.settings.reminder.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = { TextButton(onClick = { vm.setReminderTime(picker.hour, picker.minute); pickingTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
            title = { Text("Reminder time") },
        ) { TimePicker(picker) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = JournalTheme.colors.secondaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
}

@Composable
private fun Footer(text: String, warning: Boolean = false) {
    Text(text, color = if (warning) JournalTheme.colors.warning else JournalTheme.colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun ExportRow(label: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Icon(painterResource(R.drawable.ic_share), contentDescription = null, tint = JournalTheme.colors.accent)
        Text("  $label", color = JournalTheme.colors.accent, fontSize = 17.sp, modifier = Modifier.weight(1f))
    }
}
