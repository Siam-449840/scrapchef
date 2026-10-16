package com.scrapchef.android.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.scrapchef.android.data.local.FavoriteDao
import com.scrapchef.android.data.local.IngredientDao
import com.scrapchef.android.data.local.PantryDao
import com.scrapchef.android.data.model.ALLOWED_PANTRY_STAPLES
import com.scrapchef.android.data.model.FavoriteRecipeEntity
import com.scrapchef.android.data.model.IngredientEntity
import com.scrapchef.android.data.model.Recipe
import com.scrapchef.android.data.model.Tool
import com.scrapchef.android.data.remote.PuterRepository
import com.scrapchef.android.data.remote.StreamingRecipeState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ScrapChefRepository(
    private val context: Context,
    private val ingredientDao: IngredientDao,
    private val favoriteDao: FavoriteDao,
    private val pantryDao: PantryDao,
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
    private val puterRepository: PuterRepository = PuterRepository(
        tokenProvider = { settingsRepository.puterAuthTokenFlow.firstOrNull() }
    )
) {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(listType)
    private val recipeAdapter = moshi.adapter(Recipe::class.java)

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun searchIngredients(query: String): List<IngredientEntity> {
        if (query.isBlank()) return emptyList()
        return ingredientDao.searchIngredients(query)
    }

    @Deprecated("Static offline database is banned per PRD §0.1. Will always return null.")
    suspend fun findStaticMatch(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int
    ): Recipe? {
        return null
    }

    // Pantry Management
    fun getPantryItems(): Flow<List<com.scrapchef.android.data.model.PantryItemEntity>> = pantryDao.getAllPantryItems()

    suspend fun addPantryItem(name: String, category: String) {
        pantryDao.insertPantryItem(com.scrapchef.android.data.model.PantryItemEntity(name = name, category = category))
    }

    suspend fun removePantryItem(name: String) {
        pantryDao.deletePantryItemByName(name)
    }

    /**
     * Calls the Gemini API to generate a recipe.
     * Returns Result.success with the recipe, or Result.failure with a typed exception.
     * Does NOT fall back to any hardcoded recipe — errors are propagated to the ViewModel.
     */
    suspend fun generateRecipeWithAi(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): Result<Recipe> {
        return puterRepository.generateRecipe(
            userIngredients, selectedTools, maxTimeMinutes,
            dietaryPreferences, cuisineCountry
        )
    }

    /**
     * Streaming variant — returns a Flow that emits progressive recipe state.
     * Per §10.6, this is the primary generation path for the Generating Screen.
     */
    fun generateRecipeStream(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): kotlinx.coroutines.flow.Flow<StreamingRecipeState> {
        return puterRepository.generateRecipeStream(
            userIngredients, selectedTools, maxTimeMinutes,
            dietaryPreferences, cuisineCountry
        )
    }

    // generateFallbackRecipe() has been removed.
    // No hardcoded/generic recipes will ever be shown.

    fun getFavorites(): Flow<List<FavoriteRecipeEntity>> = favoriteDao.getAllFavorites()

    suspend fun isFavorite(name: String): Boolean {
        return favoriteDao.getFavoriteByName(name) != null
    }

    suspend fun toggleFavorite(recipe: Recipe): Boolean {
        val existing = favoriteDao.getFavoriteByName(recipe.name)
        return if (existing != null) {
            favoriteDao.deleteFavoriteByName(recipe.name)
            false
        } else {
            val json = recipeAdapter.toJson(recipe)
            // Use upsertByName to prevent duplicate favorites from concurrent toggles
            favoriteDao.upsertFavoriteByName(
                FavoriteRecipeEntity(name = recipe.name, recipeJson = json)
            )
            true
        }
    }

    fun parseRecipeFromJson(json: String): Recipe? {
        return try {
            recipeAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFavoriteByName(name: String) {
        favoriteDao.deleteFavoriteByName(name)
    }
}
