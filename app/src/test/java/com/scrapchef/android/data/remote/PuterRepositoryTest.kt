package com.scrapchef.android.data.remote

import com.scrapchef.android.data.model.Tool
import com.scrapchef.android.ui.viewmodel.MockPuterApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PuterRepositoryTest {

    private lateinit var mockService: MockPuterApiService
    private lateinit var repository: PuterRepository

    @Before
    fun setup() {
        mockService = MockPuterApiService()
        repository = PuterRepository(
            apiService = mockService,
            tokenProvider = { "valid_test_token" }
        )
    }

    @Test
    fun generateRecipe_success_parsesValidJson() = runBlocking {
        val result = repository.generateRecipe(
            userIngredients = listOf("Tomato", "Onion", "Garlic"),
            selectedTools = setOf(Tool.STOVETOP),
            maxTimeMinutes = 30
        )

        assertTrue(result.isSuccess)
        val recipe = result.getOrNull()
        assertNotNull(recipe)
        assertEquals("Skillet Tomato & Onion Sauté", recipe?.name)
    }

    @Test
    fun generateRecipe_authError_returnsPuterAuthException() = runBlocking {
        mockService.shouldThrowAuthError = true
        val result = repository.generateRecipe(
            userIngredients = listOf("Tomato", "Onion", "Garlic"),
            selectedTools = setOf(Tool.STOVETOP),
            maxTimeMinutes = 30
        )

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is PuterAuthException)
    }

    @Test
    fun generateRecipe_quotaError_returnsPuterQuotaException() = runBlocking {
        mockService.shouldThrowQuotaError = true
        val result = repository.generateRecipe(
            userIngredients = listOf("Tomato", "Onion", "Garlic"),
            selectedTools = setOf(Tool.STOVETOP),
            maxTimeMinutes = 30
        )

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is PuterQuotaException)
    }

    @Test
    fun generateRecipe_rateLimitError_returnsPuterRateLimitException() = runBlocking {
        mockService.shouldThrowRateLimitError = true
        val result = repository.generateRecipe(
            userIngredients = listOf("Tomato", "Onion", "Garlic"),
            selectedTools = setOf(Tool.STOVETOP),
            maxTimeMinutes = 30
        )

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is PuterRateLimitException)
    }
}
