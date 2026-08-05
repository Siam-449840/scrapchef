package com.scrapchef.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Extended color scheme for M3 surface container levels ───────
@Immutable
data class ExtendedColors(
    // Surface container levels
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    // Semantic: Success
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    // Semantic: Warning
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    // Semantic: Staple (sage green — pantry staples)
    val staple: Color,
    val onStaple: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        surfaceContainerLowest = Color.Unspecified,
        surfaceContainerLow = Color.Unspecified,
        surfaceContainer = Color.Unspecified,
        surfaceContainerHigh = Color.Unspecified,
        surfaceContainerHighest = Color.Unspecified,
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        warningContainer = Color.Unspecified,
        onWarningContainer = Color.Unspecified,
        staple = Color.Unspecified,
        onStaple = Color.Unspecified,
    )
}

// ─── M3 Color Schemes ────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = ScrapChefPrimaryLight,
    onPrimary = ScrapChefOnPrimaryLight,
    primaryContainer = ScrapChefPrimaryContainerLight,
    onPrimaryContainer = ScrapChefOnPrimaryContainerLight,
    secondary = ScrapChefSecondaryLight,
    onSecondary = ScrapChefOnSecondaryLight,
    secondaryContainer = ScrapChefSecondaryContainerLight,
    onSecondaryContainer = ScrapChefOnSecondaryContainerLight,
    tertiary = ScrapChefTertiaryLight,
    onTertiary = ScrapChefOnTertiaryLight,
    tertiaryContainer = ScrapChefTertiaryContainerLight,
    onTertiaryContainer = ScrapChefOnTertiaryContainerLight,
    background = ScrapChefBackgroundLight,
    onBackground = ScrapChefOnSurfaceLight,
    surface = ScrapChefSurfaceLight,
    onSurface = ScrapChefOnSurfaceLight,
    surfaceVariant = ScrapChefSurfaceVariantLight,
    onSurfaceVariant = ScrapChefOnSurfaceVariantLight,
    outline = ScrapChefOutlineLight,
    outlineVariant = ScrapChefOutlineVariantLight,
    error = ScrapChefErrorLight,
    onError = ScrapChefOnErrorLight,
    errorContainer = ScrapChefErrorContainerLight,
    onErrorContainer = ScrapChefOnErrorContainerLight,
    scrim = ScrapChefScrimLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ScrapChefPrimaryDark,
    onPrimary = ScrapChefOnPrimaryDark,
    primaryContainer = ScrapChefPrimaryContainerDark,
    onPrimaryContainer = ScrapChefOnPrimaryContainerDark,
    secondary = ScrapChefSecondaryDark,
    onSecondary = ScrapChefOnSecondaryDark,
    secondaryContainer = ScrapChefSecondaryContainerDark,
    onSecondaryContainer = ScrapChefOnSecondaryContainerDark,
    tertiary = ScrapChefTertiaryDark,
    onTertiary = ScrapChefOnTertiaryDark,
    tertiaryContainer = ScrapChefTertiaryContainerDark,
    onTertiaryContainer = ScrapChefOnTertiaryContainerDark,
    background = ScrapChefBackgroundDark,
    onBackground = ScrapChefOnSurfaceDark,
    surface = ScrapChefSurfaceDark,
    onSurface = ScrapChefOnSurfaceDark,
    surfaceVariant = ScrapChefSurfaceElevatedDark,
    onSurfaceVariant = ScrapChefOnSurfaceVariantDark,
    outline = ScrapChefOutlineDark,
    outlineVariant = ScrapChefOutlineVariantDark,
    error = ScrapChefErrorDark,
    onError = ScrapChefOnErrorDark,
    errorContainer = ScrapChefErrorContainerDark,
    onErrorContainer = ScrapChefOnErrorContainerDark,
    scrim = ScrapChefScrimDark
)

// ─── Extended surface container colors per theme ─────────────────

private val LightExtendedColors = ExtendedColors(
    surfaceContainerLowest = ScrapChefSurfaceContainerLowestLight,
    surfaceContainerLow = ScrapChefSurfaceContainerLowLight,
    surfaceContainer = ScrapChefSurfaceContainerLight,
    surfaceContainerHigh = ScrapChefSurfaceContainerHighLight,
    surfaceContainerHighest = ScrapChefSurfaceContainerHighestLight,
    success = ScrapChefSuccessLight,
    onSuccess = ScrapChefOnSuccessLight,
    successContainer = ScrapChefSuccessContainerLight,
    onSuccessContainer = ScrapChefOnSuccessContainerLight,
    warning = ScrapChefWarningLight,
    onWarning = ScrapChefOnWarningLight,
    warningContainer = ScrapChefWarningContainerLight,
    onWarningContainer = ScrapChefOnWarningContainerLight,
    staple = ScrapChefStapleLight,
    onStaple = ScrapChefOnStapleLight,
)

private val DarkExtendedColors = ExtendedColors(
    surfaceContainerLowest = ScrapChefSurfaceContainerLowestDark,
    surfaceContainerLow = ScrapChefSurfaceContainerLowDark,
    surfaceContainer = ScrapChefSurfaceContainerDark,
    surfaceContainerHigh = ScrapChefSurfaceContainerHighDark,
    surfaceContainerHighest = ScrapChefSurfaceContainerHighestDark,
    success = ScrapChefSuccessDark,
    onSuccess = ScrapChefOnSuccessDark,
    successContainer = ScrapChefSuccessContainerDark,
    onSuccessContainer = ScrapChefOnSuccessContainerDark,
    warning = ScrapChefWarningDark,
    onWarning = ScrapChefOnWarningDark,
    warningContainer = ScrapChefWarningContainerDark,
    onWarningContainer = ScrapChefOnWarningContainerDark,
    staple = ScrapChefStapleDark,
    onStaple = ScrapChefOnStapleDark,
)

// ─── Theme Composable ────────────────────────────────────────────

@Composable
fun ScrapChefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: transparent status bar, system nav bar matches background
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalScrapChefTokens provides ScrapChefTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScrapChefTypography,
            content = content
        )
    }
}

// ─── Convenience accessor ────────────────────────────────────────

/** Access extended surface container colors from any composable. */
object ScrapChefThemeExtras {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
