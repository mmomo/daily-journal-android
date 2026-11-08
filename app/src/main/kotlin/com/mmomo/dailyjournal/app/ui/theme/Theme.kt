package com.mmomo.dailyjournal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** iOS look (NFR-1.5): the iOS accent color and grouped backgrounds, light and dark. No wallpaper colors. */
@Immutable
data class JournalColors(
    val accent: Color,
    val groupedBackground: Color,
    val card: Color,
    val secondaryText: Color,
    val tertiaryFill: Color,
    val success: Color,
    val warning: Color,
)

private val Light = JournalColors(
    accent = Color(0xFFE86B3E), // iOS AccentColor (0.910, 0.420, 0.243)
    groupedBackground = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF6C6C70),
    tertiaryFill = Color(0x1F767680),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9500),
)

private val Dark = JournalColors(
    accent = Color(0xFFFA8F66), // iOS AccentColor dark (0.980, 0.560, 0.400)
    groupedBackground = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    secondaryText = Color(0xFF98989F),
    tertiaryFill = Color(0x3D767680),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
)

val LocalJournalColors = staticCompositionLocalOf { Light }

object JournalTheme {
    val colors: JournalColors @Composable get() = LocalJournalColors.current

    /** Prompt text uses the phone's built-in serif (Q2:A). */
    val promptStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val promptStyleSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
}

@Composable
fun JournalTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(primary = colors.accent, background = colors.groupedBackground, surface = colors.card)
    } else {
        lightColorScheme(primary = colors.accent, background = colors.groupedBackground, surface = colors.card)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalJournalColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
