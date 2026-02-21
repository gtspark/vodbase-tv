package net.vodbase.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val VodBaseDarkColors = darkColorScheme(
    primary = Color(0xFFEF4444),
    onPrimary = Color.White,
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    background = Color(0xFF0A0A0F),
    onBackground = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A3E),
    onSurfaceVariant = Color(0xFFB8B8D4),
    outline = Color(0xFF333344)
)

private val VodBaseTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Color.White
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        color = Color(0xFFB8B8D4)
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = Color(0xFFB8B8D4)
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        color = Color(0xFF7A7A9A)
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
)

@Composable
fun VodBaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VodBaseDarkColors,
        typography = VodBaseTypography,
        content = content
    )
}
