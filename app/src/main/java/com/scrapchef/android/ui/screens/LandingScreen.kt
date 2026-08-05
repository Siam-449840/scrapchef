package com.scrapchef.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrapchef.android.R
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.theme.bounceClick
import com.scrapchef.android.ui.viewmodel.RecipeUiState
import com.scrapchef.android.ui.viewmodel.RecipeViewModel

private val QUICK_SUGGESTIONS = listOf(
    "15-minute dinners",
    "Surprise me",
    "One-pot meals",
    "No-cook ideas"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    viewModel: RecipeViewModel,
    uiState: RecipeUiState,
    onNavigateToIngredientInput: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_scrapchef_logo),
                                contentDescription = "ScrapChef logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                        Text(
                            text = "ScrapChef",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
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
                verticalArrangement = Arrangement.spacedBy(tokens.Spacing.lg)
            ) {

                // ─── Hero Card ───────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.Radius.xl),
                    colors = CardDefaults.cardColors(
                        containerColor = extendedColors.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = tokens.Elevation.sm)
                ) {
                    Column(
                        modifier = Modifier.padding(tokens.Spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(tokens.Spacing.md)
                    ) {
                        Text(
                            text = "What's in your kitchen?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Type your ingredients and get a recipe in seconds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(tokens.Spacing.xs))
                        Button(
                            onClick = onNavigateToIngredientInput,
                            shape = RoundedCornerShape(tokens.Radius.sheet),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick(scaleDown = 0.97f) { onNavigateToIngredientInput() }
                                .testTag("start_cooking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(tokens.IconSize.md)
                            )
                            Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                            Text(
                                text = "Start Cooking",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ─── Quick Suggestions ────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm)) {
                    Text(
                        text = "Quick ideas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        QUICK_SUGGESTIONS.forEach { label ->
                            Surface(
                                shape = RoundedCornerShape(tokens.Radius.pill),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.bounceClick(scaleDown = 0.95f) {
                                    onNavigateToIngredientInput()
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = tokens.Spacing.md,
                                        vertical = tokens.Spacing.sm
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ─── Footer ───────────────────────────────────────
                Text(
                    text = "We only ever use what you tell us you have, plus basic staples like salt, oil, and water.",
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
