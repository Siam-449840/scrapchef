package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalScrapChefTokens
import com.example.ui.theme.bounceClick

// No direct icon import needed — resolved through IngredientIconService

@Composable
fun IngredientChip(
    canonical: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current

    Surface(
        shape = RoundedCornerShape(tokens.Radius.pill),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .bounceClick(scaleDown = 0.95f) { onRemove() }
            .testTag("ingredient_chip_$canonical")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    start = tokens.Spacing.sm,
                    end = tokens.Spacing.xs,
                    top = tokens.Spacing.xs,
                    bottom = tokens.Spacing.xs
                )
        ) {
            Icon(
                imageVector = IngredientIconService.iconFor(canonical),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(tokens.IconSize.sm)
            )
            Spacer(modifier = Modifier.width(tokens.Spacing.xs))
            Text(
                text = canonical,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(tokens.Spacing.xs))
            // Remove button — visual size 20dp, touch target 48dp
            Box(
                modifier = Modifier
                    .size(tokens.TouchTarget.min)
                    .clip(CircleShape)
                    // The tap is now handled by the entire chip via bounceClick
                    .testTag("remove_ingredient_$canonical"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove $canonical",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
