package com.scrapchef.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrapchef.android.ui.components.IngredientAutocomplete
import com.scrapchef.android.ui.components.IngredientChip
import com.scrapchef.android.ui.components.TimeSegmentedButtons
import com.scrapchef.android.ui.components.ToolFilterChips
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.theme.bounceClick
import com.scrapchef.android.ui.viewmodel.RecipeUiState
import com.scrapchef.android.ui.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientInputScreen(
    viewModel: RecipeViewModel,
    uiState: RecipeUiState,
    onNavigateBack: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "What do you have?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetSelections()
                        onNavigateBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = tokens.Content.maxWidth)
                    .verticalScroll(scrollState)
                    .padding(horizontal = tokens.Spacing.lg, vertical = tokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(tokens.Spacing.md)
            ) {

                // ─── Error Banner ──────────────────────────
                if (uiState.error != null) {
                    Card(
                        shape = RoundedCornerShape(tokens.Radius.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                // ─── Ingredient Search ────────────────────────────
                IngredientAutocomplete(
                    query = uiState.ingredientQuery,
                    onQueryChange = { viewModel.updateQuery(it) },
                    searchResults = uiState.searchResults,
                    onIngredientSelected = { viewModel.selectIngredient(it) },
                    isDisabled = uiState.selectedIngredients.size >= 8
                )

                // ─── Selected Ingredient Chips ────────────────────
                if (uiState.selectedIngredients.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(tokens.Radius.xs),
                            color = if (uiState.selectedIngredients.size >= 3) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                extendedColors.surfaceContainerHigh
                            }
                        ) {
                            Text(
                                text = "${uiState.selectedIngredients.size} / 8",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.selectedIngredients.size >= 3) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .padding(
                                        horizontal = tokens.Spacing.sm,
                                        vertical = tokens.Spacing.xxs
                                    )
                                    .testTag("ingredient_count_text")
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.selectedIngredients.forEach { canonical ->
                            IngredientChip(
                                canonical = canonical,
                                onRemove = { viewModel.removeIngredient(canonical) }
                            )
                        }
                    }
                }

                // ─── Cooking Tools ────────────────────────────────
                Spacer(modifier = Modifier.height(tokens.Spacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = tokens.Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.SoupKitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(tokens.IconSize.sm)
                    )
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text(
                        text = "Cooking Tools",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ToolFilterChips(
                    selectedTools = uiState.selectedTools,
                    onToolToggle = { viewModel.toggleTool(it) }
                )

                // ─── Prep Time ────────────────────────────────────
                Spacer(modifier = Modifier.height(tokens.Spacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = tokens.Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(tokens.IconSize.sm)
                    )
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text(
                        text = "Prep Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TimeSegmentedButtons(
                    selectedMaxTimeMinutes = uiState.selectedMaxTimeMinutes,
                    onTimeSelected = { viewModel.setMaxTime(it) }
                )

                // ─── Generate CTA ──────────────────────────────────
                Spacer(modifier = Modifier.height(tokens.Spacing.sm))

                val canGenerate = uiState.selectedIngredients.size >= 3 &&
                        uiState.selectedTools.isNotEmpty() &&
                        !uiState.isLoading

                Button(
                    onClick = {
                        viewModel.generateRecipe(onSuccess = onNavigateToRecipe)
                    },
                    enabled = canGenerate,
                    shape = RoundedCornerShape(tokens.Radius.sheet),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = extendedColors.surfaceContainerHigh
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (canGenerate) tokens.Elevation.lg else tokens.Elevation.none,
                        pressedElevation = tokens.Elevation.sm
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_recipe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(tokens.IconSize.md)
                    )
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text(
                        text = if (uiState.selectedIngredients.size < 3) {
                            "Select at least 3 ingredients"
                        } else {
                            "Cook something"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ─── Footer ───────────────────────────────────────
                Text(
                    text = "Pantry staples we always assume: salt, pepper, oil, water",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = tokens.Spacing.lg)
                )
            }
        }
    }
}
