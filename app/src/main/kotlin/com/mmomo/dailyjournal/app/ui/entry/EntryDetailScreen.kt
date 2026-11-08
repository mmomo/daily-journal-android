package com.mmomo.dailyjournal.app.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
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
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.editor.EntryEditor
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(container: AppContainer, dayKey: String, onBack: () -> Unit) {
    val vm: EntryDetailViewModel = viewModel(key = "entry-$dayKey", factory = ViewModels.entry(container, dayKey))
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors

    Scaffold(
        containerColor = colors.groupedBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(vm.day?.key ?: "Entry", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.groupedBackground),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                EntryDetailViewModel.UiState.Loading -> Unit
                EntryDetailViewModel.UiState.Missing -> Text("Entry not found", fontSize = 17.sp, modifier = Modifier.testTag("entry-missing"))
                is EntryDetailViewModel.UiState.Loaded -> {
                    val entry = s.detail.entry
                    Text(entry.promptText, style = JournalTheme.promptStyle)
                    if (s.detail.editability.isEditable) {
                        val editor = viewModel<com.mmomo.dailyjournal.app.ui.editor.EntryEditorViewModel>(key = "editor-${entry.day.key}", factory = ViewModels.editor(container, entry.day, entry.text))
                        BackHandler { editor.leave(onBack) }
                        EntryEditor(editor, autoFocus = false, focusRequests = 0, onFinished = onBack)
                    } else {
                        JournalCard(padding = 20) { Text(entry.text, fontSize = 17.sp, lineHeight = 24.sp) }
                        Text("Past entries are read-only.", color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.testTag("entry-read-only"))
                    }
                }
            }
        }
    }
}
