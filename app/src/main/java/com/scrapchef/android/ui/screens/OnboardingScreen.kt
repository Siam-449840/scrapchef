package com.scrapchef.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrapchef.android.R
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.viewmodel.SettingsViewModel

private val COUNTRIES = listOf(
    "United States" to "US",
    "United Kingdom" to "GB",
    "Canada" to "CA",
    "Australia" to "AU",
    "India" to "IN",
    "France" to "FR",
    "Germany" to "DE",
    "Japan" to "JP",
    "Mexico" to "MX",
    "Brazil" to "BR"
)

private val LANGUAGES = listOf(
    "English", "Spanish", "French", "German", "Hindi", "Japanese", "Portuguese"
)

private val DIETARY_OPTIONS = listOf(
    "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Halal", "Kosher", "No pork", "No beef", "No shellfish", "No nuts"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }
    val tokens = LocalScrapChefTokens.current
    val uiState by viewModel.uiState.collectAsState()

    // Default to first item if none selected
    var selectedCountry by remember { mutableStateOf(if (uiState.country.isNotEmpty()) uiState.country else "US") }
    var selectedLanguage by remember { mutableStateOf(if (uiState.language.isNotEmpty()) uiState.language else "English") }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
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
                .padding(horizontal = tokens.Spacing.lg)
        ) {
            // Progress Indicator
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = tokens.Spacing.sm),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(tokens.Spacing.lg))

            when (currentStep) {
                1 -> {
                    // Step 1: Country
                    Text(
                        text = stringResource(R.string.onboarding_where_do_you_cook),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = tokens.Spacing.md)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_countries)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = tokens.Spacing.md),
                        singleLine = true
                    )

                    val filteredCountries = COUNTRIES.filter { it.first.contains(searchQuery, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredCountries) { (name, code) ->
                            val isSelected = selectedCountry == code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCountry = code }
                                    .padding(vertical = tokens.Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Step 2: Language
                    Text(
                        text = stringResource(R.string.onboarding_what_language),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = tokens.Spacing.md)
                    )
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(LANGUAGES) { language ->
                            val isSelected = selectedLanguage == language
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLanguage = language }
                                    .padding(vertical = tokens.Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Step 3: Dietary Preferences
                    Text(
                        text = stringResource(R.string.onboarding_avoid_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = tokens.Spacing.sm)
                    )
                    Text(
                        text = stringResource(R.string.onboarding_dietary_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = tokens.Spacing.lg)
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(tokens.Spacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DIETARY_OPTIONS.forEach { option ->
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

            // Bottom Actions
            Column(modifier = Modifier.padding(vertical = tokens.Spacing.lg)) {
                Button(
                    onClick = {
                        if (currentStep == 1) {
                            viewModel.setCountry(selectedCountry)
                            currentStep++
                        } else if (currentStep == 2) {
                            viewModel.setLanguage(selectedLanguage)
                            currentStep++
                        } else {
                            viewModel.completeOnboarding()
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentStep == 3) stringResource(R.string.action_start_cooking) else stringResource(R.string.action_continue))
                }
                
                if (currentStep < 3) {
                    Text(
                        text = stringResource(R.string.onboarding_change_later),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = tokens.Spacing.sm)
                    )
                } else {
                    TextButton(
                        onClick = {
                            viewModel.completeOnboarding()
                            onFinishOnboarding()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_skip_for_now))
                    }
                }
            }
        }
    }
}
