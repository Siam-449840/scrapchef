package com.scrapchef.android.data.remote

import com.scrapchef.android.BuildConfig
import com.scrapchef.android.data.model.Recipe
import com.scrapchef.android.data.model.Tool
import com.scrapchef.android.util.ScrapChefLogger
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PuterRepository(
    private val apiService: PuterApiService = PuterRetrofitClient.service,
    private val tokenProvider: (suspend () -> String?)? = null
) {
    private val moshi = Moshi.Builder().build()
    private val recipeAdapter = moshi.adapter(Recipe::class.java)

    companion object {
        private const val TAG = "ScrapChef"
        const val DEFAULT_PUTER_MODEL = "gpt-5-nano"
    }

    private suspend fun resolveAuthToken(): String {
        val userToken = tokenProvider?.invoke()?.trim()
        if (!userToken.isNullOrBlank()) {
            return userToken
        }
        // Fallback for local development/testing from BuildConfig
        return try {
            val buildToken = BuildConfig::class.java.getField("PUTER_AUTH_TOKEN").get(null) as? String ?: ""
            buildToken.trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun resolveModel(): String {
        return try {
            val buildModel = BuildConfig::class.java.getField("PUTER_MODEL").get(null) as? String ?: ""
            buildModel.ifBlank { DEFAULT_PUTER_MODEL }
        } catch (_: Exception) {
            DEFAULT_PUTER_MODEL
        }
    }

    suspend fun generateRecipe(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): Result<Recipe> = withContext(Dispatchers.IO) {
        val authToken = resolveAuthToken()
        if (authToken.isBlank() || authToken == "YOUR_PUTER_AUTH_TOKEN") {
            ScrapChefLogger.e(TAG) { "Puter auth token is not configured." }
            return@withContext Result.failure(
                PuterAuthException("Puter account not connected. Please configure your Puter authentication token.")
            )
        }

        val model = resolveModel()
        ScrapChefLogger.i(TAG) { "Using Puter model: $model" }

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

        val request = PuterDriverRequest(
            iface = "puter-chat-completion",
            driver = "ai-chat",
            testMode = false,
            method = "complete",
            authToken = authToken,
            args = PuterDriverArgs(
                model = model,
                messages = listOf(
                    PuterMessage(role = "system", content = systemPrompt),
                    PuterMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.7f,
                stream = false
            )
        )

        ScrapChefLogger.i(TAG) { "Sending Puter AI request | model=$model ingredients=$userIngredients tools=$toolNames maxTime=$timeConstraintStr" }
        val startTime = System.currentTimeMillis()

        // Attempt 1
        val result = executeApiAndParse(authToken, request, userIngredients, selectedTools, maxTimeMinutes)
        val latency = System.currentTimeMillis() - startTime

        if (result.isSuccess) {
            ScrapChefLogger.i(TAG) { "Puter AI success | latency=${latency}ms | recipe='${result.getOrNull()?.name}'" }
            return@withContext result
        }

        val firstError = result.exceptionOrNull()
            ?: return@withContext Result.failure(Exception("Puter AI attempt 1 failed with no exception"))
        ScrapChefLogger.w(TAG) { "Puter AI attempt 1 failed | latency=${latency}ms | error=${firstError.message}" }

        // Don't retry rate limit, quota, auth errors, or timeouts
        if (firstError is PuterRateLimitException || firstError is PuterQuotaException ||
            firstError is PuterAuthException || firstError is AiTimeoutException) {
            return@withContext Result.failure(firstError)
        }

        // Attempt 2 — retry with backoff and an emphatic reminder
        kotlinx.coroutines.delay(2000L)
        val retryRequest = request.copy(
            args = request.args.copy(
                messages = listOf(
                    PuterMessage(role = "system", content = systemPrompt + "\nIMPORTANT: Use STRICTLY ONLY user ingredients and allowed pantry staples. Return valid JSON only, no markdown."),
                    PuterMessage(role = "user", content = userPrompt)
                )
            )
        )

        ScrapChefLogger.i(TAG) { "Retrying Puter AI (attempt 2)" }
        val retryStart = System.currentTimeMillis()
        val retryResult = executeApiAndParse(authToken, retryRequest, userIngredients, selectedTools, maxTimeMinutes)
        val retryLatency = System.currentTimeMillis() - retryStart

        if (retryResult.isSuccess) {
            ScrapChefLogger.i(TAG) { "Puter AI retry success | latency=${retryLatency}ms | recipe='${retryResult.getOrNull()?.name}'" }
            return@withContext retryResult
        }

        val finalError = retryResult.exceptionOrNull()
        ScrapChefLogger.e(TAG, finalError) { "Puter AI both attempts failed | error=${finalError?.message}" }

        return@withContext Result.failure(classifyError(finalError ?: firstError))
    }

    /**
     * Streaming variant of generateRecipe using Puter's stream output.
     * Emits progressive recipe states as chunks arrive.
     */
    fun generateRecipeStream(
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int,
        dietaryPreferences: Set<String> = emptySet(),
        cuisineCountry: String = ""
    ): Flow<StreamingRecipeState> = callbackFlow {
        val authToken = resolveAuthToken()
        if (authToken.isBlank() || authToken == "YOUR_PUTER_AUTH_TOKEN") {
            trySend(StreamingRecipeState(error = "Puter account not connected. Please configure your Puter token."))
            close()
            return@callbackFlow
        }

        val model = resolveModel()
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

        val requestJson = JSONObject().apply {
            put("interface", "puter-chat-completion")
            put("driver", "ai-chat")
            put("test_mode", false)
            put("method", "complete")
            put("auth_token", authToken)
            put("args", JSONObject().apply {
                put("model", model)
                put("stream", true)
                put("temperature", 0.7)
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
            })
        }

        val httpRequest = Request.Builder()
            .url("https://api.puter.com/drivers/call")
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "text/plain;actually=json")
            .addHeader("Accept", "application/x-ndjson, text/event-stream, */*")
            .post(requestJson.toString().toRequestBody("text/plain;actually=json".toMediaType()))
            .build()

        val call = PuterRetrofitClient.streamClient.newCall(httpRequest)
        val accumulated = StringBuilder()
        var lastParseMs = 0L

        val thread = Thread {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = when (code) {
                        401 -> "Puter account unauthorized or token expired."
                        402 -> "Puter account monthly allowance exhausted."
                        429 -> "Rate limit exceeded. Please wait a moment and try again."
                        502, 503, 504 -> "Puter AI service is temporarily unavailable."
                        else -> "Puter stream failed with HTTP $code"
                    }
                    trySend(StreamingRecipeState(error = errorMsg))
                    close()
                    return@Thread
                }

                val body = response.body
                if (body == null) {
                    trySend(StreamingRecipeState(error = "Empty response from Puter AI."))
                    close()
                    return@Thread
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.isEmpty()) continue

                    // Support both NDJSON and SSE framing
                    val jsonStr = if (trimmed.startsWith("data:")) {
                        trimmed.removePrefix("data:").trim()
                    } else {
                        trimmed
                    }

                    if (jsonStr == "[DONE]") {
                        break
                    }

                    try {
                        val chunk = JSONObject(jsonStr)

                        // Check for Puter error chunks
                        val errorObj = chunk.optJSONObject("error")
                        if (errorObj != null) {
                            val msg = errorObj.optString("message", "Streaming error")
                            val code = errorObj.optString("code", "")
                            val userMsg = when {
                                code == "insufficient_funds" -> "Puter monthly allowance exhausted."
                                code == "too_many_requests" -> "Rate limit exceeded. Please wait a moment."
                                else -> "Generation failed: $msg"
                            }
                            trySend(StreamingRecipeState(error = userMsg, rawAccumulated = accumulated.toString()))
                            close()
                            return@Thread
                        }

                        // Extract text delta from Puter chunk formats
                        val delta = when {
                            chunk.has("text") -> chunk.optString("text", "")
                            chunk.has("delta") -> chunk.optString("delta", "")
                            chunk.has("choices") -> {
                                chunk.optJSONArray("choices")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("delta")
                                    ?.optString("content", "") ?: ""
                            }
                            else -> ""
                        }

                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            val now = System.currentTimeMillis()
                            if (now - lastParseMs >= 100) {
                                lastParseMs = now
                                val state = StreamingRecipeParser.parse(accumulated.toString())
                                trySend(state)
                            }
                        }
                    } catch (e: Exception) {
                        ScrapChefLogger.w(TAG) { "Puter stream chunk parse notice: ${e.message}" }
                    }
                }

                // Final emission
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
            } catch (t: Throwable) {
                if (!call.isCanceled()) {
                    ScrapChefLogger.e(TAG, t) { "Puter stream failure: ${t.message}" }
                    trySend(StreamingRecipeState(error = t.message ?: "Streaming error", rawAccumulated = accumulated.toString()))
                    close(t)
                }
            }
        }
        thread.start()

        awaitClose {
            call.cancel()
            ScrapChefLogger.i(TAG) { "Puter stream cancelled" }
        }
    }

    private suspend fun executeApiAndParse(
        authToken: String,
        request: PuterDriverRequest,
        userIngredients: List<String>,
        selectedTools: Set<Tool>,
        maxTimeMinutes: Int
    ): Result<Recipe> {
        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer $authToken",
                request = request
            )

            // Check for Puter driver-level errors
            if (!response.success || response.error != null) {
                val errorMsg = response.error?.message ?: "Unknown Puter error"
                val errorCode = response.error?.code ?: ""
                ScrapChefLogger.e(TAG) { "Puter error response: code=$errorCode message=$errorMsg" }
                return when {
                    errorCode == "insufficient_funds" -> Result.failure(
                        PuterQuotaException("Puter monthly allowance exhausted. Please top up your Puter account.")
                    )
                    errorCode == "token_missing" || errorCode == "token_auth_failed" -> Result.failure(
                        PuterAuthException("Puter authentication failed or token is invalid.")
                    )
                    errorCode == "too_many_requests" -> Result.failure(
                        PuterRateLimitException("Puter rate limit exceeded. Please wait a moment and try again.")
                    )
                    else -> Result.failure(AiGenerationException("Puter AI service error: $errorMsg"))
                }
            }

            val content = response.result?.message?.content?.trim()

            if (content.isNullOrBlank()) {
                ScrapChefLogger.w(TAG) { "Puter returned empty content body" }
                return Result.failure(AiGenerationException("AI returned an empty response. Please try again."))
            }

            // Clean markdown blocks ```json ... ``` if present
            val cleanedJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val recipe = recipeAdapter.fromJson(cleanedJson)
                ?: return Result.failure(AiGenerationException("Failed to parse AI response into recipe format."))

            val validated = validateRecipe(recipe, userIngredients, selectedTools, maxTimeMinutes)
                ?: return Result.failure(AiGenerationException("AI response was missing essential fields (title or instructions)."))

            Result.success(validated)
        } catch (e: HttpException) {
            val code = e.code()
            ScrapChefLogger.e(TAG, e) { "Puter HTTP error: $code" }
            when (code) {
                401 -> Result.failure(PuterAuthException("Puter account unauthorized or token expired."))
                402 -> Result.failure(PuterQuotaException("Puter monthly allowance exhausted. Please top up your Puter account."))
                429 -> Result.failure(PuterRateLimitException("Puter rate limit exceeded. Please wait a moment and try again."))
                502, 503, 504 -> Result.failure(AiGenerationException("Puter AI service is temporarily unavailable. Please try again."))
                else -> Result.failure(AiGenerationException("Puter AI service error (HTTP $code). Please try again."))
            }
        } catch (e: SocketTimeoutException) {
            ScrapChefLogger.e(TAG, e) { "Puter API timeout" }
            Result.failure(AiTimeoutException("Recipe generation timed out. Please check your internet connection and try again."))
        } catch (e: UnknownHostException) {
            ScrapChefLogger.e(TAG, e) { "Network unreachable" }
            Result.failure(NetworkException("Cannot reach Puter AI service. Please check your internet connection."))
        } catch (e: Exception) {
            ScrapChefLogger.e(TAG, e) { "Puter API call failed: ${e.javaClass.simpleName}: ${e.message}" }
            Result.failure(AiGenerationException("Failed to generate recipe. Please try again."))
        }
    }

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
            is PuterRateLimitException -> error
            is PuterQuotaException -> error
            is PuterAuthException -> error
            is AiGenerationException -> error
            is AiTimeoutException -> error
            is NetworkException -> error
            is SocketTimeoutException -> AiTimeoutException(
                "Recipe generation timed out. Please check your internet connection and try again."
            )
            is UnknownHostException -> NetworkException(
                "Cannot reach Puter AI service. Please check your internet connection."
            )
            else -> AiGenerationException(
                "Failed to generate recipe: ${error.message ?: "Unknown error"}. Please try again."
            )
        }
    }
}

// Typed exceptions for error classification
class PuterAuthException(message: String) : Exception(message)
class PuterQuotaException(message: String) : Exception(message)
class PuterRateLimitException(message: String) : Exception(message)
class AiTimeoutException(message: String) : Exception(message)
class AiGenerationException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)
