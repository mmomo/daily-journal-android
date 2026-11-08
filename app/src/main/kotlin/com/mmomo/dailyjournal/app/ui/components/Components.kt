package com.mmomo.dailyjournal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmomo.dailyjournal.app.R
import com.mmomo.dailyjournal.app.ui.theme.JournalTheme
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.StreakStatus
import com.mmomo.dailyjournal.domain.entities.StreakSummary
import java.time.format.DateTimeFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** Rounded surface on the grouped background (iOS cards). */
@Composable
fun JournalCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(JournalTheme.colors.card, RoundedCornerShape(20.dp))
            .padding(padding.dp),
        content = content,
    )
}

/** Full-width filled main action (Done, Save, Next). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = JournalTheme.colors.accent),
    ) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
fun SmallPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier, shape = CircleShape) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp), tint = JournalTheme.colors.accent)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = JournalTheme.colors.accent, fontSize = 14.sp)
    }
}

/** Current and best streak with done and at-risk states (US-4.1–4.4). */
@Composable
fun StreakCard(streak: StreakSummary, timeZone: TimeZone, modifier: Modifier = Modifier) {
    val colors = JournalTheme.colors
    val active = streak.current > 0
    JournalCard(modifier.testTag("today-streak-card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(if (active) colors.warning.copy(alpha = 0.15f) else colors.tertiaryFill, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = if (active) colors.warning else colors.secondaryText)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (active) {
                    Text("${streak.current}", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("streak-count"))
                    Text(if (streak.current == 1) "day streak" else "days in a row", color = colors.secondaryText, fontSize = 15.sp)
                } else {
                    Text("Start your streak today", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (streak.isTodayDone) {
                    Text("✓ Done", color = colors.success, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Text(
                    "Best ${streak.best}",
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.background(colors.tertiaryFill, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        val status = streak.status
        if (status is StreakStatus.AtRisk) {
            val verb = if (streak.isTodayDone) "restore" else "keep"
            Spacer(Modifier.padding(top = 10.dp))
            Text(
                "Write yesterday's entry by ${formatTime(status.until, timeZone)} to $verb your ${status.restorableCount}-day streak.",
                color = colors.warning,
                fontSize = 13.sp,
            )
        }
    }
}

/** Today's prompt with the category chip and the limited refresh (US-1.1, US-1.3). */
@Composable
fun PromptCard(prompt: Prompt, refreshesRemaining: Int, showsRefresh: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val colors = JournalTheme.colors
    JournalCard(modifier, padding = 20) {
        Text(
            prompt.category.rawValue.uppercase(),
            color = colors.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.background(colors.accent.copy(alpha = 0.12f), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Spacer(Modifier.padding(top = 12.dp))
        Text(prompt.text, style = JournalTheme.promptStyle, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("today-prompt-text"))
        if (showsRefresh) {
            Spacer(Modifier.padding(top = 12.dp))
            SmallPillButton(
                text = if (refreshesRemaining > 0) "New prompt · $refreshesRemaining left" else "No refreshes left today",
                onClick = onRefresh,
                enabled = refreshesRemaining > 0,
                iconRes = R.drawable.ic_refresh,
                modifier = Modifier.testTag("today-refresh-button"),
            )
        }
    }
}

fun formatTime(instant: Instant, timeZone: TimeZone): String =
    DateTimeFormatter.ofPattern("HH:mm").format(instant.toJavaInstant().atZone(timeZone.toJavaZoneId()))
