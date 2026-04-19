package com.planora.app.core.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme { LIGHT, DARK, MIDNIGHT }

// Fallback palette
private val FallbackLightScheme = lightColorScheme(
    primary             = Color(0xFF006A60),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF74F8E5),
    onPrimaryContainer  = Color(0xFF00201C),
    secondary           = Color(0xFF4A6360),
    surface             = Color(0xFFF4FBF9),
    background          = Color(0xFFF4FBF9),
    onSurface           = Color(0xFF161D1C),
    onBackground        = Color(0xFF161D1C),
)

private val FallbackDarkScheme = darkColorScheme(
    primary             = Color(0xFF52DBC8),
    onPrimary           = Color(0xFF003731),
    primaryContainer    = Color(0xFF00504A),
    onPrimaryContainer  = Color(0xFF74F8E5),
    secondary           = Color(0xFFB0CCCA),
    surface             = Color(0xFF0E1514),
    background          = Color(0xFF0E1514),
    onSurface           = Color(0xFFDDE4E2),
    onBackground        = Color(0xFFDDE4E2),
)

@Composable
fun PlanoraTheme(
    appTheme: AppTheme = AppTheme.MIDNIGHT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // minSdk = 34 (Android 14) implies dynamic color support.
    val baseScheme = when (appTheme) {
        AppTheme.LIGHT    -> try { dynamicLightColorScheme(context) } catch (_: Exception) { FallbackLightScheme }
        AppTheme.DARK,
        AppTheme.MIDNIGHT -> try { dynamicDarkColorScheme(context)  } catch (_: Exception) { FallbackDarkScheme  }
    }

    // Midnight overrides surface/background neutrally while inheriting monet colors.
    val colorScheme = if (appTheme == AppTheme.MIDNIGHT) {
        baseScheme.copy(
            background           = Color(0xFF000000),
            surface              = Color(0xFF0D0D0D),
            surfaceVariant       = Color(0xFF141414),
            surfaceContainer     = Color(0xFF111111),
            surfaceContainerLow  = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF181818),
        )
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = appTheme == AppTheme.LIGHT
                isAppearanceLightNavigationBars = appTheme == AppTheme.LIGHT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PlanoraTypography,
        content     = content
    )
}
