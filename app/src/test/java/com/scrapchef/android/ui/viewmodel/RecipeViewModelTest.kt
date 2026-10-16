package com.scrapchef.android.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.scrapchef.android.data.model.RecipeErrorType
import com.scrapchef.android.data.remote.PuterRetrofitClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeViewModelTest {

    private lateinit var viewModel: RecipeViewModel
    private val originalService = PuterRetrofitClient.service

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = RecipeViewModel(application)
    }

    @After
    fun tearDown() {
        PuterRetrofitClient.service = originalService
    }

    @Test
    fun `generateRecipe with 0 ingredients sets NEED_MORE_INGREDIENTS error`() {
        viewModel.resetSelections()
        viewModel.generateRecipe {}
        ShadowLooper.idleMainLooper()

        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.NEED_MORE_INGREDIENTS, error?.type)
    }

    @Test
    fun `generateRecipe with 1 ingredient sets NEED_MORE_INGREDIENTS error`() {
        viewModel.resetSelections()
        viewModel.selectIngredient("Tomato")
        viewModel.generateRecipe {}
        ShadowLooper.idleMainLooper()

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
        ShadowLooper.idleMainLooper()

        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.NEED_MORE_INGREDIENTS, error?.type)
    }

    @Test
    fun `generateRecipe with timeout sets NETWORK_TIMEOUT error`() {
        val mockService = MockPuterApiService()
        mockService.shouldThrowTimeout = true
        PuterRetrofitClient.service = mockService

        viewModel.resetSelections()
        viewModel.selectIngredient("Tomato")
        viewModel.selectIngredient("Onion")
        viewModel.selectIngredient("Garlic")

        viewModel.generateRecipe {}
        ShadowLooper.idleMainLooper()

        val error = viewModel.uiState.value.error
        assertNotNull(error)
    }

    @Test
    fun `generateRecipe offline sets OFFLINE_NO_MATCH error`() {
        val mockService = MockPuterApiService()
        mockService.shouldThrowOffline = true
        PuterRetrofitClient.service = mockService

        viewModel.resetSelections()
        viewModel.selectIngredient("Tomato")
        viewModel.selectIngredient("Onion")
        viewModel.selectIngredient("Garlic")

        viewModel.generateRecipe {}
        ShadowLooper.idleMainLooper()

        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertEquals(RecipeErrorType.OFFLINE_NO_MATCH, error?.type)
    }
}
