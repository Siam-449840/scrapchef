package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Recipe
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.LocalScrapChefTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current

    val effectiveInstructions = recipe.effectiveInstructions
    val effectiveTips = recipe.effectiveTips

    val completedSteps = remember { mutableStateMapOf<Int, Boolean>() }
    val completedCount = completedSteps.values.count { it }
    val totalSteps = effectiveInstructions.size
    val progress = if (totalSteps > 0) completedCount.toFloat() / totalSteps.toFloat() else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipe_card"),
        shape = RoundedCornerShape(tokens.Radius.xl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.Elevation.md)
    ) {
        Column(
            modifier = Modifier.padding(tokens.Spacing.xl)
        ) {

            // ─── Title ────────────────────────────────────────────
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("recipe_title")
            )

            Spacer(modifier = Modifier.height(tokens.Spacing.sm))

            // ─── Meta Row (time · servings) ───────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${recipe.totalTimeMinutes} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recipe.servings != null) {
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Icon(
                        Icons.Default.LocalDining,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${recipe.servings} servings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(tokens.Spacing.sm))

            // ─── Difficulty & Tool Chips ──────────────────────────
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm)
            ) {
                if (recipe.difficulty != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                recipe.difficulty,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(tokens.IconSize.sm)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                recipe.tools.forEach { tool ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                tool.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SoupKitchen,
                                contentDescription = null,
                                modifier = Modifier.size(tokens.IconSize.sm)
                            )
                        }
                    )
                }
            }

            // ─── Description ──────────────────────────────────────
            if (!recipe.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.md))
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // ─── Ingredients ──────────────────────────────────────
            Spacer(modifier = Modifier.height(tokens.Spacing.xxl))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                Text(
                    text = "Ingredients (${recipe.yourIngredients.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(tokens.Spacing.sm))

            recipe.yourIngredients.forEach { ing ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = tokens.Spacing.xs)
                ) {
                    Icon(
                        imageVector = IngredientIconService.iconFor(ing),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(tokens.IconSize.md)
                    )
                    Spacer(modifier = Modifier.width(tokens.Spacing.md))
                    Text(
                        text = ing,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ─── Pantry Staples ───────────────────────────────────
            if (recipe.pantryAssumptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.md))
                Surface(
                    shape = RoundedCornerShape(tokens.Radius.card),
                    color = extendedColors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(tokens.Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Opacity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(tokens.IconSize.sm)
                            )
                            Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                            Text(
                                text = "Assumed Pantry Staples",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                        Text(
                            text = recipe.pantryAssumptions.joinToString(", ") {
                                it.replaceFirstChar { c -> c.uppercase() }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Instructions ─────────────────────────────────────
            if (effectiveInstructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.xxl))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FoodBank,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text(
                        text = "Preparation Steps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(tokens.Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount of $totalSteps completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (completedCount == totalSteps && totalSteps > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(tokens.Radius.pill)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(tokens.Spacing.md))

                effectiveInstructions.forEachIndexed { index, stepText ->
                    val isDone = completedSteps[index] == true
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = tokens.Spacing.xxs)
                    ) {
                        Checkbox(
                            checked = isDone,
                            onCheckedChange = { checked -> completedSteps[index] = checked },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("step_checkbox_$index")
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 12.dp, end = tokens.Spacing.sm)
                        ) {
                            Text(
                                text = "${index + 1}. $stepText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDone) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }

            // ─── Tips ─────────────────────────────────────────────
            if (effectiveTips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.xl))
                Surface(
                    shape = RoundedCornerShape(tokens.Radius.card),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(tokens.Spacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(tokens.IconSize.lg)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.md))
                        Column {
                            Text(
                                text = if (effectiveTips.size == 1) "Chef's Tip" else "Chef's Tips",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                            effectiveTips.forEach { tipText ->
                                Text(
                                    text = if (effectiveTips.size > 1) "• $tipText" else tipText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(bottom = tokens.Spacing.xxs)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Notes ────────────────────────────────────────────
            if (recipe.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.md))
                Surface(
                    shape = RoundedCornerShape(tokens.Radius.card),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(tokens.Spacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(tokens.IconSize.lg)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.md))
                        Column {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                            recipe.notes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(bottom = tokens.Spacing.xxs)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Warnings ─────────────────────────────────────────
            if (recipe.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(tokens.Spacing.md))
                Surface(
                    shape = RoundedCornerShape(tokens.Radius.card),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(tokens.Spacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(tokens.IconSize.lg)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.md))
                        Column {
                            Text(
                                text = "Warnings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                            recipe.warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(bottom = tokens.Spacing.xxs)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Footer ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(tokens.Spacing.xxl))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(tokens.Spacing.xl))
        }
    }
}
