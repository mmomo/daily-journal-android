package com.mmomo.dailyjournal.app.ui.backfill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.components.PrimaryButton
import com.mmomo.dailyjournal.app.ui.components.formatTime
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import kotlinx.datetime.TimeZone

/** Full-screen sheet for yesterday's entry. Back (dismiss request) asks before discarding typed text. */
@Composable
fun BackfillSheet(viewModel: BackfillViewModel, timeZone: TimeZone, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(onDismissRequest = viewModel::onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(JournalTheme.colors.groupedBackground).safeDrawingPadding().imePadding()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                TextButton(onClick = viewModel::onBack) { Text("Cancel", color = JournalTheme.colors.accent) }
                Text("Yesterday · until ${formatTime(viewModel.offer.until, timeZone)}", color = JournalTheme.colors.secondaryText, fontSize = 14.sp)
                Spacer(Modifier.padding(top = 8.dp))
                Text(viewModel.offer.prompt.text, style = JournalTheme.promptStyle)
                Spacer(Modifier.padding(top = 16.dp))
                JournalCard(padding = 12) {
                    BasicTextField(
                        value = state.text,
                        onValueChange = { viewModel.onTextChange(it) },
                        enabled = !state.windowClosed,
                        textStyle = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(JournalTheme.colors.accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 300.dp).padding(4.dp).focusRequester(focus).testTag("backfill-text-field"),
                    )
                    state.message?.let { Text(it, color = JournalTheme.colors.warning, fontSize = 12.sp) }
                }
                Spacer(Modifier.padding(top = 12.dp))
                PrimaryButton("Save", onClick = viewModel::save, enabled = state.canSave, iconRes = R.drawable.ic_check, modifier = Modifier.testTag("backfill-save-button"))
            }
        }
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::keepWriting,
            title = { Text("Discard yesterday's entry?") },
            text = { Text("What you've written won't be saved.") },
            confirmButton = { TextButton(onClick = viewModel::discard, modifier = Modifier.testTag("backfill-discard")) { Text("Discard") } },
            dismissButton = { TextButton(onClick = viewModel::keepWriting) { Text("Keep writing") } },
        )
    }
}
