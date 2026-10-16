package com.scrapchef.android.ui.viewmodel

import com.scrapchef.android.data.remote.PuterApiService
import com.scrapchef.android.data.remote.PuterDriverRequest
import com.scrapchef.android.data.remote.PuterDriverResponse
import com.scrapchef.android.data.remote.PuterMessageResponse
import com.scrapchef.android.data.remote.PuterResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MockPuterApiService : PuterApiService {
    var shouldThrowTimeout = false
    var shouldThrowOffline = false
    var shouldThrowAuthError = false
    var shouldThrowQuotaError = false
    var shouldThrowRateLimitError = false
    var customResponse: PuterDriverResponse? = null

    override suspend fun chatCompletion(
        authorization: String,
        contentType: String,
        request: PuterDriverRequest
    ): PuterDriverResponse {
        if (shouldThrowTimeout) {
            throw SocketTimeoutException("Mock timeout")
        }
        if (shouldThrowOffline) {
            throw UnknownHostException("Mock offline")
        }
        if (shouldThrowAuthError) {
            val body = "{\"error\":\"Authentication failed\",\"code\":\"token_auth_failed\"}"
                .toResponseBody("application/json".toMediaType())
            throw HttpException(Response.error<Any>(401, body))
        }
        if (shouldThrowQuotaError) {
            val body = "{\"error\":\"Insufficient credits\",\"code\":\"credit_exhausted\"}"
                .toResponseBody("application/json".toMediaType())
            throw HttpException(Response.error<Any>(402, body))
        }
        if (shouldThrowRateLimitError) {
            val body = "{\"error\":\"Rate limit exceeded\",\"code\":\"rate_limit\"}"
                .toResponseBody("application/json".toMediaType())
            throw HttpException(Response.error<Any>(429, body))
        }

        return customResponse ?: PuterDriverResponse(
            success = true,
            result = PuterResult(
                message = PuterMessageResponse(
                    role = "assistant",
                    content = """
                        {
                            "name": "Skillet Tomato & Onion Sauté",
                            "description": "A quick and savory dish.",
                            "totalTimeMinutes": 15,
                            "tools": ["pan"],
                            "difficulty": "Easy",
                            "servings": 2,
                            "yourIngredients": ["Tomato", "Onion", "Garlic"],
                            "pantryAssumptions": ["oil", "salt"],
                            "instructions": [
                                "Dice the tomatoes and chop the onions.",
                                "Heat oil in a skillet over medium heat.",
                                "Sauté until fragrant and tender."
                            ],
                            "tips": ["Serve with bread."]
                        }
                    """.trimIndent()
                )
            ),
            error = null
        )
    }
}
