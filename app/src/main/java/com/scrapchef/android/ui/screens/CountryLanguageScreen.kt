package com.scrapchef.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.viewmodel.SettingsViewModel

private val COUNTRIES = listOf(
    "United States" to "US", "United Kingdom" to "GB", "Canada" to "CA",
    "Australia" to "AU", "India" to "IN", "France" to "FR",
    "Germany" to "DE", "Japan" to "JP", "Mexico" to "MX", "Brazil" to "BR"
)

private val LANGUAGES = listOf(
    "English", "Spanish", "French", "German", "Hindi", "Japanese", "Portuguese"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryLanguageScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedCountry by remember { mutableStateOf(if (uiState.country.isNotEmpty()) uiState.country else "US") }
    var selectedLanguage by remember { mutableStateOf(if (uiState.language.isNotEmpty()) uiState.language else "English") }
    var searchQuery by remember { mutableStateOf("") }
    
    // 1 for Country, 2 for Language
    var tabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Country & Language") },
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
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Country") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Language") }
                )
            }
            
            Column(modifier = Modifier.fillMaxSize().padding(tokens.Spacing.lg)) {
                if (tabIndex == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search countries") },
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
                                    .clickable {
                                        selectedCountry = code
                                        viewModel.setCountry(code)
                                    }
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
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(LANGUAGES) { language ->
                            val isSelected = selectedLanguage == language
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLanguage = language
                                        viewModel.setLanguage(language)
                                    }
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
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
