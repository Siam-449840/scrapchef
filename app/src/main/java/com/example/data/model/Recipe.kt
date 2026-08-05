package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

val ALLOWED_PANTRY_STAPLES = listOf("salt", "black pepper", "water", "vegetable oil", "olive oil", "canola oil")

@JsonClass(generateAdapter = true)
data class Recipe(
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "totalTimeMinutes") val totalTimeMinutes: Int = 0,
    @Json(name = "tools") val tools: List<String> = emptyList(),
    
    // Optional AI additions for UI
    @Json(name = "difficulty") val difficulty: String? = null,
    @Json(name = "servings") val servings: Int? = null,

    @Json(name = "yourIngredients") val yourIngredients: List<String> = emptyList(),
    @Json(name = "pantryAssumptions") val pantryAssumptions: List<String> = emptyList(),
    @Json(name = "instructions") val instructions: List<String> = emptyList(),
    @Json(name = "tips") val tips: List<String> = emptyList(),
    @Json(name = "notes") val notes: List<String> = emptyList(),
    @Json(name = "warnings") val warnings: List<String> = emptyList(),
    // Legacy field — kept for backward compatibility with old favorites JSON
    @Json(name = "steps") val steps: List<String> = emptyList(),
    @Json(name = "tip") val tip: String? = null
) {
    /**
     * Returns the effective instruction list.
     * Prefers 'instructions' if populated, falls back to legacy 'steps'.
     */
    val effectiveInstructions: List<String>
        get() = instructions.ifEmpty { steps }

    /**
     * Returns the effective tips list.
     * Combines new 'tips' list with legacy single 'tip' if present.
     */
    val effectiveTips: List<String>
        get() {
            val combined = tips.toMutableList()
            if (!tip.isNullOrBlank() && tip !in combined) {
                combined.add(tip)
            }
            return combined
        }
}

enum class RecipeErrorType {
    NEED_MORE_INGREDIENTS,
    NO_TOOL_SELECTED,
    OFFLINE_NO_MATCH,
    NO_RECIPE_POSSIBLE,
    NETWORK_ERROR,
    AI_GENERATION_FAILED,
    API_KEY_INVALID,
    API_TIMEOUT
}

data class RecipeError(
    val type: RecipeErrorType,
    val message: String
)
