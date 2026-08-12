package com.scrapchef.android.data.remote

import com.scrapchef.android.util.ScrapChefLogger
import com.scrapchef.android.BuildConfig
import com.scrapchef.android.data.model.Recipe
import com.scrapchef.android.data.model.Tool
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OpenRouterRepository(
    private val apiService: OpenRouterApiService = OpenRouterRetrofitClient.service
) {
    private val moshi = Moshi.Builder().build()
    private val recipeAdapter = moshi.adapter(Recipe::class.java)

    companion object {
        private const val TAG = "ScrapChef"
    }

    suspend fun generateRecipe(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): Result<Recipe> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_OPENROUTER_API_KEY") {
            ScrapChefLogger.e(TAG) { "OpenRouter API key is not configured." }
            return@withContext Result.failure(
                ApiKeyInvalidException("OpenRouter API key is not configured. Please set a valid key in .env file.")
            )
        }

        val model = BuildConfig.OPENROUTER_MODEL.ifBlank { "google/gemma-4-26b-a4b-it:free" }
        ScrapChefLogger.i(TAG) { "Using OpenRouter model: $model" }

        val toolNames = selectedTools.map { it.displayName }.joinToString(", ")
        val toolIds = selectedTools.map { it.id }.joinToString(", ")
        val timeConstraintStr = if (maxTimeMinutes > 0) "$maxTimeMinutes minutes max" else "No time limit"

        val dietaryClause = if (dietaryPreferences.isNotEmpty()) {
            "\n8. DIETARY REQUIREMENTS: This recipe MUST be strictly ${dietaryPreferences.joinToString(" and ")}. Do not include any ingredient that violates these restrictions."
        } else ""
        val cuisineClause = if (cuisineCountry.isNotBlank()) {
            "\n9. CUISINE PREFERENCE: Prioritize flavors, techniques, and ingredients authentic to or inspired by ${cuisineCountry} cuisine, while still using the user's provided ingredients."
        } else ""

        val systemPrompt = """
            You are a world-class international Master AI Chef for the Android app ScrapChef.
            Your goal is to craft the absolute best, authentic, or creative fusion recipe using ANY ingredients from anywhere in the world.
            
            You must use ONLY the user's provided ingredients plus these allowed basic pantry staples: salt, black pepper, water, vegetable oil (or olive/canola oil).
            
            STRICT RULES:
            1. Respect the user's ingredients completely — synthesize them into a delicious dish.
            2. Do NOT add unlisted grocery items outside the user's provided list and allowed pantry staples.
            3. Only use the allowed cooking tools: [$toolNames].
            4. Total time must not exceed: $timeConstraintStr.
            5. Return ONLY a valid JSON object with NO markdown formatting, no explanation, no extra text.
            6. Return as many preparation instructions as necessary — could be 3, 7, 12 or more. Do NOT force a fixed number.
            7. Include practical tips, notes, or warnings only when genuinely helpful. These lists can be empty if not needed.${'$'}dietaryClause${'$'}cuisineClause
            You MUST respond with ONLY this JSON format and nothing else:
            {
              "name": "Authentic / Creative Recipe Name",
              "description": "A brief 1-2 sentence description of the dish",
              "difficulty": "Easy / Medium / Hard",
              "servings": <number>,
              "totalTimeMinutes": <number>,
              "tools": ["$toolIds"],
              "yourIngredients": ["${userIngredients.joinToString("\", \"")}"],
              "pantryAssumptions": ["salt", "vegetable oil"],
              "instructions": [
                "Step 1: ...",
                "Step 2: ...",
                "... as many steps as needed ..."
              ],
              "tips": ["Optional pro tips — include only if genuinely useful"],
              "notes": ["Optional cooking notes — include only if helpful"],
              "warnings": ["Optional safety warnings — include only if relevant"]
            }
        """.trimIndent()

        val userPrompt = "User Ingredients: [${userIngredients.joinToString(", ")}]. Allowed Tools: [$toolNames]. Max Time: $timeConstraintStr. Respond with JSON only."

        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.7f,
            maxTokens = 4096
        )

        ScrapChefLogger.i(TAG) { "Sending OpenRouter API request | model=$model ingredients=$userIngredients tools=$toolNames maxTime=$timeConstraintStr" }
        val startTime = System.currentTimeMillis()

        // Attempt 1
        val result = executeApiAndParse(apiKey, request, userIngredients, selectedTools, maxTimeMinutes)
        val latency = System.currentTimeMillis() - startTime

        if (result.isSuccess) {
            ScrapChefLogger.i(TAG) { "OpenRouter API success | latency=${latency}ms | recipe='${result.getOrNull()?.name}'" }
            return@withContext result
        }

        val firstError = result.exceptionOrNull()
            ?: return@withContext Result.failure(Exception("OpenRouter API attempt 1 failed with no exception"))
        ScrapChefLogger.w(TAG) { "OpenRouter API attempt 1 failed | latency=${latency}ms | error=${firstError.message}" }

        // Don't retry rate limit, auth errors, or timeouts
        if (firstError is RateLimitException || firstError is ApiKeyInvalidException || firstError is AiTimeoutException) {
            return@withContext Result.failure(firstError)
        }

        // Attempt 2 — retry with backoff and a stronger prompt
        kotlinx.coroutines.delay(2000L)
        val retryRequest = request.copy(
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt + "\nIMPORTANT: Use STRICTLY ONLY user ingredients and allowed pantry staples. Return valid JSON only, no markdown."),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        ScrapChefLogger.i(TAG) { "Retrying OpenRouter API (attempt 2)" }
        val retryStart = System.currentTimeMillis()
        val retryResult = executeApiAndParse(apiKey, retryRequest, userIngredients, selectedTools, maxTimeMinutes)
        val retryLatency = System.currentTimeMillis() - retryStart

        if (retryResult.isSuccess) {
            ScrapChefLogger.i(TAG) { "OpenRouter API retry success | latency=${retryLatency}ms | recipe='${retryResult.getOrNull()?.name}'" }
            return@withContext retryResult
        }

        val finalError = retryResult.exceptionOrNull()
        ScrapChefLogger.e(TAG, finalError) { "OpenRouter API both attempts failed | error=${finalError?.message}" }

        return@withContext Result.failure(classifyError(finalError ?: firstError))
    }

    /**
     * Streaming variant of generateRecipe. Returns a Flow<StreamingRecipeState>
     * that emits progressively as tokens arrive via SSE.
     * Per §10.6: content streams in progressively — dish name first, then metadata,
     * then ingredients, then steps one at a time, then tips.
     */
    fun generateRecipeStream(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): Flow<StreamingRecipeState> = callbackFlow {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_OPENROUTER_API_KEY") {
            trySend(StreamingRecipeState(error = "OpenRouter API key is not configured."))
            close()
            return@callbackFlow
        }

        val model = BuildConfig.OPENROUTER_MODEL.ifBlank { "google/gemma-4-26b-a4b-it:free" }
        val toolNames = selectedTools.map { it.displayName }.joinToString(", ")
        val toolIds = selectedTools.map { it.id }.joinToString(", ")
        val timeConstraintStr = if (maxTimeMinutes > 0) "$maxTimeMinutes minutes max" else "No time limit"

        val dietaryClause = if (dietaryPreferences.isNotEmpty()) {
            "\n8. DIETARY REQUIREMENTS: This recipe MUST be strictly ${dietaryPreferences.joinToString(" and ")}. Do not include any ingredient that violates these restrictions."
        } else ""
        val cuisineClause = if (cuisineCountry.isNotBlank()) {
            "\n9. CUISINE PREFERENCE: Prioritize flavors, techniques, and ingredients authentic to or inspired by ${cuisineCountry} cuisine, while still using the user's provided ingredients."
        } else ""

        val systemPrompt = """
            You are a world-class international Master AI Chef for the Android app ScrapChef.
            Your goal is to craft the absolute best, authentic, or creative fusion recipe using ANY ingredients from anywhere in the world.
            
            You must use ONLY the user's provided ingredients plus these allowed basic pantry staples: salt, black pepper, water, vegetable oil (or olive/canola oil).
            
            STRICT RULES:
            1. Respect the user's ingredients completely — synthesize them into a delicious dish.
            2. Do NOT add unlisted grocery items outside the user's provided list and allowed pantry staples.
            3. Only use the allowed cooking tools: [$toolNames].
            4. Total time must not exceed: $timeConstraintStr.
            5. Return ONLY a valid JSON object with NO markdown formatting, no explanation, no extra text.
            6. Return as many preparation instructions as necessary — could be 3, 7, 12 or more. Do NOT force a fixed number.
            7. Include practical tips, notes, or warnings only when genuinely helpful. These lists can be empty if not needed.$dietaryClause$cuisineClause
            
            You MUST respond with ONLY this JSON format and nothing else:
            {
              "name": "Authentic / Creative Recipe Name",
              "description": "A brief 1-2 sentence description of the dish",
              "difficulty": "Easy / Medium / Hard",
              "servings": <number>,
              "totalTimeMinutes": <number>,
              "tools": ["$toolIds"],
              "yourIngredients": ["${userIngredients.joinToString("\", \"")}"],
              "pantryAssumptions": ["salt", "vegetable oil"],
              "instructions": [
                "Step 1: ...",
                "Step 2: ...",
                "... as many steps as needed ..."
              ],
              "tips": ["Optional pro tips — include only if genuinely useful"],
              "notes": ["Optional cooking notes — include only if helpful"],
              "warnings": ["Optional safety warnings — include only if relevant"]
            }
        """.trimIndent()

        val userPrompt = "User Ingredients: [${userIngredients.joinToString(", ")}]. Allowed Tools: [$toolNames]. Max Time: $timeConstraintStr. Respond with JSON only."

        // Build raw JSON body with stream: true
        val requestJson = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("temperature", 0.7)
            put("max_tokens", 4096)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val httpRequest = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://scrapchef.app")
            .addHeader("X-Title", "ScrapChef")
            .addHeader("Accept", "text/event-stream")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val accumulated = StringBuilder()
        var lastParseMs = 0L

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    // Final parse — always emit, validating structural integrity
                    val parsed = StreamingRecipeParser.parse(accumulated.toString())
                    if (parsed.name != null && parsed.instructions.isNotEmpty()) {
                        trySend(parsed.copy(isComplete = true))
                    } else {
                        trySend(parsed.copy(
                            isComplete = true,
                            error = "Recipe generation ended but the response was incomplete. Please try again."
                        ))
                    }
                    close()
                    return
                }
                try {
                    val chunk = JSONObject(data)

                    // Check for API-level error chunks (rate limits, provider errors, etc.)
                    val errorObj = chunk.optJSONObject("error")
                    if (errorObj != null) {
                        val errorMsg = errorObj.optString("message", "Streaming error")
                        val errorCode = errorObj.optInt("code", 0)
                        ScrapChefLogger.e(TAG) { "SSE error chunk: code=$errorCode message=$errorMsg" }
                        val userMsg = when (errorCode) {
                            429 -> "Rate limit exceeded. Please wait a moment and try again."
                            401, 403 -> "API key is invalid or unauthorized."
                            else -> "Generation failed: $errorMsg"
                        }
                        trySend(StreamingRecipeState(error = userMsg, rawAccumulated = accumulated.toString()))
                        close()
                        return
                    }

                    val delta = chunk
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content", "") ?: ""

                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        // Throttle parsing: at most every 100ms, or on [DONE]
                        val now = System.currentTimeMillis()
                        if (now - lastParseMs >= 100) {
                            lastParseMs = now
                            val state = StreamingRecipeParser.parse(accumulated.toString())
                            trySend(state)
                        }
                    }
                } catch (e: Exception) {
                    ScrapChefLogger.w(TAG) { "SSE chunk parse error: ${e.message}" }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // Classify HTTP errors from SSE endpoint
                val code = response?.code ?: 0
                val errorMsg = when (code) {
                    429 -> "Rate limit exceeded. Please wait a moment and try again."
                    401, 403 -> "API key is invalid or unauthorized."
                    503 -> "AI service is temporarily unavailable. Please try again."
                    else -> t?.message ?: response?.message ?: "Streaming failed"
                }
                ScrapChefLogger.e(TAG, t) { "SSE stream failure: code=$code message=$errorMsg" }
                val finalState = StreamingRecipeState(error = errorMsg, rawAccumulated = accumulated.toString())
                if (!trySend(finalState).isSuccess) {
                    ScrapChefLogger.e(TAG) { "SSE terminal state dropped (channel buffer full)" }
                }
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                if (!isClosedForSend) {
                    // Only mark complete if we have meaningful content
                    val parsed = StreamingRecipeParser.parse(accumulated.toString())
                    if (parsed.name != null && parsed.instructions.isNotEmpty()) {
                        val sent = trySend(parsed.copy(isComplete = true))
                        if (!sent.isSuccess) {
                            ScrapChefLogger.e(TAG) { "SSE onClosed state dropped (channel buffer full)" }
                        }
                    } else {
                        val sent = trySend(StreamingRecipeState(
                            error = "Connection ended before recipe was complete. Please try again.",
                            rawAccumulated = accumulated.toString()
                        ))
                        if (!sent.isSuccess) {
                            ScrapChefLogger.e(TAG) { "SSE onClosed error state dropped (channel buffer full)" }
                        }
                    }
                    close()
                }
            }
        }

        val eventSource = EventSources.createFactory(OpenRouterRetrofitClient.sseClient)
            .newEventSource(httpRequest, listener)

        ScrapChefLogger.i(TAG) { "SSE stream started | model=$model ingredients=$userIngredients" }



        awaitClose {
            eventSource.cancel()
            ScrapChefLogger.i(TAG) { "SSE stream cancelled" }
        }
    }

    private suspend fun executeApiAndParse(
        apiKey: String,
        request: ChatCompletionRequest,
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int
    ): Result<Recipe> {
        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            // Check for API-level errors in the response body
            if (response.error != null) {
                val errorMsg = response.error.message ?: "Unknown API error"
                val errorCode = response.error.code
                ScrapChefLogger.e(TAG) { "OpenRouter API error in response body: code=$errorCode message=$errorMsg" }
                return when (errorCode) {
                    429 -> Result.failure(RateLimitException("Rate limit exceeded. Please wait a moment and try again."))
                    401, 403 -> Result.failure(ApiKeyInvalidException("API key is invalid or unauthorized. Please check your OpenRouter API key."))
                    else -> Result.failure(AiGenerationException("AI service error: $errorMsg"))
                }
            }

            val content = response.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()

            if (content.isNullOrBlank()) {
                ScrapChefLogger.w(TAG) { "OpenRouter returned empty response body" }
                return Result.failure(AiGenerationException("AI returned an empty response. Please try again."))
            }

            // Clean markdown blocks ```json ... ``` if present
            val cleanedJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val recipe = recipeAdapter.fromJson(cleanedJson)
                ?: return Result.failure(AiGenerationException("Failed to parse AI response into recipe format."))

            // Minimal validation
            val validated = validateRecipe(recipe, userIngredients, selectedTools, maxTimeMinutes)
                ?: return Result.failure(AiGenerationException("AI response was missing essential fields (title or instructions)."))

            Result.success(validated)
        } catch (e: HttpException) {
            val code = e.code()
            ScrapChefLogger.e(TAG, e) { "OpenRouter HTTP error: $code" }
            when (code) {
                429 -> Result.failure(RateLimitException("Rate limit exceeded. Please wait a moment and try again."))
                402 -> Result.failure(RateLimitException("Insufficient credits. Please add credits to your OpenRouter account."))
                401, 403 -> Result.failure(ApiKeyInvalidException("API key is invalid or unauthorized. Please check your OpenRouter API key."))
                503 -> Result.failure(AiGenerationException("AI service is temporarily unavailable. Please try again."))
                else -> Result.failure(AiGenerationException("AI service error (HTTP $code). Please try again."))
            }
        } catch (e: SocketTimeoutException) {
            ScrapChefLogger.e(TAG, e) { "OpenRouter API timeout" }
            Result.failure(AiTimeoutException("Recipe generation timed out. Please check your internet connection and try again."))
        } catch (e: UnknownHostException) {
            ScrapChefLogger.e(TAG, e) { "Network unreachable" }
            Result.failure(NetworkException("Cannot reach the AI service. Please check your internet connection."))
        } catch (e: Exception) {
            ScrapChefLogger.e(TAG, e) { "OpenRouter API call failed: ${e.javaClass.simpleName}: ${e.message}" }
            Result.failure(AiGenerationException("Failed to generate recipe. Please try again."))
        }
    }

    /**
     * Minimal validation — only checks essential fields.
     */
    private fun validateRecipe(
        recipe: Recipe,
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int
    ): Recipe? {
        if (recipe.name.isBlank()) {
            ScrapChefLogger.w(TAG) { "Validation failed: recipe name is blank" }
            return null
        }

        if (recipe.effectiveInstructions.isEmpty()) {
            ScrapChefLogger.w(TAG) { "Validation failed: no instructions in recipe" }
            return null
        }

        return recipe.copy(
            yourIngredients = recipe.yourIngredients.ifEmpty { userIngredients },
            tools = recipe.tools.ifEmpty { selectedTools.map { it.id } }
        )
    }

    private fun classifyError(error: Throwable): Exception {
        return when (error) {
            is RateLimitException -> error
            is ApiKeyInvalidException -> error
            is AiGenerationException -> error
            is AiTimeoutException -> error
            is NetworkException -> error
            is SocketTimeoutException -> AiTimeoutException(
                "Recipe generation timed out. Please check your internet connection and try again."
            )
            is UnknownHostException -> NetworkException(
                "Cannot reach the AI service. Please check your internet connection."
            )
            else -> AiGenerationException(
                "Failed to generate recipe: ${error.message ?: "Unknown error"}. Please try again."
            )
        }
    }
}

// Typed exceptions for error classification in the ViewModel
class ApiKeyInvalidException(message: String) : Exception(message)
class AiTimeoutException(message: String) : Exception(message)
class AiGenerationException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
