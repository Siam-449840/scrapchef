package com.scrapchef.android.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.scrapchef.android.data.model.RecipeErrorType
import com.scrapchef.android.data.model.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeViewModelTest {

    private lateinit var viewModel: RecipeViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = RecipeViewModel(application)
    }

    @Test
    fun `generateRecipe with 0 ingredients sets NEED_MORE_INGREDIENTS error`() {
        // clear ingredients initially just to be safe
        viewModel.resetSelections()
        
        viewModel.generateRecipe {}
        
        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.NEED_MORE_INGREDIENTS, error?.type)
    }

    @Test
    fun `generateRecipe with 1 ingredient sets NEED_MORE_INGREDIENTS error`() {
        viewModel.resetSelections()
        viewModel.selectIngredient("Tomato")
        viewModel.generateRecipe {}
        
        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.NEED_MORE_INGREDIENTS, error?.type)
    }

    @Test
    fun `generateRecipe with 2 ingredients sets NEED_MORE_INGREDIENTS error`() {
        viewModel.resetSelections()
        viewModel.selectIngredient("Tomato")
        viewModel.selectIngredient("Onion")
        viewModel.generateRecipe {}
        
        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.NEED_MORE_INGREDIENTS, error?.type)
    }

}
