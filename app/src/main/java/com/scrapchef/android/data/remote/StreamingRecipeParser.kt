package com.scrapchef.android.data.remote

import com.scrapchef.android.data.model.Recipe
import org.json.JSONObject

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
            // Only mark complete if JSON is structurally valid (balanced braces)
            isComplete = isJsonStructurallyComplete(cleaned)
        )
    }

    /** Extracts a quoted string value for a given key. */
    private fun extractString(json: String, key: String): String? {
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
     * Uses quote-aware scanning to avoid splitting on brackets inside string values.
     */
    private fun extractStringArray(json: String, key: String): List<String>? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val bracketStart = json.indexOf('[', keyIndex)
        if (bracketStart == -1) return null

        // Find matching close bracket with quote-awareness
        val bracketEnd = findArrayEnd(json, bracketStart)

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

    /**
     * Finds the closing bracket of a JSON array, skipping brackets inside quoted strings.
     * Returns -1 if no matching close bracket is found (array still streaming).
     */
    private fun findArrayEnd(json: String, from: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in from until json.length) {
            val c = json[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
        }
        return -1
    }

    /**
     * Checks whether the JSON string is structurally complete by attempting a full
     * JSONObject parse. This avoids premature completion from a stray '}' inside a string value.
     */
    private fun isJsonStructurallyComplete(json: String): Boolean {
        val trimmed = json.trimEnd()
        if (!trimmed.endsWith("}")) return false
        return try {
            JSONObject(trimmed)
            true
        } catch (_: Exception) {
            false
        }
    }
}
