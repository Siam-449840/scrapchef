package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.LocalScrapChefTokens
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DietaryPreferencesScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val dietaryOptions = listOf(
        "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Halal", "Kosher", "No pork", "No beef", "No shellfish", "No nuts"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dietary Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(tokens.Spacing.lg)
        ) {
            Text(
                text = "Select your dietary requirements. We will only generate recipes that match your preferences.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = tokens.Spacing.lg)
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                dietaryOptions.forEach { option ->
                    val isSelected = uiState.dietaryPreferences.contains(option)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleDietaryPreference(option) },
                        label = { Text(option) },
                        shape = RoundedCornerShape(tokens.Radius.sm)
                    )
                }
            }
        }
    }
}
