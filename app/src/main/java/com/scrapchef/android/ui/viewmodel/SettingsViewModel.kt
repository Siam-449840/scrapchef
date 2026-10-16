package com.scrapchef.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scrapchef.android.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dietaryPreferences: Set<String> = emptySet(),
    val isOnboardingCompleted: Boolean = false,
    val country: String = "",
    val language: String = "",
    val hasPuterAuthToken: Boolean = false,
    val maskedPuterToken: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dietaryPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(dietaryPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            repository.onboardingCompletedFlow.collect { completed ->
                _uiState.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
        viewModelScope.launch {
            repository.countryFlow.collect { country ->
                _uiState.update { it.copy(country = country) }
            }
        }
        viewModelScope.launch {
            repository.languageFlow.collect { language ->
                _uiState.update { it.copy(language = language) }
            }
        }
        viewModelScope.launch {
            repository.puterAuthTokenFlow.collect { token ->
                val hasToken = !token.isNullOrBlank()
                val masked = if (hasToken && token!!.length > 8) {
                    "${token.take(4)}...${token.takeLast(4)}"
                } else if (hasToken) {
                    "****"
                } else {
                    ""
                }
                _uiState.update {
                    it.copy(
                        hasPuterAuthToken = hasToken,
                        maskedPuterToken = masked
                    )
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
        }
    }

    fun toggleDietaryPreference(preference: String) {
        viewModelScope.launch {
            // Read current prefs inside the coroutine to avoid lost-update race
            val currentPrefs = _uiState.value.dietaryPreferences
            val updatedPrefs = if (currentPrefs.contains(preference)) {
                currentPrefs - preference
            } else {
                currentPrefs + preference
            }
            repository.setDietaryPreferences(updatedPrefs)
        }
    }

    fun setCountry(country: String) {
        viewModelScope.launch {
            repository.setCountry(country)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setPuterAuthToken(token: String) {
        viewModelScope.launch {
            repository.setPuterAuthToken(token)
        }
    }

    fun clearPuterAuthToken() {
        viewModelScope.launch {
            repository.setPuterAuthToken(null)
        }
    }
}
