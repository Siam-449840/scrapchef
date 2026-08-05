package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.LocalScrapChefTokens
import com.example.ui.viewmodel.RecipeUiState
import com.example.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KitchenModeScreen(
    viewModel: RecipeViewModel,
    uiState: RecipeUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val recipe = uiState.currentRecipe ?: return
    val steps = recipe.effectiveInstructions
    
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setKitchenModeStep(pagerState.currentPage)
    }
    
    LaunchedEffect(uiState.kitchenModeCurrentStep) {
        if (pagerState.currentPage != uiState.kitchenModeCurrentStep) {
            pagerState.animateScrollToPage(uiState.kitchenModeCurrentStep)
        }
    }

    // Timer Tick
    LaunchedEffect(uiState.isKitchenModeTimerRunning) {
        while (uiState.isKitchenModeTimerRunning) {
            delay(1000L)
            viewModel.tickKitchenModeTimer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Kitchen Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extendedColors.surfaceContainerLow
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = tokens.Spacing.md, horizontal = tokens.Spacing.lg),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(6.dp)
                            .weight(1f)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val step = steps[page]
                val stepNumber = page + 1
                
                // Extremely simple time extraction for demo (e.g., "10 minutes", "5 min")
                val timeMatch = Regex("(\\d+)\\s*(?:minute|min|m)s?").find(step)
                val extractedMinutes = timeMatch?.groupValues?.get(1)?.toIntOrNull()

                Box(
                    modifier = Modifier.fillMaxSize().padding(tokens.Spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Step $stepNumber of ${steps.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(tokens.Spacing.xl))
                        
                        Text(
                            text = step,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(tokens.Spacing.xxl))

                        // Timer UI if time is detected
                        if (extractedMinutes != null) {
                            Card(
                                shape = RoundedCornerShape(tokens.Radius.lg),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(tokens.Spacing.lg),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Suggested Timer",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(tokens.Spacing.sm))
                                    
                                    val isTimerActiveForThisStep = uiState.kitchenModeTimerSeconds != null
                                    
                                    if (isTimerActiveForThisStep) {
                                        val remaining = uiState.kitchenModeTimerSeconds ?: 0
                                        val minutes = remaining / 60
                                        val seconds = remaining % 60
                                        val timeString = String.format("%02d:%02d", minutes, seconds)
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = timeString,
                                                style = MaterialTheme.typography.displayMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(tokens.Spacing.md))
                                            IconButton(
                                                onClick = { viewModel.toggleKitchenModeTimer() },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (uiState.isKitchenModeTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Toggle Timer"
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.setKitchenModeTimer(extractedMinutes * 60) }
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null)
                                            Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                                            Text("Start $extractedMinutes Min Timer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0
                ) {
                    Text("Previous")
                }
                
                if (pagerState.currentPage < steps.size - 1) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = onNavigateBack
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                        Text("Finish")
                    }
                }
            }
        }
    }
}
