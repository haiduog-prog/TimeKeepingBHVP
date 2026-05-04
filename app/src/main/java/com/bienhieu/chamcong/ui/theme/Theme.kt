package com.bienhieu.chamcong.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Colors ──
private val Primary = Color(0xFF1E88E5)        // Vibrant blue
private val PrimaryDark = Color(0xFF1565C0)
private val Secondary = Color(0xFF26A69A)       // Teal accent
private val Background = Color(0xFF0D1117)      // Deep dark
private val Surface = Color(0xFF161B22)         // Card surface
private val SurfaceVariant = Color(0xFF21262D)  // Elevated surface
private val OnPrimary = Color.White
private val OnBackground = Color(0xFFE6EDF3)
private val OnSurface = Color(0xFFC9D1D9)
private val Success = Color(0xFF2EA043)         // Green for CHECK_IN
private val Error = Color(0xFFDA3633)           // Red for errors
private val Warning = Color(0xFFD29922)         // Amber for CHECK_OUT

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error,
)

/** Extra colors accessible via [ChamCongColors]. */
object ChamCongColors {
    val success = Success
    val warning = Warning
    val primaryDark = PrimaryDark
    val cardBackground = Surface
    val divider = Color(0xFF30363D)
}

@Composable
fun ChamCongTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            headlineLarge = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            ),
            headlineMedium = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            ),
            titleLarge = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            ),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
                color = OnSurface
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                color = OnSurface
            ),
            labelLarge = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
        ),
        content = content
    )
}
