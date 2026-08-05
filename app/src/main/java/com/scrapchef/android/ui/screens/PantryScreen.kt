package com.scrapchef.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrapchef.android.data.model.PantryItemEntity
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantryScreen(
    viewModel: PantryViewModel,
    onNavigateToAdd: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val uiState by viewModel.uiState.collectAsState()

    val groupedItems = uiState.inventory.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Kitchen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(tokens.IconSize.lg)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                        Text(
                            text = "My Pantry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extendedColors.surfaceContainerLow
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
                },
                text = {
                    Text("Add Ingredient", fontWeight = FontWeight.Bold)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(tokens.Radius.xl),
                modifier = Modifier.testTag("add_ingredient_fab")
            )
        },
        bottomBar = bottomBar,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.inventory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Kitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(tokens.Spacing.md))
                    Text(
                        text = "Your pantry is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                    Text(
                        text = "Add ingredients to build your kitchen inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = tokens.Content.maxWidth),
                    contentPadding = PaddingValues(
                        start = tokens.Spacing.lg,
                        end = tokens.Spacing.lg,
                        top = tokens.Spacing.sm,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(tokens.Spacing.md)
                ) {
                    
                    groupedItems.forEach { (category, items) ->
                        stickyHeader {
                            Text(
                                text = category.ifBlank { "Other" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = tokens.Spacing.sm)
                            )
                        }

                        items(items, key = { it.name }) { item ->
                            PantryItemRow(
                                item = item,
                                onRemove = { viewModel.removePantryItem(item.name) }
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun PantryItemRow(
    item: PantryItemEntity,
    onRemove: () -> Unit
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current

    Card(
        shape = RoundedCornerShape(tokens.Radius.sm),
        colors = CardDefaults.cardColors(
            containerColor = extendedColors.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.Elevation.none),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
