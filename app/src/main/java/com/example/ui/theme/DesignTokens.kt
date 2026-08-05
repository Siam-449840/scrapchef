package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SpacingTokens {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val xxxxl: Dp = 40.dp
}

object RadiusTokens {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val pill: Dp = 100.dp
    // Named semantic radii from CSS design system
    val chip: Dp = 8.dp   // --radius-chip
    val card: Dp = 12.dp  // --radius-card
    val sheet: Dp = 20.dp // --radius-sheet (bottom sheets, CTA buttons)
}

object ElevationTokens {
    val none: Dp = 0.dp
    val xs: Dp = 0.5.dp
    val sm: Dp = 1.dp
    val md: Dp = 2.dp
    val lg: Dp = 4.dp
    val xl: Dp = 8.dp
}

object IconSizeTokens {
    val xs: Dp = 14.dp
    val sm: Dp = 16.dp
    val md: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}

object TouchTargetTokens {
    val min: Dp = 48.dp
}

object ContentTokens {
    val maxWidth: Dp = 640.dp
}

typealias Spacing = SpacingTokens
typealias Radius = RadiusTokens
typealias Elevation = ElevationTokens
typealias IconSize = IconSizeTokens
typealias TouchTarget = TouchTargetTokens
typealias Content = ContentTokens

/**
 * ScrapChef design tokens — the single source of truth for all visual measurements.
 *
 * Usage: `val tokens = LocalScrapChefTokens.current`
 */
@Immutable
object ScrapChefTokens {

    val Spacing = SpacingTokens
    val Radius = RadiusTokens
    val Elevation = ElevationTokens
    val IconSize = IconSizeTokens
    val TouchTarget = TouchTargetTokens
    val Content = ContentTokens

    /** Animation duration constants (milliseconds). */
    object Duration {
        const val Quick: Int = 80
        const val Normal: Int = 160
        const val Slow: Int = 250
    }

    /** Standard easing curves. */
    object Easing {
        val Standard = FastOutSlowInEasing
        val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    }

    /** Press feedback scale factor. */
    const val PressScale: Float = 0.97f
}

/**
 * CompositionLocal to access design tokens from any composable.
 */
val LocalScrapChefTokens = staticCompositionLocalOf { ScrapChefTokens }

