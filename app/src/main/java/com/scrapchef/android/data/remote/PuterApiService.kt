package com.scrapchef.android.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ─── Puter.js AI Chat Driver Request/Response Models ─────────────

@JsonClass(generateAdapter = true)
data class PuterMessage(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class PuterDriverArgs(
    @Json(name = "messages") val messages: List<PuterMessage>,
    @Json(name = "model") val model: String? = null,
    @Json(name = "stream") val stream: Boolean = false,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class PuterDriverRequest(
    @Json(name = "interface") val iface: String = "puter-chat-completion",
    @Json(name = "driver") val driver: String = "ai-chat",
    @Json(name = "test_mode") val testMode: Boolean = false,
    @Json(name = "method") val method: String = "complete",
    @Json(name = "args") val args: PuterDriverArgs,
    @Json(name = "auth_token") val authToken: String? = null
)

@JsonClass(generateAdapter = true)
data class PuterMessageResponse(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class PuterResult(
    @Json(name = "message") val message: PuterMessageResponse? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class PuterError(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PuterDriverResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "result") val result: PuterResult? = null,
    @Json(name = "error") val error: PuterError? = null
)

// ─── Retrofit Service ────────────────────────────────────────────

interface PuterApiService {
    @POST("drivers/call")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "text/plain;actually=json",
        @Body request: PuterDriverRequest
    ): PuterDriverResponse
}

// ─── Retrofit Client ─────────────────────────────────────────────

object PuterRetrofitClient {
    private const val BASE_URL = "https://api.puter.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .apply {
            // Only log HTTP bodies in debug builds — redact auth tokens
            if (com.scrapchef.android.BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    /** Separate client for Puter streaming (NDJSON / SSE) — longer read timeout. */
    val streamClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var _service: PuterApiService? = null

    var service: PuterApiService
        get() = _service ?: synchronized(this) {
            _service ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(PuterApiService::class.java)
                .also { _service = it }
        }
        set(value) {
            _service = value
        }
}
