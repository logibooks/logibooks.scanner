// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LogibooksBlueLight,
    onPrimary = Color(0xFF0B2545),
    primaryContainer = LogibooksBlueDark,
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0B2545),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5F5),
    outline = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D)
)

private val LightColorScheme = lightColorScheme(
    primary = LogibooksBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBFF),
    onPrimaryContainer = Color(0xFF0B2A57),
    secondary = Color(0xFF5C6B7A),
    onSecondary = Color.White,
    background = LogibooksBackground,
    onBackground = LogibooksOnSurface,
    surface = LogibooksSurface,
    onSurface = LogibooksOnSurface,
    surfaceVariant = LogibooksSurfaceVariant,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFCAD5E2),
    error = LogibooksError,
    onError = Color.White
)

@Composable
fun LogiScannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
