package com.mmomo.dailyjournal.app.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.di.AppContainer
import com.mmomo.dailyjournal.app.navigation.ViewModels
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.time.CalendarDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(container: AppContainer, onOpenEntry: (CalendarDay) -> Unit, onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel(factory = ViewModels.history(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors
    BackHandler(enabled = state.query.isNotEmpty()) { vm.onBack() }

    Scaffold(
        containerColor = colors.groupedBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { if (!vm.onBack()) onBack() }) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.groupedBackground),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("history-list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::onQueryChange,
                        placeholder = { Text("Search entries and prompts") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("history-search-field"),
                    )
                }
                items(state.entries, key = { it.id }) { entry ->
                    JournalCard(Modifier.clickable { onOpenEntry(entry.day) }.testTag("history-row-${entry.day.key}")) {
                        Text(entry.day.key, color = colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(entry.promptText, style = JournalTheme.promptStyleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.text, color = colors.secondaryText, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            when {
                state.isEmptyJournal -> EmptyState("No entries yet", "Today's prompt is waiting for you.")
                state.noMatches -> EmptyState("No entries match", "Try a different word.")
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(body, color = JournalTheme.colors.secondaryText)
        }
    }
}
