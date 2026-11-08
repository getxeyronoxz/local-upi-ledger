package org.xeyronox.ledger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)
val GreenEmerald = Color(0xFF10B981)
val RedRose = Color(0xFFF43F5E)
val AmberPending = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = Teal500,
    onPrimary = Color.Black,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF8FAFC),
    surface = Slate800,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Slate700,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Slate700
)

@Composable
fun LocalLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
