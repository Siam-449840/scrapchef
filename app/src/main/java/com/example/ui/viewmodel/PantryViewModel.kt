package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.IngredientEntity
import com.example.data.model.PantryItemEntity
import com.example.data.repository.PantryPalRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PantryUiState(
    val inventory: List<PantryItemEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<IngredientEntity> = emptyList()
)

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PantryPalRepository(
        context = application,
        ingredientDao = db.ingredientDao(),
        favoriteDao = db.favoriteDao(),
        pantryDao = db.pantryDao()
    )

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.getPantryItems().collect { items ->
                _uiState.update { it.copy(inventory = items) }
            }
        }
        observeQueryDebounce()
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _queryFlow.value = query
    }

    fun addIngredientToPantry(ingredient: IngredientEntity) {
        viewModelScope.launch {
            repository.addPantryItem(ingredient.canonical, ingredient.category)
            updateSearchQuery("")
        }
    }

    fun removePantryItem(name: String) {
        viewModelScope.launch {
            repository.removePantryItem(name)
        }
    }
}
