package com.example.data.remote

import com.example.data.model.Recipe

/**
 * Represents the progressive state of a recipe being streamed from the AI.
 * Fields become non-null as they are parsed from the accumulating JSON.
 * Per §10.6, the reveal order is: name → time/tools → yourIngredients → pantryAssumptions → instructions → tips.
 */
data class StreamingRecipeState(
    val rawAccumulated: String = "",
    val name: String? = null,
    val description: String? = null,
    val difficulty: String? = null,
    val servings: Int? = null,
    val totalTimeMinutes: Int? = null,
    val tools: List<String>? = null,
    val yourIngredients: List<String>? = null,
    val pantryAssumptions: List<String>? = null,
    val instructions: List<String> = emptyList(),
    val tips: List<String>? = null,
    val notes: List<String>? = null,
    val warnings: List<String>? = null,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    /** Approximate completion percentage for the cancel-confirm threshold (§10.6). */
    val progressPercent: Float
        get() {
            var filled = 0
            if (name != null) filled++
            if (totalTimeMinutes != null) filled++
            if (yourIngredients != null) filled++
            if (pantryAssumptions != null) filled++
            if (instructions.isNotEmpty()) filled++
            if (tips != null) filled++
            return filled / 6f
        }

    fun toRecipe(): Recipe? {
        if (name.isNullOrBlank() || instructions.isEmpty()) return null
        return Recipe(
            name = name,
            description = description ?: "",
            difficulty = difficulty ?: "Medium",
            servings = servings ?: 2,
            totalTimeMinutes = totalTimeMinutes ?: 30,
            tools = tools ?: emptyList(),
            yourIngredients = yourIngredients ?: emptyList(),
            pantryAssumptions = pantryAssumptions ?: emptyList(),
            instructions = instructions,
            tips = tips ?: emptyList(),
            notes = notes ?: emptyList(),
            warnings = warnings ?: emptyList()
        )
    }
}

/**
 * Incrementally parses a JSON string being built token-by-token from an SSE stream.
 * Uses simple regex extraction rather than a full streaming JSON parser — robust enough
 * for the well-structured output our prompt guarantees.
 */
object StreamingRecipeParser {

    fun parse(accumulated: String): StreamingRecipeState {
        val cleaned = accumulated
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return StreamingRecipeState(
            rawAccumulated = accumulated,
            name = extractString(cleaned, "name"),
            description = extractString(cleaned, "description"),
            difficulty = extractString(cleaned, "difficulty"),
            servings = extractInt(cleaned, "servings"),
            totalTimeMinutes = extractInt(cleaned, "totalTimeMinutes"),
            tools = extractStringArray(cleaned, "tools"),
            yourIngredients = extractStringArray(cleaned, "yourIngredients"),
            pantryAssumptions = extractStringArray(cleaned, "pantryAssumptions"),
            instructions = extractStringArray(cleaned, "instructions") ?: emptyList(),
            tips = extractStringArray(cleaned, "tips"),
            notes = extractStringArray(cleaned, "notes"),
            warnings = extractStringArray(cleaned, "warnings"),
            isComplete = cleaned.trimEnd().endsWith("}")
        )
    }

    /** Extracts a quoted string value for a given key. */
    private fun extractString(json: String, key: String): String? {
        // Match "key": "value" — handles escaped quotes inside value
        val pattern = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return pattern.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\n", "\n")
    }

    /** Extracts an integer value for a given key. */
    private fun extractInt(json: String, key: String): Int? {
        val pattern = Regex(""""$key"\s*:\s*(\d+)""")
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Extracts a JSON string array for a given key.
     * Returns null if the key hasn't appeared yet.
     * Returns a partial list if the array is still being streamed.
     */
    private fun extractStringArray(json: String, key: String): List<String>? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val bracketStart = json.indexOf('[', keyIndex)
        if (bracketStart == -1) return null

        // Find matching close bracket, handling nesting
        val bracketEnd = json.indexOf(']', bracketStart)

        val arrayContent = if (bracketEnd != -1) {
            json.substring(bracketStart + 1, bracketEnd)
        } else {
            // Array is still being streamed — take what we have
            json.substring(bracketStart + 1)
        }

        if (arrayContent.isBlank()) return emptyList()

        // Extract all complete quoted strings from the array content
        val itemPattern = Regex(""""((?:[^"\\]|\\.)*)"""")
        return itemPattern.findAll(arrayContent).map {
            it.groupValues[1]
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
        }.toList()
    }
}
