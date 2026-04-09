package io.yarburart.reiproxy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFD971F),          // orange
    secondary = Color(0xFFA6E22E),        // green
    tertiary = Color(0xFF64B5F6),
    background = Color(0xFF272822),       // dark Monokai base
    surface = Color(0xFF272822),
    surfaceVariant = Color(0xFF3E3D32),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF8F8F2),     // foreground text
    onSurface = Color(0xFFF8F8F2),
    onSurfaceVariant = Color(0xFFBFBFBF),
    primaryContainer = Color(0xFF5A3F0D),
    secondaryContainer = Color(0xFF3D550F),
    tertiaryContainer = Color(0xFF5A1F33),
    error = Color(0xFFF06292),
    onError = Color.Black,
    errorContainer = Color(0xFF5A1F33),
    onErrorContainer = Color(0xFFF8F8F2),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF8A65),
    secondary = Color(0xFFA6E22E),
    tertiary = Color(0xFF64B5F6),
    background = Color(0xFFF8F8F2),
    surface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFFE6E6DA),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF272822),
    onSurface = Color(0xFF272822),
    onSurfaceVariant = Color(0xFF555555),
    primaryContainer = Color(0xFFFDCA96),
    secondaryContainer = Color(0xFFDFF3B0),
    tertiaryContainer = Color(0xFFFFA6B0),
    error = Color(0xFFF06292),
    onError = Color.Black,
    errorContainer = Color(0xFFFFA6B0),
    onErrorContainer = Color(0xFF5A1F33),
)

private val WhiteColorScheme = lightColorScheme(
    primary = Color(0xFFFF8A65),
    secondary = Color(0xFFA6E22E),
    tertiary = Color(0xFF64B5F6),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF444444),
    primaryContainer = Color(0xFFFFD8B0),
    secondaryContainer = Color(0xFFDFF3B0),
    tertiaryContainer = Color(0xFFFFA6B0),
    error = Color(0xFFF06292),
    onError = Color.Black,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)
@Composable
fun ReiProxyTheme(
    themeMode: String = "system", // "system", "light", "dark", "white"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val useDynamic = themeMode != "white" && themeMode != "light" &&
        themeMode != "dark" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when (themeMode) {
        "white" -> WhiteColorScheme
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        else -> if (useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeMode != "dark"
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}