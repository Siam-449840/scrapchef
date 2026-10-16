package com.scrapchef.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens
import com.scrapchef.android.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SettingsViewModel,
    onNavigateToDietaryPrefs: () -> Unit,
    onNavigateToCountryLang: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    var showPuterDialog by remember { mutableStateOf(false) }
    var inputToken by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(tokens.IconSize.lg)
                        )
                        Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                        Text(
                            text = "Profile & Settings",
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
        bottomBar = bottomBar,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .widthIn(max = tokens.Content.maxWidth)
                .padding(tokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.Spacing.lg)
        ) {
            

            // Preferences Section
            ProfileSection("Preferences") {
                ProfileSettingRow(
                    icon = Icons.Default.RestaurantMenu,
                    title = "Dietary Preferences",
                    subtitle = "Vegetarian, Vegan, Gluten-Free, etc.",
                    onClick = onNavigateToDietaryPrefs,
                    testTag = "setting_dietary_prefs"
                )
            }

            // App Settings Section
            ProfileSection("App Settings") {
                val countryDisplay = if (uiState.country.isNotEmpty()) uiState.country else "US"
                val langDisplay = if (uiState.language.isNotEmpty()) uiState.language else "English"
                ProfileSettingRow(
                    icon = Icons.Default.Language,
                    title = "Country & Language",
                    subtitle = "$countryDisplay • $langDisplay",
                    onClick = onNavigateToCountryLang
                )
            }

            // Puter AI Account Section (User-Pays Model)
            ProfileSection("AI Engine & Account") {
                val puterStatus = if (uiState.hasPuterAuthToken) {
                    "Connected (${uiState.maskedPuterToken})"
                } else {
                    "Not Connected • Connect Puter Account"
                }
                ProfileSettingRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Puter AI Account",
                    subtitle = puterStatus,
                    onClick = {
                        inputToken = ""
                        showPuterDialog = true
                    },
                    testTag = "setting_puter_account"
                )
            }
        }
    }

    if (showPuterDialog) {
        AlertDialog(
            onDismissRequest = { showPuterDialog = false },
            title = {
                Text(
                    text = "Puter AI Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.Spacing.md)) {
                    Text(
                        text = "ScrapChef uses Puter AI under the User-Pays model. Connecting your personal Puter account provides dedicated usage quotas without shared API keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Puter Auth Token") },
                        placeholder = { Text("Paste your token here") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://puter.com/"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Token from puter.com")
                    }

                    if (uiState.hasPuterAuthToken) {
                        TextButton(
                            onClick = {
                                viewModel.clearPuterAuthToken()
                                showPuterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect Current Account", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputToken.isNotBlank()) {
                            viewModel.setPuterAuthToken(inputToken.trim())
                        }
                        showPuterDialog = false
                    },
                    enabled = inputToken.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPuterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    val tokens = LocalScrapChefTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = tokens.Spacing.sm)
        )
        Card(
            shape = RoundedCornerShape(tokens.Radius.md),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = tokens.Elevation.none),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val tokens = LocalScrapChefTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(tokens.Spacing.md)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(tokens.IconSize.md)
        )
        Spacer(modifier = Modifier.width(tokens.Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
