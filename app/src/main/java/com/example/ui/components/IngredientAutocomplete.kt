package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.IngredientEntity
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.LocalScrapChefTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientAutocomplete(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<IngredientEntity>,
    onIngredientSelected: (String) -> Unit,
    isDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    var expanded by remember { mutableStateOf(false) }
    val trimmedQuery = query.trim()
    val isCustomQueryNew = trimmedQuery.length >= 2 &&
            searchResults.none { it.canonical.equals(trimmedQuery, ignoreCase = true) }

    // Animated border color for focus feedback
    val focusedBorderColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_focus_border"
    )

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && (searchResults.isNotEmpty() || isCustomQueryNew) && !isDisabled,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = it.isNotBlank()
                },
                placeholder = {
                    Text(
                        text = "Search ingredients...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(tokens.IconSize.md)
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(tokens.TouchTarget.min)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(tokens.IconSize.sm)
                            )
                        }
                    }
                } else null,
                enabled = !isDisabled,
                singleLine = true,
                shape = RoundedCornerShape(tokens.Radius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = focusedBorderColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = extendedColors.surfaceContainerLow,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    disabledContainerColor = extendedColors.surfaceContainerLow.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .testTag("ingredient_search_input")
            )

            ExposedDropdownMenu(
                expanded = expanded && (searchResults.isNotEmpty() || isCustomQueryNew) && !isDisabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                if (isCustomQueryNew) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "Add \"$trimmedQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Custom ingredient",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add $trimmedQuery",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(tokens.IconSize.md)
                            )
                        },
                        onClick = {
                            onIngredientSelected(trimmedQuery)
                            expanded = false
                        },
                        modifier = Modifier.testTag("dropdown_custom_$trimmedQuery")
                    )
                }

                searchResults.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = item.canonical,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add ${item.canonical}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(tokens.IconSize.md)
                            )
                        },
                        onClick = {
                            onIngredientSelected(item.canonical)
                            expanded = false
                        },
                        modifier = Modifier.testTag("dropdown_item_${item.canonical}")
                    )
                }
            }
        }

        if (isDisabled) {
            Text(
                text = "Maximum 8 ingredients selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = tokens.Spacing.sm, top = tokens.Spacing.xs)
            )
        }
    }
}
