package com.scrapchef.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.scrapchef.android.data.model.Tool
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolFilterChips(
    selectedTools: Set<Tool>,
    onToolToggle: (Tool) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm)
    ) {
        Tool.entries.forEach { tool ->
            val isSelected = selectedTools.contains(tool)
            val icon = when (tool) {
                Tool.STOVETOP -> Icons.Default.SoupKitchen
                Tool.OVEN -> Icons.Default.OutdoorGrill
                Tool.MICROWAVE -> Icons.Default.Microwave
                Tool.AIR_FRYER -> Icons.Default.Air
                Tool.NO_COOK -> Icons.Outlined.Restaurant
            }

            FilterChip(
                selected = isSelected,
                onClick = { onToolToggle(tool) },
                label = {
                    Text(
                        text = tool.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(tokens.IconSize.sm)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(tokens.IconSize.sm)
                        )
                    }
                },
                shape = RoundedCornerShape(tokens.Radius.md),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = extendedColors.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    borderWidth = tokens.Elevation.xs,
                    selectedBorderWidth = tokens.Elevation.xs,
                    enabled = true,
                    selected = isSelected
                ),
                modifier = Modifier.testTag("tool_chip_${tool.id}")
            )
        }
    }
}
