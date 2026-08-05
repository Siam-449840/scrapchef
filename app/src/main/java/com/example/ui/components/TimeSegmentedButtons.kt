package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.LocalScrapChefTokens

@Composable
fun TimeSegmentedButtons(
    selectedMaxTimeMinutes: Int,
    onTimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current

    val options = remember {
        listOf(
            15 to "15 min",
            30 to "30 min",
            45 to "45 min",
            0 to "No limit"
        )
    }

    Surface(
        shape = RoundedCornerShape(tokens.Radius.md),
        color = extendedColors.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(tokens.Spacing.xxs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (timeVal, label) ->
                val isSelected = selectedMaxTimeMinutes == timeVal

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "time_bg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "time_content"
                )

                val elevation by animateDpAsState(
                    targetValue = if (isSelected) tokens.Elevation.sm else tokens.Elevation.none,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "time_elevation"
                )

                Surface(
                    shape = RoundedCornerShape(tokens.Radius.sm),
                    color = bgColor,
                    shadowElevation = elevation,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(tokens.Radius.sm))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            role = Role.RadioButton,
                            onClick = { onTimeSelected(timeVal) }
                        )
                        .testTag("time_button_$timeVal")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            vertical = tokens.Spacing.md
                        )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(tokens.IconSize.xs)
                            )
                            Spacer(modifier = Modifier.width(tokens.Spacing.xs))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
