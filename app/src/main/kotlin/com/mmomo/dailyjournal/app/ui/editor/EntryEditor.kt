package com.mmomo.dailyjournal.app.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.ui.components.JournalCard
import com.mmomo.dailyjournal.app.ui.components.PrimaryButton
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme

/** Editor card plus the full-width Done button. Long text scrolls inside the editor; Done stays above the keyboard. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryEditor(
    viewModel: EntryEditorViewModel,
    autoFocus: Boolean,
    focusRequests: Int,
    onFinished: () -> Unit,
    finishLabel: String = "Done",
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = JournalTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val doneVisible = remember { BringIntoViewRequester() }

    LaunchedEffect(autoFocus, focusRequests) {
        if ((autoFocus || focusRequests > 0) && !state.isLocked) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(state.text) { doneVisible.bringIntoView() }

    Column {
        JournalCard(padding = 12) {
            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 300.dp)) {
                if (state.text.isEmpty()) {
                    Text("Start writing…", color = colors.secondaryText, fontSize = 17.sp, modifier = Modifier.padding(4.dp))
                }
                BasicTextField(
                    value = state.text,
                    onValueChange = { viewModel.onTextChange(it) },
                    enabled = !state.isLocked,
                    textStyle = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().padding(4.dp).focusRequester(focus).testTag("editor-text-field"),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
                val status = state.message ?: when (state.status) {
                    EntryEditorViewModel.Status.SAVING -> "Saving…"
                    EntryEditorViewModel.Status.SAVED -> "✓ Saved"
                    else -> ""
                }
                Text(status, fontSize = 12.sp, color = if (state.message != null) colors.warning else colors.secondaryText, modifier = Modifier.weight(1f).testTag("editor-status"))
                if (state.text.isNotEmpty()) {
                    Text(if (state.wordCount == 1) "1 word" else "${state.wordCount} words", fontSize = 12.sp, color = colors.secondaryText)
                }
            }
        }
        Spacer(Modifier.padding(top = 12.dp))
        PrimaryButton(
            text = finishLabel,
            onClick = { viewModel.done { keyboard?.hide(); onFinished() } },
            enabled = state.canFinish,
            iconRes = R.drawable.ic_check,
            modifier = Modifier.bringIntoViewRequester(doneVisible).testTag("editor-done-button"),
        )
    }
}
