package com.planora.app.core.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme { LIGHT, DARK, MIDNIGHT, ESPRESSO, MATCHA, NORD, ROSE }


// Espresso (Chocolate) palette
private val EspressoScheme = darkColorScheme(
    primary             = EspressoPrimary,
    onPrimary           = EspressoOnPrimary,
    primaryContainer    = Color(0xFF3D2E14),
    onPrimaryContainer  = Color(0xFFF0E0C8),
    secondary           = EspressoSecondary,
    background          = EspressoBackground,
    surface             = EspressoSurface,
    surfaceVariant      = Color(0xFF2B2624),
    surfaceContainerLow = Color(0xFF1F1B1A),
    onSurface           = Color(0xFFE8E1DE),
    onBackground        = Color(0xFFE8E1DE),
    onSurfaceVariant    = Color(0xFFADA09A),
    outline             = Color(0xFF5A4F4A),
)

// Matcha palette
private val MatchaScheme = darkColorScheme(
    primary             = MatchaPrimary,
    onPrimary           = MatchaOnPrimary,
    primaryContainer    = Color(0xFF2A4A2A),
    onPrimaryContainer  = Color(0xFFD0F0D0),
    secondary           = MatchaSecondary,
    background          = MatchaBackground,
    surface             = MatchaSurface,
    surfaceVariant      = Color(0xFF243224),
    surfaceContainerLow = Color(0xFF182018),
    onSurface           = Color(0xFFDAE8DA),
    onBackground        = Color(0xFFDAE8DA),
    onSurfaceVariant    = Color(0xFF98B098),
    outline             = Color(0xFF4A604A),
)

// Nord palette
private val NordScheme = darkColorScheme(
    primary             = NordPrimary,
    onPrimary           = NordOnPrimary,
    primaryContainer    = Color(0xFF434C5E),
    onPrimaryContainer  = Color(0xFFD8DEE9),
    secondary           = NordSecondary,
    background          = NordBackground,
    surface             = NordSurface,
    surfaceVariant      = Color(0xFF434C5E),
    surfaceContainerLow = Color(0xFF353C4A),
    onSurface           = Color(0xFFECEFF4),
    onBackground        = Color(0xFFECEFF4),
    onSurfaceVariant    = Color(0xFFD8DEE9),
    outline             = Color(0xFF4C566A),
)

// Rosé palette
private val RoseScheme = darkColorScheme(
    primary             = RosePrimary,
    onPrimary           = RoseOnPrimary,
    primaryContainer    = Color(0xFF4A2535),
    onPrimaryContainer  = Color(0xFFF8D8E8),
    secondary           = RoseSecondary,
    background          = RoseBackground,
    surface             = RoseSurface,
    surfaceVariant      = Color(0xFF30222A),
    surfaceContainerLow = Color(0xFF22181E),
    onSurface           = Color(0xFFE8DDE2),
    onBackground        = Color(0xFFE8DDE2),
    onSurfaceVariant    = Color(0xFFB8A0AC),
    outline             = Color(0xFF5A4050),
)

/**
 * Resolves dynamic (Material You) colors where available, falling back to static palettes.
 * The [isDark] parameter is used by LIGHT/DARK/MIDNIGHT to decide which system scheme to use.
 */
private fun dynamicOrFallback(context: android.content.Context, isDark: Boolean): ColorScheme {
    return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
fun PlanoraTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (appTheme) {
        AppTheme.LIGHT    -> dynamicOrFallback(context, isDark = false)
        AppTheme.DARK     -> dynamicOrFallback(context, isDark = true)
        AppTheme.MIDNIGHT -> {
            // Material You colors with pure-black background for AMOLED screens
            val base = dynamicOrFallback(context, isDark = true)
            base.copy(
                background           = Color.Black,
                surface              = Color(0xFF0A0A0A),
                surfaceVariant       = Color(0xFF141414),
                surfaceContainer     = Color(0xFF0E0E0E),
                surfaceContainerLow  = Color(0xFF060606),
                surfaceContainerHigh = Color(0xFF1A1A1A),
                surfaceDim           = Color.Black,
            )
        }
        AppTheme.ESPRESSO -> EspressoScheme
        AppTheme.MATCHA   -> MatchaScheme
        AppTheme.NORD     -> NordScheme
        AppTheme.ROSE     -> RoseScheme
    }

    // Determine whether the status bar icons should be light or dark
    val isLightTheme = appTheme == AppTheme.LIGHT

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = isLightTheme
                isAppearanceLightNavigationBars = isLightTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PlanoraTypography,
        content     = content
    )
}
