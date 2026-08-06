package com.scrapchef.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrapchef.android.data.remote.StreamingRecipeState
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.viewmodel.RecipeViewModel

/**
 * Generating + Recipe Detail screen per §10.6.
 * Content streams in progressively:
 * (1) dish name, (2) time/tools row, (3) your ingredients, (4) pantry staples,
 * (5) numbered steps one at a time, (6) tip.
 * On completion, this screen IS the Recipe Detail screen — no navigation jump.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratingScreen(
    viewModel: RecipeViewModel,
    onNavigateBack: () -> Unit,
    onStartCooking: () -> Unit,
    onNewRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val streamingState by viewModel.streamingState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showCancelDialog by remember { mutableStateOf(false) }

    // Per §10.6: cancel confirmation only if >50% streamed
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel generation?") },
            text = { Text("The recipe is almost ready. Are you sure you want to cancel?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelGeneration()
                    onNavigateBack()
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep going") }
            }
        )
    }

    // When a recipe was loaded from favorites or a previous generation,
    // prefer its fields over empty streamingState.
    // Extract to local val to enable smart cast (delegated properties don't support it).
    val currentRecipe = uiState.currentRecipe
    val displayRecipe = if (currentRecipe != null && streamingState.isComplete) {
        // Use recipe data if streamingState is also marked complete (generation finished)
        streamingState
    } else if (currentRecipe != null && !streamingState.isComplete && streamingState.name == null) {
        // StreamingState is empty — we're viewing a saved recipe (from favorites or process restart)
        // Use a synthetic state derived from currentRecipe
        StreamingRecipeState(
            name = currentRecipe.name,
            description = currentRecipe.description,
            difficulty = currentRecipe.difficulty,
            servings = currentRecipe.servings,
            totalTimeMinutes = currentRecipe.totalTimeMinutes,
            tools = currentRecipe.tools,
            yourIngredients = currentRecipe.yourIngredients,
            pantryAssumptions = currentRecipe.pantryAssumptions,
            instructions = currentRecipe.instructions,
            tips = currentRecipe.tips,
            notes = currentRecipe.notes,
            warnings = currentRecipe.warnings,
            isComplete = true
        )
    } else {
        streamingState
    }

    val isComplete = displayRecipe.isComplete && (uiState.currentRecipe != null || displayRecipe.name != null)

    androidx.activity.compose.BackHandler(enabled = !isComplete && uiState.error == null) {
        if (displayRecipe.progressPercent > 0.5f) {
            showCancelDialog = true
        } else {
            viewModel.cancelGeneration()
            onNavigateBack()
        }
    }

    // Error state per §10.6.1
    if (uiState.error != null && !uiState.isLoading) {
        Scaffold(modifier = modifier) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(tokens.Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(tokens.Spacing.lg))
                Text(
                    text = uiState.error?.message ?: "Something went wrong.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(tokens.Spacing.xl))
                Button(
                    onClick = { viewModel.dismissError(); viewModel.generateRecipe { /* already on this screen */ } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Try Again") }
                Spacer(modifier = Modifier.height(tokens.Spacing.sm))
                OutlinedButton(
                    onClick = { viewModel.dismissError(); onNavigateBack() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Adjust Ingredients") }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isComplete) "Recipe" else "Generating…") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (displayRecipe.progressPercent > 0.5f && !isComplete) {
                            showCancelDialog = true
                        } else {
                            viewModel.cancelGeneration()
                            onNavigateBack()
                        }
                    }) { Text(if (isComplete) "Back" else "Cancel") }
                },
                actions = {
                    if (isComplete) {
                        IconButton(onClick = { viewModel.toggleCurrentFavorite() }) {
                            Icon(
                                if (uiState.isCurrentRecipeFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isCurrentRecipeFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isComplete) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(tokens.Spacing.lg)) {
                        Button(
                            onClick = onStartCooking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Outlined.Kitchen, contentDescription = null)
                            Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                            Text("Start Cooking")
                        }
                        TextButton(
                            onClick = onNewRecipe,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("New Recipe") }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = tokens.Spacing.lg)
        ) {
            // (1) Dish name
            AnimatedVisibility(
                visible = displayRecipe.name != null,
                enter = fadeIn() + slideInVertically { -it / 2 }
            ) {
                Text(
                    text = displayRecipe.name ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = tokens.Spacing.md)
                )
            }

            // Description
            AnimatedVisibility(
                visible = displayRecipe.description != null,
                enter = fadeIn()
            ) {
                Text(
                    text = displayRecipe.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = tokens.Spacing.md)
                )
            }

            // (2) Time / Tools / Difficulty row
            AnimatedVisibility(
                visible = displayRecipe.totalTimeMinutes != null,
                enter = fadeIn() + slideInVertically { -it / 2 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = tokens.Spacing.md)
                ) {
                    displayRecipe.totalTimeMinutes?.let { time ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${time} min") },
                            leadingIcon = { Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    displayRecipe.difficulty?.let { diff ->
                        AssistChip(
                            onClick = {},
                            label = { Text(diff) }
                        )
                    }
                }
            }

            // (3) Your ingredients
            AnimatedVisibility(
                visible = displayRecipe.yourIngredients != null,
                enter = fadeIn() + slideInVertically { -it / 2 }
            ) {
                Column(modifier = Modifier.padding(bottom = tokens.Spacing.md)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = tokens.Spacing.sm))
                    Text(
                        text = "Your ingredients",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = tokens.Spacing.sm)
                    )
                    displayRecipe.yourIngredients?.forEach { ingredient ->
                        Text(
                            text = "• $ingredient",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // (4) Pantry staples
            AnimatedVisibility(
                visible = displayRecipe.pantryAssumptions != null,
                enter = fadeIn() + slideInVertically { -it / 2 }
            ) {
                Column(modifier = Modifier.padding(bottom = tokens.Spacing.md)) {
                    Text(
                        text = "Pantry staples used",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = tokens.Spacing.sm)
                    )
                    displayRecipe.pantryAssumptions?.forEach { staple ->
                        Text(
                            text = "◦ $staple",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // (5) Steps — stream in one at a time
            if (displayRecipe.instructions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = tokens.Spacing.sm))
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = tokens.Spacing.sm)
                )
                displayRecipe.instructions.forEachIndexed { index, step ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // (6) Tips
            AnimatedVisibility(
                visible = displayRecipe.tips != null && displayRecipe.tips!!.isNotEmpty(),
                enter = fadeIn() + slideInVertically { -it / 2 }
            ) {
                Card(
                    shape = RoundedCornerShape(tokens.Radius.md),
                    colors = CardDefaults.outlinedCardColors(),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = tokens.Spacing.md)
                ) {
                    Row(modifier = Modifier.padding(tokens.Spacing.md)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                        Column {
                            displayRecipe.tips?.forEach { tip ->
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Loading indicator while streaming
            if (!isComplete && displayRecipe.error == null) {
                Spacer(modifier = Modifier.height(tokens.Spacing.lg))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(tokens.Spacing.sm))
                Text(
                    text = if (displayRecipe.name == null) "Finding the perfect recipe…" else "Crafting your recipe…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(tokens.Spacing.xl))
        }
    }
}
