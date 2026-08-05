package com.example.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.LocalScrapChefTokens

@Composable
fun ShimmerModifier(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 4.dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    return modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(brush)
}

@Composable
fun RecipeSkeleton(modifier: Modifier = Modifier) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.Radius.xl),
        colors = CardDefaults.cardColors(
            containerColor = extendedColors.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.Elevation.md)
    ) {
        Column(
            modifier = Modifier.padding(tokens.Spacing.xl)
        ) {
            // Header badge
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
                    .then(ShimmerModifier(cornerRadius = tokens.Radius.xs))
            )

            Spacer(modifier = Modifier.height(tokens.Spacing.md))

            // Title (2 lines)
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(32.dp).then(ShimmerModifier()))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(32.dp).then(ShimmerModifier()))

            Spacer(modifier = Modifier.height(tokens.Spacing.sm))

            // Meta row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(80.dp).height(16.dp).then(ShimmerModifier()))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.width(60.dp).height(16.dp).then(ShimmerModifier()))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.width(70.dp).height(16.dp).then(ShimmerModifier()))
            }

            Spacer(modifier = Modifier.height(tokens.Spacing.xxl))

            // Section Title
            Box(modifier = Modifier.width(150.dp).height(24.dp).then(ShimmerModifier()))
            Spacer(modifier = Modifier.height(tokens.Spacing.sm))

            // List items
            repeat(4) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = tokens.Spacing.xs)
                ) {
                    Box(modifier = Modifier.size(tokens.IconSize.md).clip(CircleShape).then(ShimmerModifier(cornerRadius = 12.dp)))
                    Spacer(modifier = Modifier.width(tokens.Spacing.md))
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp).then(ShimmerModifier()))
                }
            }
            
            Spacer(modifier = Modifier.height(tokens.Spacing.xxl))
            
            // Instructions skeleton
            Box(modifier = Modifier.width(180.dp).height(24.dp).then(ShimmerModifier()))
            Spacer(modifier = Modifier.height(tokens.Spacing.md))
            
            repeat(3) {
                Row(modifier = Modifier.padding(vertical = tokens.Spacing.xs)) {
                    Box(modifier = Modifier.size(24.dp).then(ShimmerModifier(cornerRadius = 4.dp)))
                    Spacer(modifier = Modifier.width(tokens.Spacing.md))
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(16.dp).then(ShimmerModifier()))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).then(ShimmerModifier()))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
