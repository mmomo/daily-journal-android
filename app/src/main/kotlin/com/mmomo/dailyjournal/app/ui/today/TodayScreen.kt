package com.mmomo.dailyjournal.app.ui.today

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.navigation.ViewModels
import com.mmomo.dailyjournal.app.ui.backfill.BackfillSheet
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.components.PromptCard
import com.mmomo.dailyjournal.app.ui.components.SmallPillButton
import com.mmomo.dailyjournal.app.ui.components.StreakCard
import com.mmomo.dailyjournal.app.ui.components.formatTime
import com.mmomo.dailyjournal.app.ui.editor.EntryEditor
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    container: AppContainer,
    linkRequests: Flow<DeepLink>,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: TodayViewModel = viewModel(factory = ViewModels.today(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        linkRequests.collect { link ->
            when (link) {
                DeepLink.TODAY_EDITOR -> vm.requestFocus()
                DeepLink.BACKFILL_EDITOR -> vm.openBackfill()
            }
        }
    }

    Scaffold(
        containerColor = colors.groupedBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.today?.day?.let(::longTitle) ?: "Today", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenHistory, modifier = Modifier.testTag("today-history-button")) {
                        Icon(painterResource(R.drawable.ic_book), contentDescription = "History")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("today-settings-button")) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.groupedBackground),
            )
        },
    ) { padding ->
        val today = state.today
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (today == null) {
                state.message?.let { Text(it, color = colors.warning) }
                return@Column
            }
            StreakCard(today.streak, container.time.timeZone)
            today.backfillOffer?.let { offer ->
                JournalCard(Modifier.clickable(onClick = vm::openBackfill).testTag("today-backfill-button")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = colors.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Write yesterday's entry", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Available until ${formatTime(offer.until, container.time.timeZone)}", color = colors.secondaryText, fontSize = 13.sp)
                        }
                        Text("›", color = colors.secondaryText, fontSize = 22.sp)
                    }
                }
            }
            PromptCard(today.prompt, today.refreshesRemaining, showsRefresh = !today.isTodayDone, onRefresh = vm::refreshPrompt)

            val editorDay = state.editorDay
            if (editorDay != null) {
                val initial = if (editorDay == today.day) today.entry?.text.orEmpty() else ""
                val editor = viewModel<com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel>(key = "editor-${editorDay.key}", factory = ViewModels.editor(container, editorDay, initial))
                editor.onSaved = { vm.onEntrySaved(editorDay) }
                BackHandler(enabled = true) { editor.leave { activity?.finish() } }
                EntryEditor(
                    viewModel = editor,
                    autoFocus = !today.isTodayDone,
                    focusRequests = state.focusRequests,
                    onFinished = vm::finishEditing,
                )
            } else {
                today.entry?.let { SavedEntryCard(it, editable = today.entryEditability.isEditable, onEdit = vm::startEditing) }
            }
            state.message?.let { Text(it, color = colors.warning, fontSize = 13.sp) }
        }
    }

    val offer = state.today?.backfillOffer
    if (state.showBackfill && offer != null) {
        val backfill = viewModel<com.mmomo.dailyjournal.app.ui.backfill.BackfillViewModel>(key = "backfill-${offer.day.key}", factory = ViewModels.backfill(container, offer))
        BackfillSheet(backfill, container.time.timeZone, onClose = vm::closeBackfill)
    }
}

@Composable
private fun SavedEntryCard(entry: JournalEntry, editable: Boolean, onEdit: () -> Unit) {
    val colors = JournalTheme.colors
    JournalCard(Modifier.testTag("today-saved-entry"), padding = 20) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.success, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("TODAY'S ENTRY", color = colors.success, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Spacer(Modifier.padding(top = 12.dp))
        Text(entry.text, fontSize = 17.sp, lineHeight = 24.sp)
        Spacer(Modifier.padding(top = 14.dp))
        if (editable) {
            SmallPillButton("Edit", onClick = onEdit, iconRes = R.drawable.ic_edit, modifier = Modifier.testTag("today-edit-button"))
        } else {
            Text("This entry is locked.", color = colors.secondaryText, fontSize = 13.sp)
        }
    }
}

private val longTitleFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

fun longTitle(day: com.mmomo.dailyjournal.domain.time.CalendarDay): String =
    longTitleFormat.format(LocalDate.of(day.year, day.month, day.day))
