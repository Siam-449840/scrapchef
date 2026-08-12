package com.scrapchef.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scrapchef.android.util.ScrapChefLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val DIETARY_PREFS = stringSetPreferencesKey("dietary_preferences")
    private val ONBOARDING_COMPLETED = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")
    private val COUNTRY = stringPreferencesKey("country")
    private val LANGUAGE = stringPreferencesKey("language")

    companion object {
        private const val TAG = "ScrapChef"
    }

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
        try {
            context.dataStore.edit { settings ->
                settings[ONBOARDING_COMPLETED] = completed
            }
        } catch (e: IOException) {
            ScrapChefLogger.e(TAG, e) { "Failed to save onboarding status" }
        }
    }

    suspend fun setDietaryPreferences(preferences: Set<String>) {
        try {
            context.dataStore.edit { settings ->
                settings[DIETARY_PREFS] = preferences
            }
        } catch (e: IOException) {
            ScrapChefLogger.e(TAG, e) { "Failed to save dietary preferences" }
        }
    }

    suspend fun setCountry(country: String) {
        try {
            context.dataStore.edit { settings ->
                settings[COUNTRY] = country
            }
        } catch (e: IOException) {
            ScrapChefLogger.e(TAG, e) { "Failed to save country" }
        }
    }

    suspend fun setLanguage(language: String) {
        try {
            context.dataStore.edit { settings ->
                settings[LANGUAGE] = language
            }
        } catch (e: IOException) {
            ScrapChefLogger.e(TAG, e) { "Failed to save language" }
        }
    }
}
