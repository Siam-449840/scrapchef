package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val DIETARY_PREFS = stringSetPreferencesKey("dietary_preferences")
    private val ONBOARDING_COMPLETED = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")
    private val COUNTRY = stringPreferencesKey("country")
    private val LANGUAGE = stringPreferencesKey("language")

    val dietaryPreferencesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[DIETARY_PREFS] ?: emptySet()
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val countryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[COUNTRY] ?: ""
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: ""
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { settings ->
            settings[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDietaryPreferences(preferences: Set<String>) {
        context.dataStore.edit { settings ->
            settings[DIETARY_PREFS] = preferences
        }
    }

    suspend fun setCountry(country: String) {
        context.dataStore.edit { settings ->
            settings[COUNTRY] = country
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { settings ->
            settings[LANGUAGE] = language
        }
    }
}
