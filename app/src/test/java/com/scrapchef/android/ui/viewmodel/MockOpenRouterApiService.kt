package com.scrapchef.android.ui.viewmodel

import com.scrapchef.android.data.remote.ChatCompletionRequest
import com.scrapchef.android.data.remote.ChatCompletionResponse
import com.scrapchef.android.data.remote.OpenRouterApiService
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MockOpenRouterApiService : OpenRouterApiService {
    var shouldThrowTimeout = false
    var shouldThrowOffline = false

    override suspend fun chatCompletion(
        authorization: String,
        referer: String,
        title: String,
        request: ChatCompletionRequest
    ): ChatCompletionResponse {
        if (shouldThrowTimeout) {
            throw SocketTimeoutException("Mock timeout")
        }
        if (shouldThrowOffline) {
            throw UnknownHostException("Mock offline")
        }
        return ChatCompletionResponse(choices = emptyList(), error = null)
    }
}
