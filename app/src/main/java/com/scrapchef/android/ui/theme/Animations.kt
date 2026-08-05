package com.scrapchef.android.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

/**
 * Navigation transition specs for ScrapChef.
 *
 * Motion policy: every animation communicates spatial relationship
 * or state change. No decorative motion.
 */
object ScrapChefTransitions {

    private val duration = ScrapChefTokens.Duration.Normal
    private val easing = FastOutSlowInEasing

    /** Push navigation (e.g., landing → recipe result). */
    val pushEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.06f).toInt() },
        animationSpec = tween(durationMillis = duration, easing = easing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = duration, easing = easing)
    )

    val pushExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.02f).toInt() },
        animationSpec = tween(durationMillis = duration, easing = easing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = duration, easing = easing)
    )

    /** Pop navigation (e.g., recipe result → landing via back). */
    val popEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.02f).toInt() },
        animationSpec = tween(durationMillis = duration, easing = easing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = duration, easing = easing)
    )

    val popExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.06f).toInt() },
        animationSpec = tween(durationMillis = duration, easing = easing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = duration, easing = easing)
    )

    /** Tab switch — instant crossfade with LinearEasing (no slide, no overlap jank). */
    val tabEnter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = ScrapChefTokens.Duration.Quick, easing = LinearEasing)
    )

    val tabExit: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = ScrapChefTokens.Duration.Quick, easing = LinearEasing)
    )
}

/**
 * Adds a bounce (scale down) animation when the component is pressed,
 * along with haptic feedback.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "bounceScale"
    )
    val view = LocalView.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // We handle the visual feedback via scale and elevation
            onClick = onClick
        )
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}
