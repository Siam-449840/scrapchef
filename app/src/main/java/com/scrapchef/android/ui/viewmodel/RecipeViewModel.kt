package com.scrapchef.android.ui.viewmodel

import android.app.Application
import com.scrapchef.android.util.ScrapChefLogger
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scrapchef.android.data.local.AppDatabase
import com.scrapchef.android.data.model.FavoriteRecipeEntity
import com.scrapchef.android.data.model.Recipe
import com.scrapchef.android.data.model.RecipeError
import com.scrapchef.android.data.model.RecipeErrorType
import com.scrapchef.android.data.model.Tool
import com.scrapchef.android.data.remote.AiGenerationException
import com.scrapchef.android.data.remote.AiTimeoutException
import com.scrapchef.android.data.remote.ApiKeyInvalidException
import com.scrapchef.android.data.remote.NetworkException
import com.scrapchef.android.data.remote.RateLimitException
import com.scrapchef.android.data.remote.StreamingRecipeState
import com.scrapchef.android.data.repository.ScrapChefRepository
import com.scrapchef.android.data.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

@Immutable
data class RecipeUiState(
    val selectedIngredients: List<String> = emptyList(),
    val ingredientQuery: String = "",
    val searchResults: List<com.scrapchef.android.data.model.IngredientEntity> = emptyList(),
    val selectedTools: Set<Tool> = setOf(Tool.STOVETOP),
    val selectedMaxTimeMinutes: Int = 30,
    val isLoading: Boolean = false,
    val currentRecipe: Recipe? = null,
    val error: RecipeError? = null,
    val isCurrentRecipeFavorite: Boolean = false,
    val userMessage: String? = null,
    
    // Kitchen Mode State
    val kitchenModeCurrentStep: Int = 0,
    val kitchenModeTimerSeconds: Int? = null,
    val isKitchenModeTimerRunning: Boolean = false
)

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ScrapChefRepository(
        context = application,
        ingredientDao = db.ingredientDao(),
        favoriteDao = db.favoriteDao(),
        pantryDao = db.pantryDao()
    )
    private val settingsRepository = SettingsRepository(application)

    companion object {
        private const val TAG = "ScrapChef"
    }

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    /** Progressive streaming state for the Generating Screen (§10.6). */
    private val _streamingState = MutableStateFlow(StreamingRecipeState())
    val streamingState: StateFlow<StreamingRecipeState> = _streamingState.asStateFlow()

    private var generationJob: kotlinx.coroutines.Job? = null

    val favoritesFlow: StateFlow<List<FavoriteRecipeEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Pre-parsed favorites — eliminates repository access from Composables. */
    val parsedFavoritesFlow: StateFlow<List<Pair<FavoriteRecipeEntity, Recipe?>>> =
        repository.getFavorites()
            .map { entities ->
                entities.map { entity ->
                    entity to repository.parseRecipeFromJson(entity.recipeJson)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteFavorite(name: String) {
        viewModelScope.launch {
            repository.deleteFavoriteByName(name)
            _uiState.update {
                it.copy(userMessage = "Recipe removed from Favorites.")
            }
        }
    }

    // Settings — collected once at startup, used at generation time
    private val _dietaryPreferences = MutableStateFlow<Set<String>>(emptySet())
    private val _cuisineCountry = MutableStateFlow("")

    private val _queryFlow = MutableStateFlow("")

    init {
        observeQueryDebounce()
        viewModelScope.launch {
            settingsRepository.dietaryPreferencesFlow.collect { prefs ->
                _dietaryPreferences.value = prefs
            }
        }
        viewModelScope.launch {
            settingsRepository.countryFlow.collect { country ->
                _cuisineCountry.value = country
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeQueryDebounce() {
        viewModelScope.launch {
            _queryFlow.debounce(300).collect { query ->
                if (query.isBlank()) {
                    _uiState.update { it.copy(searchResults = emptyList()) }
                } else {
                    val results = repository.searchIngredients(query)
                    _uiState.update { it.copy(searchResults = results) }
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(ingredientQuery = query) }
        _queryFlow.value = query
    }

    fun selectIngredient(canonical: String) {
        val current = _uiState.value.selectedIngredients
        if (current.contains(canonical)) {
            updateQuery("")
            return
        }
        if (current.size >= 8) {
            _uiState.update { it.copy(userMessage = "Maximum 8 ingredients allowed.") }
            return
        }
        _uiState.update {
            it.copy(
                selectedIngredients = current + canonical,
                ingredientQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun removeIngredient(canonical: String) {
        _uiState.update {
            it.copy(selectedIngredients = it.selectedIngredients - canonical)
        }
    }

    fun toggleTool(tool: Tool) {
        _uiState.update { state ->
            val current = state.selectedTools
            val updated = if (current.contains(tool)) {
                if (current.size == 1) {
                    setOf(Tool.STOVETOP) // At least 1 tool always selected
                } else {
                    current - tool
                }
            } else {
                current + tool
            }
            state.copy(selectedTools = updated)
        }
    }

    fun setMaxTime(minutes: Int) {
        _uiState.update { it.copy(selectedMaxTimeMinutes = minutes) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun generateRecipe(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.selectedIngredients.size < 3) {
            _uiState.update {
                it.copy(error = RecipeError(RecipeErrorType.NEED_MORE_INGREDIENTS, "Please select at least 3 ingredients."))
            }
            return
        }
        if (state.selectedTools.isEmpty()) {
            _uiState.update {
                it.copy(error = RecipeError(RecipeErrorType.NO_TOOL_SELECTED, "Please select at least one cooking tool."))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        _streamingState.value = StreamingRecipeState()

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            if (!repository.isOnline()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = RecipeError(
                            RecipeErrorType.OFFLINE_NO_MATCH,
                            "You're offline. Please connect to the internet for AI-generated recipes."
                        )
                    )
                }
                return@launch
            }

            ScrapChefLogger.i(TAG) { "Online — starting SSE streaming generation" }
            onSuccess() // Navigate to generating screen immediately

            try {
                repository.generateRecipeStream(
                    userIngredients = state.selectedIngredients,
                    selectedTools = state.selectedTools,
                    maxTimeMinutes = state.selectedMaxTimeMinutes,
                    dietaryPreferences = _dietaryPreferences.value,
                    cuisineCountry = _cuisineCountry.value
                ).collect { streamState ->
                    _streamingState.value = streamState

                    if (streamState.error != null) {
                        ScrapChefLogger.e(TAG) { "Streaming error: ${streamState.error}" }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = RecipeError(
                                    RecipeErrorType.AI_GENERATION_FAILED,
                                    streamState.error
                                )
                            )
                        }
                        return@collect
                    }

                    if (streamState.isComplete) {
                        val recipe = streamState.toRecipe()
                        if (recipe != null) {
                            val isFav = repository.isFavorite(recipe.name)
                            ScrapChefLogger.i(TAG) { "Streaming complete: '${recipe.name}' with ${recipe.effectiveInstructions.size} steps" }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentRecipe = recipe,
                                    isCurrentRecipeFavorite = isFav
                                )
                            }
                        } else {
                            ScrapChefLogger.e(TAG) { "Streaming complete but recipe parse failed" }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = RecipeError(
                                        RecipeErrorType.AI_GENERATION_FAILED,
                                        "We couldn't quite get this one right. Want to try again?"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                ScrapChefLogger.e(TAG, e) { "Stream collection failed: ${e.message}" }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = classifyException(e)
                    )
                }
            }
        }
    }

    private fun classifyException(error: Throwable?): RecipeError {
        return when (error) {
            is ApiKeyInvalidException -> RecipeError(
                RecipeErrorType.API_KEY_INVALID,
                error.message ?: "API key is invalid."
            )
            is AiTimeoutException -> RecipeError(
                RecipeErrorType.API_TIMEOUT,
                error.message ?: "Request timed out."
            )
            is NetworkException -> RecipeError(
                RecipeErrorType.NETWORK_ERROR,
                error.message ?: "Network error."
            )
            is RateLimitException -> RecipeError(
                RecipeErrorType.AI_GENERATION_FAILED,
                error.message ?: "Rate limit exceeded. Please wait and try again."
            )
            is AiGenerationException -> RecipeError(
                RecipeErrorType.AI_GENERATION_FAILED,
                error.message ?: "AI generation failed."
            )
            else -> RecipeError(
                RecipeErrorType.AI_GENERATION_FAILED,
                error?.message ?: "An unexpected error occurred. Please try again."
            )
        }
    }

    fun cancelGeneration() {
        if (generationJob?.isActive == true) {
            generationJob?.cancel()
            generationJob = null
            ScrapChefLogger.i(TAG) { "Recipe generation cancelled by user" }
            _streamingState.update { it.copy(isComplete = true) } // Stop UI loading
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleCurrentFavorite() {
        val recipe = _uiState.value.currentRecipe ?: return
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(recipe)
            _uiState.update {
                it.copy(
                    isCurrentRecipeFavorite = isNowFav,
                    userMessage = if (isNowFav) "Recipe saved to Favorites!" else "Recipe removed from Favorites."
                )
            }
        }
    }

    fun toggleFavoriteFromList(recipe: Recipe) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(recipe)
            _uiState.update {
                it.copy(
                    userMessage = if (isNowFav) "Recipe saved to Favorites!" else "Recipe removed from Favorites."
                )
            }
        }
    }

    fun setRecipeForDetail(recipe: Recipe) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(recipe.name)
            _uiState.update {
                it.copy(currentRecipe = recipe, isCurrentRecipeFavorite = isFav)
            }
        }
    }

    fun resetSelections() {
        _uiState.update {
            it.copy(
                selectedIngredients = emptyList(),
                ingredientQuery = "",
                searchResults = emptyList(),
                selectedTools = setOf(Tool.STOVETOP),
                selectedMaxTimeMinutes = 30,
                error = null,
                currentRecipe = null,
                kitchenModeCurrentStep = 0,
                kitchenModeTimerSeconds = null,
                isKitchenModeTimerRunning = false
            )
        }
    }

    fun setKitchenModeStep(stepIndex: Int) {
        _uiState.update { it.copy(kitchenModeCurrentStep = stepIndex) }
    }

    fun setKitchenModeTimer(seconds: Int?) {
        _uiState.update { it.copy(kitchenModeTimerSeconds = seconds, isKitchenModeTimerRunning = false) }
    }

    fun toggleKitchenModeTimer() {
        _uiState.update { it.copy(isKitchenModeTimerRunning = !it.isKitchenModeTimerRunning) }
    }

    fun tickKitchenModeTimer() {
        _uiState.update { state ->
            if (state.isKitchenModeTimerRunning && state.kitchenModeTimerSeconds != null && state.kitchenModeTimerSeconds > 0) {
                state.copy(kitchenModeTimerSeconds = state.kitchenModeTimerSeconds - 1)
            } else if (state.kitchenModeTimerSeconds == 0) {
                state.copy(isKitchenModeTimerRunning = false)
            } else {
                state
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
